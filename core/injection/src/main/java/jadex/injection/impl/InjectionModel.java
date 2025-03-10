package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
	
	/** Code to run on component start. */
	protected Consumer<IComponent>	onstart;
	
	/** Code to run on component end. */
	protected Consumer<IComponent>	onend;
	
	/**
	 *  Create injection model for given pojo class.
	 */
	public InjectionModel(Class<?> clazz)
	{
		this.clazz	= clazz;
	}
	
	
	/**
	 * Get the code to run on component start.
	 */
	public Consumer<IComponent> getOnStart()
	{
		if(onstart==null)
		{
			List<Consumer<IComponent>>	handles	= getInjectionHandles(clazz);
			handles.addAll(getInvocationHandles(clazz, OnStart.class));
			onstart	= unifyHandles(handles);
		}
		
		return onstart;
	}

	/**
	 * Get the code to run on component end.
	 */
	public Consumer<IComponent> getOnEnd()
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
	protected static Map<Function<Class<?>, Boolean>, BiFunction<IComponent, Class<?>, Object>>	injections	= new LinkedHashMap<>();
	
	static
	{
		injections.put(clazz -> IComponent.class.equals(clazz), (self, clazz) -> self);
		
		injections.put(clazz -> SReflect.isSupertype(IComponentFeature.class, clazz), (self, clazz) ->
		{
			@SuppressWarnings("unchecked")
			Class<IComponentFeature>	feature	= (Class<IComponentFeature>)clazz;
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
	
	
	protected static List<Consumer<IComponent>>	getInjectionHandles(Class<?> clazz)
	{
		List<Consumer<IComponent>>	handles	= new ArrayList<Consumer<IComponent>>();
		List<Field> fields = findFields(clazz, Inject.class);
		
		for(Field field: fields)
		{
			try
			{
				field.setAccessible(true);
				MethodHandle	handle	= MethodHandles.lookup().unreflectSetter(field);
				
				BiFunction<IComponent, Class<?>, Object>	injection	= null;
				for(Function<Class<?>, Boolean> check: injections.keySet())
				{
					if(check.apply(field.getType()))
					{
						injection	= injections.get(check);
					}
				}
						
				if(injection!=null)
				{
					final BiFunction<IComponent, Class<?>, Object>	finjection	= injection;
					handles.add(self -> {
						try
						{
							Object[]	args	= new Object[]{self.getPojo(), finjection.apply(self, field.getType())};
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
	
	protected static List<Consumer<IComponent>>	getInvocationHandles(Class<?> clazz, Class<? extends Annotation> annotation)
	{
		List<Consumer<IComponent>>	handles	= new ArrayList<Consumer<IComponent>>();
		
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
					List<BiFunction<IComponent, Class<?>, Object>>	param_injections	= new ArrayList<>();
					Class<?>[]	ptypes	= method.getParameterTypes();
					for(Class<?> param: ptypes)
					{
						BiFunction<IComponent, Class<?>, Object>	injection	= null;
						for(Function<Class<?>, Boolean> check: injections.keySet())
						{
							if(check.apply(param))
							{
								injection	= injections.get(check);
							}
						}
						
						if(injection!=null)
						{
							param_injections.add(injection);
						}
						else
						{
							throw new UnsupportedOperationException("Cannot inject "+param.getSimpleName()+" in "+method);
						}

						handles.add(self -> {
							try
							{
								Object[]	args	= new Object[param_injections.size()+1];
								args[0]	= self.getPojo();
								for(int i=1; i<args.length; i++)
								{
									args[i]	= param_injections.get(i-1).apply(self, ptypes[i-1]);
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
					handles.add(self -> {
						try
						{
							handle.invoke(self.getPojo());
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


	protected static Consumer<IComponent> unifyHandles(List<Consumer<IComponent>> handles)
	{
		Consumer<IComponent> ret;
		
		// No handles
		if(handles.isEmpty())
		{
			ret	= self -> {};// nop
		}
		
		// Single handle
		else if(handles.size()==1)
		{
			ret	= handles.get(0);
		}
		
		// Multiple handles
		else
		{
			ret	= self ->
			{
				for(int i=0; i<handles.size(); i++)
					handles.get(i).accept(self);
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
}
