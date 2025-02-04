package jadex.execution.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.common.IParameterGuesser;
import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.SimpleParameterGuesser;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.SCloner;
import jadex.common.transformation.traverser.Traverser;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ICallable;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.InvalidComponentAccessException;
import jadex.core.LambdaPojo;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SComponentFeatureProvider;
import jadex.execution.AgentMethod;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.execution.NoCopy;
import jadex.execution.future.FutureFunctionality;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.IntermediateFuture;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

public class ExecutionFeatureProvider extends ComponentFeatureProvider<IExecutionFeature>	implements IBootstrapping, IComponentLifecycleManager
{
	static
	{
		/*MjComponent.addComponentCreator(new IComponentLifecycleManager() 
		{
			@Override
			public boolean isCreator(Object obj) 
			{
				return Runnable.class.isAssignableFrom(obj.getClass())
					|| Supplier.class.isAssignableFrom(obj.getClass())
					|| IThrowingFunction.class.isAssignableFrom(obj.getClass());
			}
			
			@Override
			public void create(Object pojo, ComponentIdentifier cid)
			{
				if(pojo instanceof Runnable)
					LambdaAgent.create((Runnable)pojo, cid);
				else if(pojo instanceof Callable)
					LambdaAgent.create((Callable<?>)pojo, cid);
				else if(pojo instanceof IThrowingFunction)
					LambdaAgent.create((IThrowingFunction<IComponent, ?>)pojo, cid);
			}
			
			@Override
			public boolean isTerminator(IComponent component) 
			{
				return component.getClass().equals(MjComponent.class);
			}
				
			@Override
			public void terminate(IComponent component) 
			{
				((IMjInternalExecutionFeature)component.getFeature(IMjExecutionFeature.class)).terminate();
			}
		});*/
		
		// Init the component with schedule step functionality (hack?!)
		Component.setExternalAccessFactory(comp ->
		{
			return new IExternalAccess() 
			{
				protected Object pojo;
				
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
				public <T> T getPojo(Class<T> type)
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
							.method(ElementMatchers.not(ElementMatchers.isAnnotatedWith(AgentMethod.class)))
				            .intercept(InvocationHandlerAdapter.of(new InvocationHandler() 
				            {
				            	@Override
				            	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable 
				                {
				            		throw new InvalidComponentAccessException(comp.getId());
				                }
				            }))
							.method(ElementMatchers.isAnnotatedWith(AgentMethod.class)) 
							.intercept(InvocationHandlerAdapter.of((Object target, Method method, Object[] args)->
							{
								Future<Object> ret = FutureFunctionality.createReturnFuture(method, args, target.getClass().getClassLoader());
							    
								List<Object> myargs = new ArrayList<Object>(); 
								Class<?>[] ptypes = method.getParameterTypes();
								java.lang.annotation.Annotation[][] pannos = method.getParameterAnnotations();
								List<ITraverseProcessor> procs = Traverser.getDefaultProcessors();
								//;new ArrayList<>(ISerializationServices.get().getCloneProcessors());
								
								for(int p = 0; p < ptypes.length; p++)
								{
									for(int a = 0; a < pannos[p].length; a++)
									{
										if(hasAnnotation(pannos[p], NoCopy.class))
											myargs.add(args[p]);
										else
											myargs.add(SCloner.clone(args[p], procs));
									}
								}
								
								//final Object[] myargs = copyargs.toArray();
								
						        if(IComponentManager.get().getCurrentComponent()!=null && IComponentManager.get().getCurrentComponent().getId().equals(getId()))
						        {
						        	//System.out.println("already on agent: "+getId());
						        	if(ret instanceof IIntermediateFuture)
						        	{
						        		((IIntermediateFuture)invokeMethod(comp, pojo, myargs, method)).next(val ->
							            {
							            	if(!hasAnnotation(method.getAnnotatedReturnType().getAnnotations(), NoCopy.class))
							            	{
												val = SCloner.clone(val, procs);
												//System.out.println("cloned val: "+val);
							            	}
											((IntermediateFuture)ret).addIntermediateResult(val);
							            	
							            })
							        	.finished(Void -> ((IntermediateFuture)ret).setFinished())
							        	.catchEx(ret);
						        	}
						        	else if(ret instanceof IFuture)
						        	{
						        		((IFuture)invokeMethod(comp, pojo, myargs, method)).then(val ->
							            {
							            	if(!hasAnnotation(method.getAnnotatedReturnType().getAnnotations(), NoCopy.class))
							            	{
												Object val2 = SCloner.clone(val, procs);
												//System.out.println("cloned val: "+val+" "+val2+" "+(val==val2));
												val = val2;
							            	}
											ret.setResult(val);
							            }).catchEx(ret);
						        	}
						        	else
						        	{
						        		invokeMethod(comp, pojo, myargs, method);
						        	}
						        }
						        else
						        {
						        	//System.out.println("scheduled on agent: "+getId());
						        	if(ret instanceof IIntermediateFuture)
						        	{
							        	((IIntermediateFuture)scheduleAsyncStep(new ICallable<>()
							            {
							        		public IFuture<Object> call() throws Exception
							        		{
							        			return (IFuture<Object>)invokeMethod(comp, pojo, myargs, method);
							        			//return (IFuture<Object>)method.invoke(pojo, myargs);
							        		}
							        		
							        		public Class<? extends IFuture<?>> getFutureReturnType() 
						        		    {
						        		    	return (Class<? extends IFuture<?>>)ret.getClass();
						        		    }
							            }))
							        	.next(val ->
							            {
							            	if(!hasAnnotation(method.getAnnotatedReturnType().getAnnotations(), NoCopy.class))
							            	{
												val = SCloner.clone(val, procs);
												//System.out.println("cloned val: "+val);
							            	}
											((IntermediateFuture)ret).addIntermediateResult(val);
							            	
							            })
							        	.finished(Void -> ((IntermediateFuture)ret).setFinished())
							        	.catchEx(ret);
						        	}
						        	else if(ret instanceof IFuture)
						        	{
						        		scheduleAsyncStep(() ->
							            {
							            	//return (IFuture<Object>)method.invoke(pojo, myargs);
							            	return (IFuture<Object>)invokeMethod(comp, pojo, myargs, method);
							            })
						        		.then(val ->
							            {
							            	if(!hasAnnotation(method.getAnnotatedReturnType().getAnnotations(), NoCopy.class))
							            	{
												Object val2 = SCloner.clone(val, procs);
												//System.out.println("cloned val: "+val+" "+val2+" "+(val==val2));
												val = val2;
							            	}
											ret.setResult(val);
							            }).catchEx(ret);
						        	}
						        	else
						        	{
						        		scheduleStep(() -> invokeMethod(comp, pojo, myargs, method));
						        	}
						        }
						        
						        return ret;
							}))
							.make()
							.load(pojo.getClass().getClassLoader())
			                .getLoaded()
			                .getConstructor()
			                .newInstance();
						
						return proxy;
					}
					catch(Exception e)
					{
						SUtil.rethrowAsUnchecked(e);
					}
					return null;
				}
				
				protected Object invokeMethod(Component component, Object pojo, List<Object> args, Method method)
				{
					// Try to guess parameters from given args or component internals.
					IParameterGuesser guesser = new SimpleParameterGuesser(component.getValueProvider().getParameterGuesser(), args);
					Object[] iargs = new Object[method.getParameterTypes().length];
					for(int i=0; i<method.getParameterTypes().length; i++)
					{
						iargs[i] = guesser.guessParameter(method.getParameterTypes()[i], false);
					}
					
					try
					{
						// It is now allowed to use protected/private component methods
						SAccess.setAccessible(method, true);
						return method.invoke(pojo, iargs);
					}
					catch(Exception e)
					{
						SUtil.rethrowAsUnchecked(e);
					}
					return null;
				}
				
				protected boolean hasAnnotation(Annotation[] pannos, Class<? extends Annotation> anntype) 
				{
				    for (Annotation anno : pannos) 
				    {
				        if (anno.annotationType().equals(anntype)) 
				        {
				            return true;
				        }
				    }
				    return false;
				}
				
				@Override
				public <T> IFuture<T> scheduleStep(Callable<T> step) 
				{
					return comp.getFeature(IExecutionFeature.class).scheduleStep(step);
				}
				
				@Override
				public void scheduleStep(Runnable step) 
				{
					comp.getFeature(IExecutionFeature.class).scheduleStep(step);
				}
				
				@Override
				public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
				{
					return comp.getFeature(IExecutionFeature.class).scheduleStep(step);
				}
				
				@Override
				public void scheduleStep(IThrowingConsumer<IComponent> step)
				{
					comp.getFeature(IExecutionFeature.class).scheduleStep(step);
				}
				
				@Override
				public <T> IFuture<T> scheduleAsyncStep(Callable<IFuture<T>> step)
				{
					return comp.getFeature(IExecutionFeature.class).scheduleAsyncStep(step);
				}
				
				@Override
				public <T> IFuture<T> scheduleAsyncStep(IThrowingFunction<IComponent, IFuture<T>> step)
				{
					return comp.getFeature(IExecutionFeature.class).scheduleAsyncStep(step);
				}
			};
		}, true);
	}
	
	@Override
	public Class<IExecutionFeature> getFeatureType()
	{
		return IExecutionFeature.class;
	}

	@Override
	public IExecutionFeature createFeatureInstance(Component self)
	{
		ExecutionFeature	ret	= ExecutionFeature.LOCAL.get();

		// Component created without bootstrapping
		// TODO: disallow plain component creation?
		if(ret==null || ret.self!=null)
		{
			ret = doCreateFeatureInstance();				
		}
		// else inside bootstrap -> reuse bootstrap feature
		
		assert ret.self==null;
		ret.self	= self;
		return ret;
	}

	/**
	 *  Template method allowing subclasses to provide a subclass of the feature implementation.
	 */
	protected ExecutionFeature doCreateFeatureInstance()
	{
		return new ExecutionFeature();
	}
	
	@Override
	public <T extends Component> T	bootstrap(Class<T> type, Supplier<T> creator)
	{
		Map<Class<Object>, ComponentFeatureProvider<Object>>	providers	= SComponentFeatureProvider.getProvidersForComponent(type);
		Object	exeprovider	= providers.get(IExecutionFeature.class);	// Hack!!! cannot cast wtf???
		IExecutionFeature	exe	= ((ExecutionFeatureProvider)exeprovider).doCreateFeatureInstance();
		Future<T>	ret	= new Future<>();

		// Fast Lambda Agent -> optimized lifecycle
		if(SReflect.isSupertype(FastLambda.class, type))
		{
			exe.scheduleStep(() -> 
			{
				@SuppressWarnings("unchecked")
				FastLambda<Object> self = (FastLambda<Object>)creator.get();
				startFeatures(self);
				
				// run body and termination in same step as init
				try
				{
					/*ILifecycle lfeature = (ILifecycle)feature;
					System.out.println("starting: "+lfeature);
					lfeature.onStart();*/
					
					Object	result	= self.getPojo().apply(self);
					if(self.result!=null)
						self.result.setResult(result);
				}
				catch(Exception e)
				{
					self.handleException(e);
				}
				if(self.terminate)
				{
					exe.scheduleStep((Runnable)() -> self.terminate());
				}
				
				@SuppressWarnings("unchecked")
				T t	= (T)self;
				ret.setResult(t);
			});
		}
		
		// Normal component
		else
		{
			exe.scheduleStep(() -> 
			{
				T self = creator.get();
				startFeatures(self);
				ret.setResult(self);
			});
		}
		
		return ret.get();			
	}
	
	/**
	 *  Start all features, i.e. thos that implement ILifecycle.
	 */
	protected <T extends Component> void startFeatures(T self)
	{
		for(Object feature:	self.getFeatures())
		{
			if(feature instanceof ILifecycle)
			{
				ILifecycle lfeature = (ILifecycle)feature;
				//System.out.println("starting: "+lfeature);
				lfeature.onStart();
			}
		}
	}
	
	
	@Override
	public boolean isCreator(Object obj) 
	{
		return Runnable.class.isAssignableFrom(obj.getClass())
			|| Supplier.class.isAssignableFrom(obj.getClass())
			|| IThrowingFunction.class.isAssignableFrom(obj.getClass())
			|| LambdaPojo.class.isAssignableFrom(obj.getClass());
	}
	
	@Override
	public IExternalAccess create(Object pojo, ComponentIdentifier cid, Application app)
	{
		IExternalAccess ret;
		
		if(pojo instanceof Runnable)
		{
			ret = LambdaAgent.create((Runnable)pojo, cid, app);
		}
		else if(pojo instanceof Callable)
		{
			ret = LambdaAgent.create((Callable<?>)pojo, cid, app).component();
		}
		else if(pojo instanceof IThrowingFunction)
		{
			@SuppressWarnings("unchecked")
			IThrowingFunction<IComponent, ?>	itf	= (IThrowingFunction<IComponent, ?>)pojo;
			ret = LambdaAgent.create(itf, cid, app).component();
		}
		else if(pojo instanceof IThrowingConsumer)
		{
			@SuppressWarnings("unchecked")
			IThrowingConsumer<IComponent>	itc	= (IThrowingConsumer<IComponent>)pojo;
			ret = LambdaAgent.create(itc, cid, app);
		}
		else if(pojo instanceof LambdaPojo)
		{
			ret = LambdaAgent.create((LambdaPojo<?>)pojo, cid, app);
		}
		else
		{
			throw new RuntimeException("Cannot create lambda agent from: "+cid);
		}
	
		return ret;
	}
	
	/*@Override
	public boolean isTerminator(IComponent component) 
	{
		return component.getClass().equals(MjComponent.class);
	}*/
		
	@Override
	public void terminate(IComponent component) 
	{
		((IInternalExecutionFeature)component.getFeature(IExecutionFeature.class)).terminate();
	}
	
	public Map<String, Object> getResults(Object pojo)
	{
		Map<String, Object> ret = new HashMap<String, Object>();
		if(pojo instanceof LambdaPojo)
		{
			LambdaPojo<?> lp = (LambdaPojo<?>)pojo;
			ret.put("result", lp.getResult());
		}
		return ret;
	}
}
