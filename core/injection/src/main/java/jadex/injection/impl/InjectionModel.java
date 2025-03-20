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
	/** The pojo class. */
	protected Class<?>	clazz;
	
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
	 *  Create injection model for given pojo class.
	 */
	public InjectionModel(Class<?> clazz)
	{
		this.clazz	= clazz;
	}
	
	//-------- handles for lifecycle phases --------
	
	/**
	 *  Get the field injection handles.
	 */
	public IInjectionHandle	getFieldInjections()
	{
		if(fields==null)
		{
			fields	= unifyHandles(getFieldInjections(clazz));
		}
		
		return fields;
	}
	
	/**
	 * Get the code to run on component start.
	 */
	public IInjectionHandle getOnStart()
	{
		if(onstart==null)
		{
			onstart	= unifyHandles(getMethodInvocations(clazz, OnStart.class));
		}
		
		return onstart;
	}

	/**
	 * Get external code to run on component start.
	 */
	public IInjectionHandle getExtraOnStart()
	{
		if(extra==null)
		{
			extra	= unifyHandles(getExtraOnstartHandles(clazz));
		}
		
		return extra;
	}

	/**
	 * Get method injections to run after component start.
	 */
	public IInjectionHandle getMethodInjections()
	{
		if(methods==null)
		{
			methods	= unifyHandles(getMethodInjections(clazz));
		}
		
		return methods;
	}

	/**
	 * Get the code to run on component end.
	 */
	public IInjectionHandle getOnEnd()
	{
		if(onend==null)
		{
			onend	= unifyHandles(getMethodInvocations(clazz, OnEnd.class));
		}
		
		return onend;
	}
	
	//-------- static part --------
	
	/** The model cache. */
	protected static Map<Class<?>, InjectionModel>	cache	= new LinkedHashMap<>();
	
	/**
	 *  Get the model for a pojo class.
	 */
	public static InjectionModel	get(Class<?> clazz)
	{
		synchronized(cache)
		{
			if(!cache.containsKey(clazz))
			{
				cache.put(clazz, new InjectionModel(clazz));
			}
			return cache.get(clazz);
		}
	}
	
	
	protected static List<IInjectionHandle>	getFieldInjections(Class<?> clazz)
	{
		List<IInjectionHandle>	handles	= new ArrayList<>();
		List<Field> fields = findFields(clazz, Inject.class);
		
		for(Field field: fields)
		{
			try
			{
				IValueFetcher	fetcher	= null;
				for(Function<Class<?>, IValueFetcher> check: fetchers)
				{
					IValueFetcher	test	= check.apply(field.getType());
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
					handles.add((self, pojo, context) -> {
						try
						{
							Object[]	args	= new Object[]{pojo, ffetcher.getValue(self, pojo, context)};
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
		
		return handles;
	}
	
	/**
	 *  Get method handles for methods annotated with Inject.
	 *  Used as extension point.
	 */
	protected static List<IInjectionHandle>	getMethodInjections(Class<?> clazz)
	{
		List<IInjectionHandle>	ret	= new ArrayList<>();
		for(Method method: InjectionModel.findMethods(clazz, Inject.class))
		{
			IInjectionHandle injection	= null;
			for(Function<Method, IInjectionHandle> check: minjections)
			{
				IInjectionHandle	test	= check.apply(method);
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
	protected static List<IInjectionHandle>	getMethodInvocations(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<IInjectionHandle>	handles	= new ArrayList<>();
		
		List<Method> methods = findMethods(clazz, annotation);
		for(Method method: methods)
		{
			handles.add(createMethodInvocation(method, null));
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
			ret	= (self, pojo, context) -> {};// nop
		}
		
		// Single handle
		else if(handles.size()==1)
		{
			ret	= handles.get(0);
		}
		
		// Multiple handles
		else
		{
			ret	= (self, pojo, context) ->
			{
				for(int i=0; i<handles.size(); i++)
					handles.get(i).handleInjection(self, pojo, context);
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
	public static IInjectionHandle	createMethodInvocation(Method method, List<IValueFetcher> preparams)
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
						for(Function<Class<?>, IValueFetcher> check: fetchers)
						{
							IValueFetcher	test	= check.apply(ptypes[i]);
							if(test!=null)
							{
								if(fetcher!=null)
								{
									throw new RuntimeException("Conflicting field injections: "+fetcher+", "+test);
								}
								fetcher	= test;
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

				ret	= (self, pojo, context) ->
				{
					try
					{
						Object[]	args	= new Object[param_injections.size()+1];
						args[0]	= pojo;
						for(int j=1; j<args.length; j++)
						{
							args[j]	= param_injections.get(j-1).getValue(self, pojo, context);
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
				ret	= (self, pojo, context) ->
				{
					try
					{
						handle.invoke(pojo);
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
	protected static List<Function<Class<?>, IValueFetcher>>	fetchers	= new ArrayList<>();
	
	static
	{
		addValueFetcher(clazz -> IComponent.class.equals(clazz) ? ((self, pojo, context) -> self) : null);
		
		addValueFetcher(clazz -> SReflect.isSupertype(IComponentFeature.class, clazz) ? ((self, pojo, context) ->
		{
			@SuppressWarnings("unchecked")
			Class<IComponentFeature>	feature	= (Class<IComponentFeature>)clazz;
			return self.getFeature((Class<IComponentFeature>)feature);
		}): null);
	}

	/**
	 *  Add a parameter/field injection (i.e field/parameter type -> value fetcher).
	 */
	public static void	addValueFetcher(Function<Class<?>,  IValueFetcher> fetcher)
	{
		fetchers.add(fetcher);
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
	protected static List<Function<Method, IInjectionHandle>>	minjections	= new ArrayList<>();

	/**
	 *  Add a method injections (i.e method -> injection handle).
	 */
	public static void	addMethodInjection(Function<Method, IInjectionHandle> minjection)
	{
		minjections.add(minjection);
	}

}
