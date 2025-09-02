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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.core.IComponent;
import jadex.execution.impl.ExecutionFeatureProvider;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.injection.annotation.ProvideResult;

/**
 *  The injection model caches all injection/invocation-related stuff.
 */
public class InjectionModel
{
	static IInjectionHandle	NULL	= (self, pojos, context, oldval) -> null;
	
	/** The pojo classes as a hierarchy of component pojo plus subobjects, if any.
	 *  The model is for the last pojo in the list. */
	protected List<Class<?>>	classes;
	
	/** Optional path name(s) if this model is a named subobject (e.g. capability). */
	protected List<String>	path;
	
	/** The context specific value fetchers. */
	protected Map<Class<? extends Annotation>,List<IValueFetcherCreator>>	contextfetchers;
	
	/** Code to run on component start before field injections. */
	protected volatile IInjectionHandle	preinject;
	
	/** Field injections on component start. */
	protected volatile IInjectionHandle	fields;
	
	/** Code to run on component start after field injections. */
	protected volatile IInjectionHandle	postinject;
	
	/** User code to run on component start. */
	protected volatile IInjectionHandle	onstart;
	
	/** Method injections to be initialized after component start. */
	protected volatile IInjectionHandle	methods;
	
	/** User code to run on component end. */
	protected volatile IInjectionHandle	onend;
	
	/** Code to fetch component results. */
	protected volatile IInjectionHandle	results;
	
	/**
	 *  Create injection model for given stack of pojo classes.
	 */
	protected InjectionModel(List<Class<?>> classes, List<String> path, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		this.classes	= classes;
		this.path	= path;
		this.contextfetchers	= contextfetchers;
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
					Map<Field, IInjectionHandle>	allfields	= new LinkedHashMap<>();
					
					// Search in global fetchers.
					synchronized(fetchers)
					{
						addValueFetchers(classes, allfields, fetchers);
					}
					
					// Search in local fetchers.
					if(contextfetchers!=null)
					{
						addValueFetchers(classes, allfields, contextfetchers);
					}
					
					for(Field f: allfields.keySet())
					{
						if(allfields.get(f)==null)
						{
							throw new UnsupportedOperationException("Cannot inject "+f);
						}
					}
					
					fields	= unifyHandles(allfields.values());					
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
					onstart	= unifyHandles(getMethodInvocations(OnStart.class));
				}
			}
		}
		
		return onstart==NULL ? null : onstart;
	}

	/**
	 * Get external code to run before field injections.
	 */
	public IInjectionHandle getPreInject()
	{
		if(preinject==null)
		{
			synchronized(this)
			{
				if(preinject==null)
				{
					preinject	= unifyHandles(getPreInjectHandles(classes, path, contextfetchers));
				}
			}
		}
		
		return preinject==NULL ? null : preinject;
	}

	/**
	 * Get external code to run after field injections.
	 */
	public IInjectionHandle getPostInject()
	{
		if(postinject==null)
		{
			synchronized(this)
			{
				if(postinject==null)
				{
					postinject	= unifyHandles(getPostInjectHandles(classes, path, contextfetchers));
				}
			}
		}
		
		return postinject==NULL ? null : postinject;
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
									IInjectionHandle	test	= check.getInjectionHandle(classes, method, contextfetchers);
									if(test!=null)
									{
										if(injection!=null)
										{
											throw new RuntimeException("Conflicting method injections: "+injection+", "+test);
										}
										injection	= test;
									}
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
					methods	= unifyHandles(ret);
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
					onend	= unifyHandles(getMethodInvocations(OnEnd.class));
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
					List<Getter>	fetchers = getGetters(classes, ProvideResult.class, contextfetchers);
					
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
						results	= (comp, pojos, context, oldval) ->
						{
							Map<String, Object>	ret	= new LinkedHashMap<>();
							for(String name: ffetchers.keySet())
							{
								ret.put(name, ExecutionFeatureProvider.copyVal(ffetchers.get(name).getFirstEntity().apply(comp, pojos, context, null), ffetchers.get(name).getSecondEntity()));
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
	public static InjectionModel	get(List<Object> pojos, List<String> path, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		List<Class<?>>	key	= new ArrayList<Class<?>>(pojos.size());
		for(Object pojo: pojos)
		{
			key.add(pojo!=null ? pojo.getClass() : Object.class);
		}
		return getStatic(key, path, contextfetchers);
	}
	
	/**
	 *  Get the model for a stack of pojo classes.
	 */
	public static InjectionModel	getStatic(List<Class<?>> key, List<String> path, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{
		synchronized(cache)
		{
			InjectionModel model	= cache.get(key);
			if(model==null)
			{
				model	= new InjectionModel(key, path, contextfetchers);
				cache.put(key, model);
			}
			return model;
		}
	}
	
	/**
	 *  Add value fetchers for fields.
	 */
	protected static void addValueFetchers(List<Class<?>> classes, Map<Field, IInjectionHandle> allfields,
			Map<Class<? extends Annotation>, List<IValueFetcherCreator>> fetchers)
	{
		for(Class<? extends Annotation> anno: fetchers.keySet())
		{
			List<Field> fields = findFields(classes.get(classes.size()-1), anno);
			
			for(Field field: fields)
			{
				// TODO: do we want to check this?
//				if(allfields.containsKey(field))
//				{
//					throw new RuntimeException("Multiple Inject annotations on field: "+field);
//				}
				if(!allfields.containsKey(field))
				{
					allfields.put(field, null);
				}
				
				try
				{
					IInjectionHandle	fetcher	= null;
					for(IValueFetcherCreator check: fetchers.get(anno))
					{
						IInjectionHandle	test	= check.getValueFetcher(classes, field.getGenericType(), field.getAnnotation(anno));
						if(test!=null)
						{
							if(fetcher!=null && fetcher!=test)	// TODO: why duplicate of fetcher in bdi marsworld
							{
								throw new RuntimeException("Conflicting field injections: "+field+", "+fetcher.getClass()+", "+test.getClass());
							}
							fetcher	= test;
						}
					}
							
					if(fetcher!=null)
					{
						if(allfields.get(field)!=null)
						{
							throw new UnsupportedOperationException("Multiple value fetchers for field: "+field);
						}
						
						field.setAccessible(true);
						MethodHandle	getter	= MethodHandles.lookup().unreflectGetter(field);
						MethodHandle	setter	= MethodHandles.lookup().unreflectSetter(field);
						final IInjectionHandle	ffetcher	= fetcher;
						allfields.put(field, (self, pojos, context, oldval) -> {
							try
							{
								oldval	= getter.invoke(pojos.get(pojos.size()-1));
								Object[]	args	= new Object[]{pojos.get(pojos.size()-1), ffetcher.apply(self, pojos, context, oldval)};
								return setter.invokeWithArguments(args);
							}
							catch(Throwable e)
							{
								// Rethrow user exception
								throw SUtil.throwUnchecked(e);
							}
						});			
					}
				}
				catch(Exception e)
				{
					SUtil.throwUnchecked(e);
				}
			}
		}
	}
	
	/**
	 *  Get all invocation handles for methods with given annotation.
	 */
	protected List<IInjectionHandle>	getMethodInvocations(Class<? extends Annotation> annotation)
	{
		List<IInjectionHandle>	handles	= new ArrayList<>();
		
		List<Method> methods = findMethods(classes.get(classes.size()-1), annotation);
		for(Method method: methods)
		{
			handles.add(createMethodInvocation(method, classes, contextfetchers, null));
		}
		
		return handles;
	}

	/**
	 *  Combine potentially multiple handles to one.
	 *  Only useful when the return value does not matter.
	 * 	@param handles A potentially empty list of handles or NULL.
	 */
	protected static IInjectionHandle	unifyHandles(Collection<IInjectionHandle> handles)
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
			ret	= handles.iterator().next();
		}
		
		// Multiple handles
		else
		{
			ret	= (self, pojos, context, oldval) ->
			{
				for(IInjectionHandle handle: handles)
					handle.apply(self, pojos, context, null);
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
	public static List<Getter> getGetters(List<Class<?>> classes, Class<? extends Annotation> anno,
		Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
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
				fetchers.add(new Getter(field, field.getAnnotation(anno), (comp, pojos, context, oldval) ->
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
				createMethodInvocation(method, classes, contextfetchers, null)));
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
	 *  Helper method to find all constructors with the given annotation.
	 */
	public static List<Constructor<?>> findConstructors(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Constructor<?>>	allcons	= new ArrayList<>();
		for(Constructor<?> con: clazz.getDeclaredConstructors())
		{
			if(con.isAnnotationPresent(annotation))
			{
				allcons.add(con);
			}
		}
		return allcons;
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
			// Sort by name as java does not guarantee any order (reversed because it is later re-reversed)
			List<Method>	methods	= Arrays.asList(myclazz.getDeclaredMethods());
			methods.sort((a,b) -> a.getName().compareTo(b.getName()));
			for(Method method: methods.reversed())
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
	public static IInjectionHandle	findViableConstructor(Class<?> clazz, List<Class<?>> parentclasses,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers)
	{
		Constructor<?>	found	= null;
		IInjectionHandle	ret	= null;
		for(Constructor<?> con: clazz.getDeclaredConstructors())
		{
			try
			{
				ret	= createMethodInvocation(con, parentclasses, contextfetchers, null);
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
	public static IInjectionHandle	createMethodInvocation(Executable method, List<Class<?>> classes,
		Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers, List<IInjectionHandle> preparams)
	{
		try
		{
			IInjectionHandle	ret;
			
			method.setAccessible(true);
			boolean	isstatic	= method instanceof Constructor || (method.getModifiers()&Modifier.STATIC)!=0;
			MethodHandle	handle	= method instanceof Method ? MethodHandles.lookup().unreflect((Method)method) : MethodHandles.lookup().unreflectConstructor((Constructor<?>) method);
			
			// Find parameters
			Parameter[]	params	= method.getParameters();
			Type[]	ptypes	= method.getGenericParameterTypes();
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
											throw new RuntimeException("Conflicting parameter injections: "+method+", "+fetcher+", "+test);
										}
										fetcher	= test;
									}
								}
							}
						}
						if(contextfetchers!=null)
						{
							for(Class<? extends Annotation> anno: contextfetchers.keySet())
							{
								for(IValueFetcherCreator check: contextfetchers.get(anno))
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
						throw new UnsupportedOperationException("Cannot inject "+ptypes[i].getTypeName()+" in "+method);
					}
				}
				
				if(isstatic)
				{
					ret	= new IInjectionHandle()
					{
						@Override
						public Object apply(IComponent self, List<Object> pojos, Object context, Object oldval)
						{
							try
							{
								Object[]	args	= new Object[param_injections.size()];
								for(int j=0; j<args.length; j++)
								{
									args[j]	= param_injections.get(j).apply(self, pojos, context, null);
								}
								
								return handle.invokeWithArguments(args);
							}
							catch(Throwable e)
							{
								// Rethrow user exception
								throw SUtil.throwUnchecked(e);
							}
						}
						
						@Override
						public boolean isStatic()
						{
							return true;
						}
					};
				}
				else
				{
					ret	= (self, pojos, context, oldval) ->
					{
						try
						{
							Object[]	args	= new Object[param_injections.size()+1];
							args[0]	= pojos.get(pojos.size()-1);
							for(int j=1; j<args.length; j++)
							{
								args[j]	= param_injections.get(j-1).apply(self, pojos, context, null);
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
					ret	= new IInjectionHandle()
					{
						@Override
						public Object apply(IComponent self, List<Object> pojos, Object context, Object oldval)
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
						}
						
						@Override
						public boolean isStatic()
						{
							return true;
						}
					};
				}
				else
				{
					ret	= (self, pojos, context, oldval) ->
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
	
	
	/** Other features can add their handles that get executed before field injections. */
	protected static List<IExtraCodeCreator>	pre_inject	= Collections.synchronizedList(new ArrayList<>());
	
	/**
	 * Other features can add their handles that get executed before field injections.
	 */
	protected static List<IInjectionHandle>	getPreInjectHandles(List<Class<?>> pojoclazzes, List<String> path, Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers)
	{
		List<IInjectionHandle>	ret	= new ArrayList<>();
		for(IExtraCodeCreator extra: pre_inject)
		{
			ret.addAll(extra.getExtraCode(pojoclazzes, path, contextfetchers));
		}
		return ret;
	}

	/**
	 * Other features can add their handles that get executed before field injections.
	 */
	public static void	addPreInject(IExtraCodeCreator extra)
	{
		pre_inject.add(extra);
	}
	
	
	/** Other features can add their handles that get executed after field injection and before @OnStart methods. */
	protected static List<IExtraCodeCreator>	post_inject	= Collections.synchronizedList(new ArrayList<>());
	
	/**
	 * Other features can add their handles that get executed after field injection and before @OnStart methods.
	 */
	protected static List<IInjectionHandle>	getPostInjectHandles(List<Class<?>> pojoclazzes, List<String> path, Map<Class<? extends Annotation>, List<IValueFetcherCreator>> contextfetchers)
	{
		List<IInjectionHandle>	ret	= new ArrayList<>();
		for(IExtraCodeCreator extra: post_inject)
		{
			ret.addAll(extra.getExtraCode(pojoclazzes, path, contextfetchers));
		}
		return ret;
	}

	/**
	 * Other features can add their handles that get executed after field injection and before @OnStart methods.
	 */
	public static void	addPostInject(IExtraCodeCreator extra)
	{
		post_inject.add(extra);
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
