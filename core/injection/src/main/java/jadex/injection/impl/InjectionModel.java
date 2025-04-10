package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.execution.impl.ExecutionFeatureProvider;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;

/**
 *  The injection model caches all injection/invocation-related stuff.
 */
public class InjectionModel
{
	static IInjectionHandle	NULL	= (self, pojos, context) -> null;
	
	 /** The pojo classes as a hierarchy of component pojo plus subobjects, if any.
	  *  The model is for the last pojo in the list. */
	protected List<Class<?>>	classes;
	
	/** Field injections on component start. */
	protected volatile IInjectionHandle	fields;
	
	/** Code to run on component start. */
	protected volatile IInjectionHandle	onstart;
	
	/** Extra code to run on component start (before OnStart). */
	protected volatile IInjectionHandle	extra;
	
	/** Method injections after component start. */
	protected volatile IInjectionHandle	methods;
	
	/** Code to run on component end. */
	protected volatile IInjectionHandle	onend;
	
	/** Code to fetch component results. */
	protected volatile IInjectionHandle	results;
	
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
			synchronized(this)
			{
				if(fields==null)
				{
					fields	= unifyHandles(getFieldInjections(classes));					
				}
			}
		}
		
		return fields==NULL ? null : fields;
	}
	
	/**
	 * Get the code to run on component start.
	 */
	public IInjectionHandle getOnStart()
	{
		if(onstart==null)
		{
			synchronized(this)
			{
				if(onstart==null)
				{
					onstart	= unifyHandles(getMethodInvocations(classes, OnStart.class));
				}
			}
		}
		
		return onstart==NULL ? null : onstart;
	}

	/**
	 * Get external code to run on component start.
	 */
	public IInjectionHandle getExtraOnStart()
	{
		if(extra==null)
		{
			synchronized(this)
			{
				if(extra==null)
				{
					extra	= unifyHandles(getExtraOnstartHandles(classes.get(classes.size()-1)));
				}
			}
		}
		
		return extra==NULL ? null : extra;
	}

	/**
	 * Get method injections to run after component start.
	 */
	public IInjectionHandle getMethodInjections()
	{
		if(methods==null)
		{
			synchronized(this)
			{
				if(methods==null)
				{
					methods	= unifyHandles(getMethodInjections(classes));
				}
			}
		}
		
		return methods==NULL ? null : methods;
	}

	/**
	 * Get the code to run on component end.
	 */
	public IInjectionHandle getOnEnd()
	{
		if(onend==null)
		{
			synchronized(this)
			{
				if(onend==null)
				{
					onend	= unifyHandles(getMethodInvocations(classes, OnEnd.class));
				}
			}
		}
		
		return onend==NULL ? null : onend;
	}
	
	/**
	 *  Get the fetcher to retrieve current component results.
	 */
	public IInjectionHandle getResultsFetcher()
	{
		if(results==null)
		{
			synchronized(this)
			{
				if(results==null)
				{
					// TODO: also @Provide!???
					List<Getter>	fetchers = getGetters(classes, ProvideResult.class);
					
					if(fetchers==null)
					{
						results	= NULL;
					}
					else
					{
						// Find names for getters
						Map<String, Tuple2<IInjectionHandle, Annotation[]>>	ffetchers	= new LinkedHashMap<>();
						for(Getter fetcher: fetchers)
						{
							String name	= fetcher.annotation() instanceof ProvideResult && ! "".equals(((ProvideResult)fetcher.annotation()).value())
								? ((ProvideResult)fetcher.annotation()).value()
								: fetcher.member() instanceof Method && fetcher.member().getName().startsWith("get")
									? fetcher.member().getName().substring(3).toLowerCase()
									: fetcher.member().getName();
							Annotation[]	annos	= fetcher.member() instanceof Method
								? (Annotation[])SUtil.joinArrays(((Method)fetcher.member()).getAnnotations(),
									((Method)fetcher.member()).getAnnotatedReturnType().getAnnotations())
								: ((Field)fetcher.member()).getAnnotations();
							ffetchers.put(name, new Tuple2<>(fetcher.fetcher(), annos));
						}
						
						// New handle to apply all getters
						results	= (comp, pojos, context) ->
						{
							Map<String, Object>	ret	= new LinkedHashMap<>();
							for(String name: ffetchers.keySet())
							{
								ret.put(name, ExecutionFeatureProvider.copyVal(ffetchers.get(name).getFirstEntity().apply(comp, pojos, context), ffetchers.get(name).getSecondEntity()));
							}
							return ret;
						};
					}
				}
			}
		}
		return results==NULL ? null : results;
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
						IInjectionHandle	fetcher	= null;
						for(IValueFetcherCreator check: fetchers.get(anno))
						{
							IInjectionHandle	test	= check.getValueFetcher(classes, field.getGenericType(), field.getAnnotation(anno));
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
							MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(field);
							MethodHandle	setter	= MethodHandles.lookup().unreflectSetter(field);
							final IInjectionHandle	ffetcher	= fetcher;
							handles.add((self, pojos, context) -> {
								try
								{
									Object	oldval	= getter.invoke(pojos.get(pojos.size()-1));
									Object[]	args	= new Object[]{pojos.get(pojos.size()-1), ffetcher.apply(self, pojos, oldval)};
									return setter.invokeWithArguments(args);
								}
								catch(Throwable e)
								{
									// Rethrow user exception
									throw SUtil.throwUnchecked(e);
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
		synchronized(minjections)
		{
			for(Class<? extends Annotation> anno: minjections.keySet())
			{
				for(Method method: InjectionModel.findMethods(classes.get(classes.size()-1), anno))
				{
					IInjectionHandle injection	= null;
					for(IMethodInjectionCreator check: minjections.get(anno))
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
	 *  Only useful when the return value does not matter.
	 * 	@param handles A potentially empty list of handles or NULL.
	 */
	protected static IInjectionHandle	unifyHandles(List<IInjectionHandle> handles)
	{
		IInjectionHandle ret;
		
		// No handles
		if(handles.isEmpty())
		{
			ret	= NULL;
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
					handles.get(i).apply(self, pojos, context);
				return null;
			};
		}
		
		return ret;
	}
	
	/**
	 *  Generic object to get a value from a field or method.
	 */
	public record Getter(Member member, Annotation annotation, IInjectionHandle fetcher) {}
	
	/**
	 *  Get value fetchers, that fetch the value of an annotated field.
	 *  The fetchers provide the result as FieldValue record.
	 */
	public static List<Getter> getGetters(List<Class<?>> classes, Class<? extends Annotation> anno)
	{
		List<Getter>	fetchers	= null;
		for(Field field: findFields(classes.get(classes.size()-1), anno))
		{
			if(fetchers==null)
			{
				fetchers	= new ArrayList<>(4);
			}
			try
			{
				field.setAccessible(true);
				MethodHandle	get	= MethodHandles.lookup().unreflectGetter(field);
				fetchers.add(new Getter(field, field.getAnnotation(anno), (comp, pojos, context) ->
				{
					try
					{
						return get.invoke(pojos.get(pojos.size()-1));
					}
					catch(Throwable t)
					{
						throw SUtil.throwUnchecked(t);
					}
				}));
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}

		for(Method method: findMethods(classes.get(classes.size()-1), anno))
		{
			if(fetchers==null)
			{
				fetchers	= new ArrayList<>(4);
			}
			
			fetchers.add(new Getter(method, method.getAnnotation(anno),
				createMethodInvocation(method, classes, null)));
		}

		return fetchers;
	}
	
	/**
	 *  Helper method to find all direct and inherited fields with the given annotation.
	 *  Fields are returned in order: subclass first, then superclass.
	 */
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
	
	/**
	 *  Helper method to find all direct and inherited methods with the given annotation.
	 *  Methods are returned in reverse order: superclass first, then subclass.
	 */
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
	 *  Helper method to find all direct and inherited inner classes with the given annotation.
	 *  Inner classes are returned in order: subclass first, then superclass.
	 */
	public static List<Class<?>> findInnerClasses(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Class<?>>	allclasses	= new ArrayList<>();
		Class<?> myclazz	= clazz;
		while(myclazz!=null)
		{
			for(Class<?> innerclazz: myclazz.getDeclaredClasses())
			{
				if(innerclazz.isAnnotationPresent(annotation))
				{
					allclasses.add(innerclazz);
				}
			}
			myclazz	= myclazz.getSuperclass();
		}
		return allclasses;
	}
	
	/**
	 *  Check constructors of the given class and find one where all args can be injected.
	 *  @throw {@link UnsupportedOperationException} when no viable constructor is found or multiple constructors are viable.
	 */
	public static IInjectionHandle	findViableConstructor(Class<?> clazz, List<Class<?>> parentclasses)
	{
		Constructor<?>	found	= null;
		IInjectionHandle	ret	= null;
		for(Constructor<?> con: clazz.getDeclaredConstructors())
		{
			try
			{
				ret	= createMethodInvocation(con, parentclasses, null);	// TODO: preparams!?
				if(found!=null)
				{
					throw new UnsupportedOperationException("Multiple viable constructors: "+con+", "+found);
				}
				found	= con;
			}
			catch(Exception e)
			{
				// ignore constructor.
			}
		}
		
		if(ret==null)
		{
			throw new UnsupportedOperationException("No viable constructors: "+clazz);
		}
		
		return ret;
	}
	
	/**
	 *  Create a handle for a method or constructor invocation.
	 */
	public static IInjectionHandle	createMethodInvocation(Executable method, List<Class<?>> classes, List<IInjectionHandle> preparams)
	{
		try
		{
			IInjectionHandle	ret;
			
			method.setAccessible(true);
			boolean	isstatic	= method instanceof Constructor || (method.getModifiers()&Modifier.STATIC)!=0;
			MethodHandle	handle	= method instanceof Method ? MethodHandles.lookup().unreflect((Method)method) : MethodHandles.lookup().unreflectConstructor((Constructor<?>) method);
			
			// Find parameters
			Parameter[]	params	= method.getParameters();
			Class<?>[]	ptypes	= method.getParameterTypes();
			if(ptypes.length!=0)
			{
				List<IInjectionHandle>	param_injections	= new ArrayList<>();
				for(int i=0; i<ptypes.length; i++)
				{
					// Check, if fetcher is provided from outside
					IInjectionHandle	fetcher	= preparams!=null && i<preparams.size()
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
										continue;
									}
									tried.add(check);
									
									IInjectionHandle	test	= check.getValueFetcher(classes, ptypes[i], params[i].isAnnotationPresent(anno) ? params[i].getAnnotation(anno) : null);
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
				
				if(isstatic)
				{
					ret	= (self, pojos, context) ->
					{
						try
						{
							Object[]	args	= new Object[param_injections.size()];
							for(int j=0; j<args.length; j++)
							{
								args[j]	= param_injections.get(j).apply(self, pojos, context);
							}
							
							return handle.invokeWithArguments(args);
						}
						catch(Throwable e)
						{
							// Rethrow user exception
							throw SUtil.throwUnchecked(e);
						}
					};

				}
				else
				{
					ret	= (self, pojos, context) ->
					{
						try
						{
							Object[]	args	= new Object[param_injections.size()+1];
							args[0]	= pojos.get(pojos.size()-1);
							for(int j=1; j<args.length; j++)
							{
								args[j]	= param_injections.get(j-1).apply(self, pojos, context);
							}
							
							return handle.invokeWithArguments(args);
						}
						catch(Throwable e)
						{
							// Rethrow user exception
							throw SUtil.throwUnchecked(e);
						}
					};
				}
			}
			else
			{
				if(isstatic)
				{
					ret	= (self, pojos, context) ->
					{
						try
						{
							return handle.invoke();
						}
						catch(Throwable e)
						{
							// Rethrow user exception
							throw SUtil.throwUnchecked(e);
						}
					};
				}
				else
				{
					ret	= (self, pojos, context) ->
					{
						try
						{
							return handle.invoke(pojos.get(pojos.size()-1));
						}
						catch(Throwable e)
						{
							// Rethrow user exception
							throw SUtil.throwUnchecked(e);
						}
					};					
				}
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
	
	/**
	 *  Add a parameter/field injection (i.e field/parameter type -> value fetcher).
	 *  In case of field injection, the context is the old field value.
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
	protected static Map<Class<? extends Annotation>, List<IMethodInjectionCreator>>	minjections	= new LinkedHashMap<>();

	/**
	 *  Add a method injections (i.e annotation -> method -> injection handle).
	 */
	@SafeVarargs
	public static void	addMethodInjection(IMethodInjectionCreator minjection, Class<? extends Annotation>... annos)
	{
		if(annos.length==0)
		{
			// Catch common programming mistake.
			throw new IllegalArgumentException("Missing annotation type(s).");
		}
		
		synchronized(minjections)
		{
			for(Class<? extends Annotation> anno: annos)
			{
				List<IMethodInjectionCreator>	list	= minjections.get(anno);
				if(list==null)
				{
					list	= new ArrayList<>(1);
					minjections.put(anno, list);
				}
				list.add(minjection);
			}
		}
	}
}
