package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.IComponentFeature;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;

/**
 *  The injection model caches all injection/invocation-related stuff.
 */
public class InjectionModel
{
	static IInjectionHandle	NOP	= (self, pojos, context) -> {};
	
	 /** The pojo classes as a hierachy of component pojo plus subobjects, if any.
	  *  The model is for the last pojo in the list. */
	protected List<Class<?>>	classes;
	
	/** Field injections on component start. */
	protected IInjectionHandle	fields;
	
	/** Code to run on component start. */
	protected IInjectionHandle	onstart;
	
	/** Extra code to run on component start. */
	protected IInjectionHandle	extra;
	
	/** Method injections after component start. */
	protected IInjectionHandle	methods;
	
	/** Code to run on component end. */
	protected IInjectionHandle	onend;
	
	/**
	 *  Create injection model for given stack of pojo classes.
	 */
	protected InjectionModel(List<Class<?>> classes)
	{
		this.classes	= classes;
	}
	
	//-------- handles for lifecycle phases --------
	
	/**
	 *  Get the field injection handles.
	 */
	public IInjectionHandle	getFieldInjections()
	{
		if(fields==null)
		{
			fields	= unifyHandles(getFieldInjections(classes));
		}
		
		return fields==NOP ? null : fields;
	}
	
	/**
	 * Get the code to run on component start.
	 */
	public IInjectionHandle getOnStart()
	{
		if(onstart==null)
		{
			onstart	= unifyHandles(getMethodInvocations(classes, OnStart.class));
		}
		
		return onstart==NOP ? null : onstart;
	}

	/**
	 * Get external code to run on component start.
	 */
	public IInjectionHandle getExtraOnStart()
	{
		if(extra==null)
		{
			extra	= unifyHandles(getExtraOnstartHandles(classes.get(classes.size()-1)));
		}
		
		return extra==NOP ? null : extra;
	}

	/**
	 * Get method injections to run after component start.
	 */
	public IInjectionHandle getMethodInjections()
	{
		if(methods==null)
		{
			methods	= unifyHandles(getMethodInjections(classes));
		}
		
		return methods==NOP ? null : methods;
	}

	/**
	 * Get the code to run on component end.
	 */
	public IInjectionHandle getOnEnd()
	{
		if(onend==null)
		{
			onend	= unifyHandles(getMethodInvocations(classes, OnEnd.class));
		}
		
		return onend==NOP ? null : onend;
	}
	
	//-------- static part --------
	
	/** The model cache. */
	protected static Map<List<Class<?>>, InjectionModel>	cache	= new LinkedHashMap<>();
	
	/**
	 *  Get the model for a stack of pojo objects.
	 */
	public static InjectionModel	get(List<Object> pojos)
	{
		List<Class<?>>	key	= new ArrayList<Class<?>>(pojos.size());
		for(Object pojo: pojos)
		{
			key.add(pojo!=null ? pojo.getClass() : Object.class);
		}
		synchronized(cache)
		{
			if(!cache.containsKey(key))
			{
				cache.put(key, new InjectionModel(key));
			}
			return cache.get(key);
		}
	}
	
	
	protected static List<IInjectionHandle>	getFieldInjections(List<Class<?>> classes)
	{
		List<IInjectionHandle>	handles	= new ArrayList<>();
		List<Field>	allfields	= new ArrayList<Field>();
		
		synchronized(fetchers)
		{
			for(Class<? extends Annotation> anno: fetchers.keySet())
			{
				List<Field> fields = findFields(classes.get(classes.size()-1), anno);
				
				for(Field field: fields)
				{
					if(allfields.contains(field))
					{
						throw new RuntimeException("Multiple Inject annotations on field: "+field);
					}
					allfields.add(field);
					
					try
					{
						IValueFetcher	fetcher	= null;
						for(IValueFetcherCreator check: fetchers.get(anno))
						{
							IValueFetcher	test	= check.getValueFetcher(classes, field.getGenericType());
							if(test!=null)
							{
								if(fetcher!=null)
								{
									throw new RuntimeException("Conflicting field injections: "+fetcher+", "+test);
								}
								fetcher	= test;
							}
						}
								
						if(fetcher!=null)
						{
							field.setAccessible(true);
							MethodHandle	handle	= MethodHandles.lookup().unreflectSetter(field);
							final IValueFetcher	ffetcher	= fetcher;
							handles.add((self, pojos, context) -> {
								try
								{
									Object[]	args	= new Object[]{pojos.get(pojos.size()-1), ffetcher.getValue(self, pojos, context)};
									handle.invokeWithArguments(args);
								}
								catch(Throwable e)
								{
									// Rethrow user exception
									SUtil.throwUnchecked(e);
								}
							});			
						}
						else
						{
							throw new UnsupportedOperationException("Cannot inject "+field);
						}
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
				}
			}
		}
		
		return handles;
	}
	
	/**
	 *  Get method handles for methods annotated with Inject.
	 *  Used as extension point.
	 */
	protected static List<IInjectionHandle>	getMethodInjections(List<Class<?>> classes)
	{
		List<IInjectionHandle>	ret	= new ArrayList<>();
		for(Method method: InjectionModel.findMethods(classes.get(classes.size()-1), Inject.class))
		{
			IInjectionHandle injection	= null;
			for(IMethodInjectionCreator check: minjections)
			{
				IInjectionHandle	test	= check.getInjectionHandle(classes, method);
				if(test!=null)
				{
					if(injection!=null)
					{
						throw new RuntimeException("Conflicting method injections: "+injection+", "+test);
					}
				}
				injection	= test;
			}
			
			if(injection!=null)
			{
				ret.add(injection);	
			}
			else
			{
				throw new UnsupportedOperationException("Cannot inject "+method);
			}

		}
		return ret;
	}
	
	/**
	 *  Get all invocation handles for methods with given annotation.
	 */
	protected static List<IInjectionHandle>	getMethodInvocations(List<Class<?>> classes, Class<? extends Annotation> annotation)
	{
		List<IInjectionHandle>	handles	= new ArrayList<>();
		
		List<Method> methods = findMethods(classes.get(classes.size()-1), annotation);
		for(Method method: methods)
		{
			handles.add(createMethodInvocation(method, classes, null));
		}
		
		return handles;
	}

	/**
	 *  Combine potentially multiple handles to one.
	 * 	@param handles A potentially empty list of handles or null.
	 */
	protected static IInjectionHandle	unifyHandles(List<IInjectionHandle> handles)
	{
		IInjectionHandle ret;
		
		// No handles
		if(handles.isEmpty())
		{
			ret	= NOP;
		}
		
		// Single handle
		else if(handles.size()==1)
		{
			ret	= handles.get(0);
		}
		
		// Multiple handles
		else
		{
			ret	= (self, pojos, context) ->
			{
				for(int i=0; i<handles.size(); i++)
					handles.get(i).handleInjection(self, pojos, context);
			};
		}
		
		return ret;
	}

	public static List<Field> findFields(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Field>	allfields	= new ArrayList<>();
		Class<?> myclazz	= clazz;
		while(myclazz!=null)
		{
			for(Field field: myclazz.getDeclaredFields())
			{
				if(field.isAnnotationPresent(annotation))
				{
					if((field.getModifiers() & Modifier.STATIC)!=0)
					{
						throw new RuntimeException("Fields with @"+annotation.getSimpleName()+" must not be static: "+field);
					}
					allfields.add(field);
				}
			}
			myclazz	= myclazz.getSuperclass();
		}
		return allfields;
	}
	
	public static List<Method> findMethods(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Method>	allmethods	= new ArrayList<>();
		Class<?> myclazz	= clazz;
		while(myclazz!=null)
		{
			for(Method method: myclazz.getDeclaredMethods())
			{
				if(method.isAnnotationPresent(annotation))
				{
					if((method.getModifiers() & Modifier.STATIC)!=0)
					{
						throw new RuntimeException("Methods with @"+annotation.getSimpleName()+" must not be static: "+method);
					}
					allmethods.add(method);
				}
			}
			myclazz	= myclazz.getSuperclass();
		}
		
		// Reverse list to execute superclass methods first
		Set<List<Object>>	method_ids	= new HashSet<>();
		List<Method>	methods	= new ArrayList<>();
		while(!allmethods.isEmpty())
		{
			try
			{
				Method	m	= allmethods.removeLast();

				// Skip if already added (i.e. annotation declared in superclass and subclass)
				List<Object>	method_id	= new ArrayList<>();
				method_id.add(m.getName());
				for(Class<?> param: m.getParameterTypes())
					method_id.add(param);
				if(!method_ids.contains(method_id))
				{
					methods.add(m);
					method_ids.add(method_id);
				}
			}
			catch(Exception e)
			{
				// Should not happen
				SUtil.throwUnchecked(e);
			}
		}
		return methods;
	}
	
	/**
	 *  Create a handle for a method invocation.
	 *  
	 */
	public static IInjectionHandle	createMethodInvocation(Method method, List<Class<?>> classes, List<IValueFetcher> preparams)
	{
		try
		{
			IInjectionHandle	ret;
			
			method.setAccessible(true);
			MethodHandle	handle	= MethodHandles.lookup().unreflect(method);
			
			// Find parameters
			Class<?>[]	ptypes	= method.getParameterTypes();
			if(ptypes.length!=0)
			{
				List<IValueFetcher>	param_injections	= new ArrayList<>();
				for(int i=0; i<ptypes.length; i++)
				{
					// Check, if fetcher is provided from outside
					IValueFetcher	fetcher	= preparams!=null && i<preparams.size()
						? preparams.get(i) : null;
					
					if(fetcher==null)
					{
						Set<IValueFetcherCreator>	tried	= new HashSet<IValueFetcherCreator>();
						synchronized(fetchers)
						{
							for(Class<? extends Annotation> anno: fetchers.keySet())
							{
								for(IValueFetcherCreator check: fetchers.get(anno))
								{
									if(tried.contains(check))
									{
										// Hack!!! Skip duplicate if same creator is added for different annotations.
										// TODO: check Inject annotations on parameters
										continue;
									}
									tried.add(check);
									
									IValueFetcher	test	= check.getValueFetcher(classes, ptypes[i]);
									if(test!=null)
									{
										if(fetcher!=null)
										{
											throw new RuntimeException("Conflicting parameter injections: "+fetcher+", "+test);
										}
										fetcher	= test;
									}
								}
							}
						}
					}
					
					if(fetcher!=null)
					{
						param_injections.add(fetcher);
					}
					else
					{
						throw new UnsupportedOperationException("Cannot inject "+ptypes[i].getSimpleName()+" in "+method);
					}
				}

				ret	= (self, pojos, context) ->
				{
					try
					{
						Object[]	args	= new Object[param_injections.size()+1];
						args[0]	= pojos.get(pojos.size()-1);
						for(int j=1; j<args.length; j++)
						{
							args[j]	= param_injections.get(j-1).getValue(self, pojos, context);
						}
						
						handle.invokeWithArguments(args);
					}
					catch(Throwable e)
					{
						// Rethrow user exception
						SUtil.throwUnchecked(e);
					}
				};			
			}
			else
			{
				ret	= (self, pojos, context) ->
				{
					try
					{
						handle.invoke(pojos.get(pojos.size()-1));
					}
					catch(Throwable e)
					{
						// Rethrow user exception
						SUtil.throwUnchecked(e);
					}
				};
			}
			
			return ret;
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	//-------- extension points --------
	
	/** The supported parameter/field injections (i.e class -> value). */
	protected static Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	fetchers	= new LinkedHashMap<>();
	
	static
	{
		// Inject IComponent
		addValueFetcher(
			(comptypes, valuetype) -> IComponent.class.equals(valuetype) ? ((self, pojo, context) -> self) : null,
			Inject.class);
		
		// Inject any pojo from hierarchy of subobjects.
		addValueFetcher((comptypes, valuetype) -> 
		{
			IValueFetcher	ret	= null;
			for(int i=0; i<comptypes.size(); i++)
			{
				if((valuetype instanceof Class) && SReflect.isSupertype((Class<?>)valuetype, comptypes.get(i)))
				{
					if(ret!=null)
					{
						throw new RuntimeException("Conflicting value injections: "+valuetype+", "+comptypes);
					}
					int	index	= i;
					ret	= (self, pojos, context) -> pojos.get(index);
				}
			}
			
			return ret;
		}, Inject.class);
		
		// Inject features
		addValueFetcher((comptypes, valuetype) ->
			(valuetype instanceof Class) && SReflect.isSupertype(IComponentFeature.class, (Class<?>)valuetype) ? ((self, pojo, context) ->
		{
			@SuppressWarnings("unchecked")
			Class<IComponentFeature>	feature	= (Class<IComponentFeature>)valuetype;
			return self.getFeature((Class<IComponentFeature>)feature);
		}): null, Inject.class);
	}

	/**
	 *  Add a parameter/field injection (i.e field/parameter type -> value fetcher).
	 */
	@SafeVarargs
	public static void	addValueFetcher(IValueFetcherCreator fetcher, Class<? extends Annotation>... annos)
	{
		if(annos.length==0)
		{
			// Catch common programming mistake.
			throw new IllegalArgumentException("Missing annotation type(s).");
		}
		
		synchronized(fetchers)
		{
			for(Class<? extends Annotation> anno: annos)
			{
				List<IValueFetcherCreator>	list	= fetchers.get(anno);
				if(list==null)
				{
					list	= new ArrayList<>(1);
					fetchers.put(anno, list);
				}
				list.add(fetcher);
			}
		}
	}
	
	
	/** Other features can add their handles that get executed after field injection and before @OnStart methods. */
	protected static List<Function<Class<?>, List<IInjectionHandle>>>	extra_onstart	= Collections.synchronizedList(new ArrayList<>());
	
	/**
	 * Other features can add their handles that get executed after field injection and before @OnStart methods.
	 */
	protected List<IInjectionHandle>	getExtraOnstartHandles(Class<?> pojoclazz)
	{
		List<IInjectionHandle>	ret	= new ArrayList<>();
		for(Function<Class<?>, List<IInjectionHandle>> extra: extra_onstart)
		{
			ret.addAll(extra.apply(pojoclazz));
		}
		return ret;
	}

	/**
	 * Other features can add their handles that get executed after field injection and before @OnStart methods.
	 */
	public static void	addExtraOnStart(Function<Class<?>, List<IInjectionHandle>> extra)
	{
		extra_onstart.add(extra);
	}
	
	
	/** The supported method injections (i.e method -> injection handle). */
	protected static List<IMethodInjectionCreator>	minjections	= new ArrayList<>();

	/**
	 *  Add a method injections (i.e method -> injection handle).
	 */
	public static void	addMethodInjection(IMethodInjectionCreator minjection)
	{
		minjections.add(minjection);
	}

}
