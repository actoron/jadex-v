package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jadex.common.SUtil;
import jadex.core.IComponent;
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
			onstart	= unifyHandles(getInvocationHandles(clazz, OnStart.class));
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
	
	
	protected static List<Consumer<IComponent>>	getInvocationHandles(Class<?> clazz, Class<? extends Annotation> annotation) throws Error
	{
		List<Consumer<IComponent>>	handles	= new ArrayList<Consumer<IComponent>>();
		
		List<Method> methods = findMethods(clazz, annotation);			
		for(Method method: methods)
		{
			try
			{
				method.setAccessible(true);
				MethodHandle	handle	= MethodHandles.lookup().unreflect(method);
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
			catch(Exception e)
			{
				// Should not happen?
				SUtil.throwUnchecked(e);
			}
		}
		
		return handles;
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
				// Go backwards through list to execute superclass methods first
				for(int i=handles.size()-1; i>=0; i--)
					handles.get(i).accept(self);
			};
		}
		
		return ret;
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
