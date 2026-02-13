package jadex.execution.impl;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.ComponentIdentifier;
import jadex.core.ICallable;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingFunction;
import jadex.core.InvalidComponentAccessException;
import jadex.core.annotation.NoCopy;
import jadex.core.impl.Component;
import jadex.execution.Call;
import jadex.execution.ComponentMethod;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

@NoCopy
public class ExecutableComponentHandle implements IComponentHandle
{
	private final Component comp;
	public static Set<String> ALLOWED_METHODS = new HashSet<>();
	static
	{
		ALLOWED_METHODS.add("toString");
		ALLOWED_METHODS.add("hashCode");
		ALLOWED_METHODS.add("equals");
	}
	protected Object pojo;

	public ExecutableComponentHandle(Component comp)
	{
		this.comp = comp;
	}

	@Override
	public ComponentIdentifier getId()
	{
		return comp.getId();
	}

	@Override
	public String getAppId()
	{
		return comp.getAppId();
	}
	
	@Override
	public IFuture<Map<String, Object>> getResults()
	{
		return Component.getResults(comp);
	}

	@Override
	public ISubscriptionIntermediateFuture<ChangeEvent> subscribeToResults()
	{
		return Component.subscribeToResults(comp);
	}
	
	@Override
	public <T> T getPojoHandle(Class<T> type)
	{
		if(pojo==null)
		{
			synchronized(this)
			{
				if(pojo==null)
				{
					pojo = createPojo();
				}
			}
		}
		return (T)pojo;
	}

	protected Object createPojo()
	{
		try
		{
			Object pojo = comp.getPojo();
			
			Object proxy = new ByteBuddy()
				.subclass(pojo.getClass())
				.method(ElementMatchers.not(ElementMatchers.isAnnotatedWith(ComponentMethod.class)))
	            .intercept(InvocationHandlerAdapter.of(new InvocationHandler() 
	            {
	            	@Override
	            	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
	                {
	            		if(IComponentManager.get().getCurrentComponent()!=null && IComponentManager.get().getCurrentComponent().getId().equals(getId()))
	            		{   
	            			return invokeMethod(comp, pojo, SUtil.arrayToList(args), method, null);
	            		}
	            		else if(ALLOWED_METHODS.contains(method.getName()))
	            		{
	            			return invokeMethod(comp, pojo, SUtil.arrayToList(args), method, null);
	            		}
	            		else
	            		{
	            			throw new InvalidComponentAccessException(comp.getId());
	            		}
	                }
	            }))
				.method(ElementMatchers.isAnnotatedWith(ComponentMethod.class)) 
				.intercept(InvocationHandlerAdapter.of((Object target, Method method, Object[] args)->
				{
					//Call next = Call.createCall(caller.getId(), null);
					Call next = Call.getOrCreateNextInvocation();
					
					List<Object> myargs = new ArrayList<Object>(); 
					Class<?>[] ptypes = method.getParameterTypes();
					java.lang.annotation.Annotation[][] pannos = method.getParameterAnnotations();

					for(int p = 0; p < ptypes.length; p++)
					{
						myargs.add(Component.copyVal(args[p], pannos[p]));
					}
					
					Future<Object> ret = null;
//					if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
//					{
//						FutureFunctionality func = new ComponentFutureFunctionality(comp.getFeature(IExecutionFeature.class))
//						{							
//							public Object handleResult(Object val) throws Exception
//							{
//								return Component.copyVal(val, method.getAnnotatedReturnType().getAnnotations());
//							}
//							
//							public Object handleIntermediateResult(Object val) throws Exception
//							{
//								return Component.copyVal(val, method.getAnnotatedReturnType().getAnnotations());
//							}
//						};
//						
//						ret = FutureFunctionality.createReturnFuture(method, func);
//					}
//					//final Object[] myargs = copyargs.toArray();
					
			        if(IComponentManager.get().getCurrentComponent()!=null && IComponentManager.get().getCurrentComponent().getId().equals(getId()))
			        {
//			        	System.out.println("already on agent: "+getId());
			        	if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
			        	{
			        		ret	= (Future<Object>) invokeMethod(comp, pojo, myargs, method, next);
			        	}
			        	else if(method.getReturnType().equals(void.class))
			        	{
			        		invokeMethod(comp, pojo, myargs, method, next);
			        	}
			        	else
			        	{
			        		//System.out.println("Agent methods must be async: "+method.getName()+" "+pojo);
			        		throw new InvalidComponentAccessException(comp.getId(), "Component methods must be async: "+method.getName());
			        	}
			        }
			        else
			        {
			        	if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
			        	{
							ret = scheduleAsyncStep(new ICallable<IFuture<Object>>() 
			        		{
								@Override
							    public AnnotatedType	getReturnType()
							    {
									return method.getAnnotatedReturnType();
							    }
			        			
			        			@Override
			        			public IFuture<Object> call() throws Exception
			        			{
			        				return (IFuture<Object>)invokeMethod(comp, pojo, myargs, method, next);
			        			}
							});
			        	}
			        	//System.out.println("scheduled on agent: "+getId());
			        	else if(method.getReturnType().equals(void.class))
			        	{
			        		scheduleStep(() -> invokeMethod(comp, pojo, myargs, method, next))
			        			.catchEx(ex -> comp.handleException(ex));
			        	}
			        	else 
			        	{
			        		//System.out.println("Component methods must be async: "+method.getName()+" "+pojo);
			          		throw new InvalidComponentAccessException(comp.getId(), "Agent methods must be async: "+method.getName());
			        	}
			        }
			        
			        return ret;
				}))
				.make()
				.load(pojo.getClass().getClassLoader())
	            .getLoaded()
	            .getConstructor()
	            .newInstance();
			
			//System.out.println("proxy hashCode: "+proxy.hashCode());
			
			return proxy;
		}
		catch(Exception e)
		{
			SUtil.rethrowAsUnchecked(e);
		}
		return null;
	}

	protected Object invokeMethod(Component component, Object pojo, List<Object> args, Method method, Call next)
	{
		//Call.roll();
		//Call next = Call.getNextInvocation();
		Call.setCurrentInvocation(next); // next becomes current
		Call.resetNextInvocation(); // next is null
		
//		// Try to guess parameters from given args or component internals.
//		IParameterGuesser guesser = new SimpleParameterGuesser(component.getValueProvider().getParameterGuesser(), args);
//		Object[] iargs = new Object[method.getParameterTypes().length];
//		for(int i=0; i<method.getParameterTypes().length; i++)
//		{
//			iargs[i] = guesser.guessParameter(method.getParameterTypes()[i], false);
//		}
		
		try
		{
			// It is now allowed to use protected/private component methods
			SAccess.setAccessible(method, true);
			return method.invoke(pojo, args.toArray(new Object[args.size()]));
		}
		catch(Exception e)
		{
			SUtil.rethrowAsUnchecked(e);
		}
		return null;
	}

	@Override
	public <T> IFuture<T> scheduleStep(Callable<T> step) 
	{
		return comp.getFeature(IExecutionFeature.class).scheduleStep(step);
	}

	@Override
	public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
	{
		return comp.getFeature(IExecutionFeature.class).scheduleStep(step);
	}

	@Override
	// Hack!!! separate arg and return future types as type can't be fetched from lambda leading to class cast exception
	public <E, T1 extends IFuture<E>, T2 extends IFuture<E>> T1 scheduleAsyncStep(Callable<T2> step)
	{
		return comp.getFeature(IExecutionFeature.class).scheduleAsyncStep(step);
	}

	@Override
	// Hack!!! separate arg and return future types as type can't be fetched from lambda leading to class cast exception
	public <E, T1 extends IFuture<E>, T2 extends IFuture<E>> T1 scheduleAsyncStep(IThrowingFunction<IComponent, T2> step)
	{
		return comp.getFeature(IExecutionFeature.class).scheduleAsyncStep(step);
	}
}