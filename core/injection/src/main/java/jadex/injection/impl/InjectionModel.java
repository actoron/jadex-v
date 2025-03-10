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
			onstart	= unifyHandles(getInvocationHandles(clazz, OnStart.class));
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
	 * Get the code to run on component end.
	 */
	public IInjectionHandle getOnEnd()
	{
		if(onend==null)
		{
			onend	= unifyHandles(getInvocationHandles(clazz, OnEnd.class));
		}
		
		return onend;
	}
	
	//-------- static part --------
	
	/** The model cache. */
	protected static Map<Class<?>, InjectionModel>	cache	= new LinkedHashMap<>();
	
	/** The supported parameter/field injections (i.e class -> value). */
	protected static Map<Function<Class<?>, Boolean>, IValueFetcher>	fetchers	= new LinkedHashMap<>();
	
	static
	{
		fetchers.put(clazz -> IComponent.class.equals(clazz), (self, pojo, type, context) -> self);
		
		fetchers.put(clazz -> SReflect.isSupertype(IComponentFeature.class, clazz), (self, pojo, type, context) ->
		{
			@SuppressWarnings("unchecked")
			Class<IComponentFeature>	feature	= (Class<IComponentFeature>)type;
			return self.getFeature((Class<IComponentFeature>)feature);
		});
	}
	
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
				for(Function<Class<?>, Boolean> check: fetchers.keySet())
				{
					if(check.apply(field.getType()))
					{
						if(fetcher!=null)
						{
							throw new RuntimeException("Conflicting field injections: "+fetcher+", "+fetchers.get(check));
						}
						fetcher	= fetchers.get(check);
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
							Object[]	args	= new Object[]{pojo, ffetcher.getValue(self, pojo, field.getType(), context)};
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
	
	protected static List<IInjectionHandle>	getInvocationHandles(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<IInjectionHandle>	handles	= new ArrayList<>();
		
		List<Method> methods = findMethods(clazz, annotation);
		for(Method method: methods)
		{
			try
			{
				method.setAccessible(true);
				MethodHandle	handle	= MethodHandles.lookup().unreflect(method);
				
				// Find parameters
				if(method.getParameters().length!=0)
				{
					List<IValueFetcher>	param_injections	= new ArrayList<>();
					Class<?>[]	ptypes	= method.getParameterTypes();
					for(Class<?> param: ptypes)
					{
						IValueFetcher	fetcher	= null;
						for(Function<Class<?>, Boolean> check: fetchers.keySet())
						{
							if(check.apply(param))
							{
								if(fetcher!=null)
								{
									throw new RuntimeException("Conflicting field injections: "+fetcher+", "+fetchers.get(check));
								}
								fetcher	= fetchers.get(check);
							}
						}
						
						if(fetcher!=null)
						{
							param_injections.add(fetcher);
						}
						else
						{
							throw new UnsupportedOperationException("Cannot inject "+param.getSimpleName()+" in "+method);
						}

						handles.add((self, pojo, context) ->
						{
							try
							{
								Object[]	args	= new Object[param_injections.size()+1];
								args[0]	= pojo;
								for(int i=1; i<args.length; i++)
								{
									args[i]	= param_injections.get(i-1).getValue(self, pojo, ptypes[i-1], context);
								}
								
								handle.invokeWithArguments(args);
							}
							catch(Throwable e)
							{
								// Rethrow user exception
								SUtil.throwUnchecked(e);
							}
						});					
					}					
				}
				else
				{
					handles.add((self, pojo, context) ->
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
					});					
				}				
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}
		
		// Go backwards through list to execute superclass methods first
		return handles.reversed();
	}


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

	protected static List<Field> findFields(Class<?> clazz, Class<? extends Annotation> annotation)
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
	
	protected static List<Method> findMethods(Class<?> clazz, Class<? extends Annotation> annotation)
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
	
	//-------- extension points --------
	
	/** Other features can add their handles that get executed after field injection and before @OnStart methods. */
	public static List<Function<Class<?>, List<IInjectionHandle>>>	extra_onstart	= Collections.synchronizedList(new ArrayList<>());
	
	protected List<IInjectionHandle>	getExtraOnstartHandles(Class<?> pojoclazz)
	{
		List<IInjectionHandle>	ret	= new ArrayList<>();
		for(Function<Class<?>, List<IInjectionHandle>> extra: extra_onstart)
		{
			ret.addAll(extra.apply(pojoclazz));
		}
		return ret;
	}
}
