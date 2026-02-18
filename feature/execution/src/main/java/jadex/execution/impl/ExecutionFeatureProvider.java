package jadex.execution.impl;

import java.lang.reflect.AnnotatedType;
import java.util.concurrent.Callable;

import jadex.common.ErrorException;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.ICallable;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentFeatureProvider;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SComponentFeatureProvider;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;

public class ExecutionFeatureProvider extends ComponentFeatureProvider<IExecutionFeature>	implements IBootstrapping, IComponentLifecycleManager
{
	static
	{		
		// Init the component with schedule step functionality (hack?!)
		Component.setExternalAccessFactory(comp ->
		{
			return new ExecutableComponentHandle(comp);
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

		// TODO: disallow plain component creation?
		if(ret==null || ret.self!=self)
		{
			ret = doCreateFeatureInstance(self);				
		}
		// else inside bootstrap -> reuse bootstrap feature
		
		return ret;
	}

	/**
	 *  Template method allowing subclasses to provide a subclass of the feature implementation.
	 */
	protected ExecutionFeature doCreateFeatureInstance(Component self)
	{
		return new ExecutionFeature(self);
	}
	
	@Override
	public <T extends Component> IFuture<IComponentHandle>	bootstrap(T component)
	{
		IExecutionFeature	exe	= doCreateFeatureInstance(component);
		
		// Fast Lambda Agent -> optimized lifecycle
		if(component instanceof FastLambda)
		{
			@SuppressWarnings("unchecked")
			FastLambda<Object> fself	= (FastLambda<Object>)component;
			AnnotatedType	type	= ExecutionFeature.getReturnType(fself.getPojo());
			@SuppressWarnings("rawtypes")
			ICallable	step	= new ICallable()
			{
				@Override
				public AnnotatedType getReturnType()
				{
					return type;
				}
				
				@Override
				public Object call() throws Exception
				{
					try
					{
						// Extra init so component doesn't get added when just created as object
						fself.init();
						
						// run body and termination in same step as init
						Object	result	= null;
						Object	pojo	= fself.getPojo();
						if(pojo instanceof Callable)
						{
							result	= ((Callable<?>)pojo).call();							
						}
						else if(pojo instanceof IThrowingFunction)
						{
							@SuppressWarnings("unchecked")
							IThrowingFunction<IComponent, T>	itf	= (IThrowingFunction<IComponent, T>)pojo;
							result	= itf.apply(fself);							
						}
						else if(pojo instanceof Runnable)
						{
							((Runnable)pojo).run();							
						}
						else //if(pojo instanceof IThrowingConsumer)
						{
							@SuppressWarnings("unchecked")
							IThrowingConsumer<IComponent>	itc	= (IThrowingConsumer<IComponent>)pojo;
							itc.accept(fself);							
						}
						
						if(!FastLambda.KEEPALIVE)
						{
							if(fself.isAsync())
							{
								@SuppressWarnings("unchecked")
								IFuture<Object>	resfut	= (IFuture<Object>) result;
								resfut.then(v -> fself.doTerminate())
									.catchEx(ex ->
								{
									fself.handleException(ex);
									// Terminate, if not terminated by exception handler (default terminate -> StepAborted)
									fself.doTerminate();									
								});
							}
							else
							{
								exe.scheduleStep((Runnable)() -> fself.doTerminate());
							}
						}
						
						return result;
					}
					catch(Exception e)
					{
						exe.scheduleStep((Runnable)() ->
						{
							fself.handleException(e);
							// Terminate, if not terminated by exception handler (default terminate -> StepAborted)
							fself.doTerminate();
						});
						throw e;
					}
					catch(Error e)
					{
						exe.scheduleStep((Runnable)() ->
						{
							fself.handleException(new ErrorException(e));
							// Terminate, if not terminated by exception handler (default terminate -> StepAborted)
							fself.doTerminate();
						});
						throw e;
					}
					finally
					{
						ComponentManager.get().decreaseCreating(fself.getId(), fself.getApplication());
					}
				}
			};
			ComponentManager.get().increaseCreating(component.getApplication());
//			System.out.println("Creating fast lambda agent: "+type);
			@SuppressWarnings("unchecked")
			IFuture<Object>	resfut	= fself.isAsync() ? exe.scheduleAsyncStep(step) : exe.scheduleStep(step);
			fself.setResultFuture(resfut);
			
			// No handle needed, because the user only waits for the run() result
			return null;
		}
		
		// Normal component
		else
		{
			ComponentManager.get().increaseCreating(component.getApplication());
			return exe.scheduleStep(() -> 
			{
				try
				{
					// Extra init so component doesn't get added when just created as object
					component.init();
					
					// if lambda agent -> schedule body step before making the component handle available
					// cf. ResultTest.testSubscriptionAfterTerminate()
					Object	pojo		= component.getPojo();
					if(pojo!=null && SComponentFeatureProvider.getCreator(pojo.getClass())==this)
					{
						Runnable	step;
						if(pojo instanceof Callable)
						{
							step	= () ->
							{
								try
								{
									Object	result	= ((Callable<?>)pojo).call();
									// Fail if no result feature available
									Component.setResult(component, "result", result, ExecutionFeature.getReturnAnnotations(pojo));
								}
								catch(Exception e)
								{
									// Force exception handling inside component and not in scheduleStep() return future.
									component.handleException(e);
								}
							};
						}
						else if(pojo instanceof IThrowingFunction)
						{
							step	= () ->
							{
								try
								{
									@SuppressWarnings("unchecked")
									IThrowingFunction<IComponent, T>	itf	= (IThrowingFunction<IComponent, T>)pojo;
									Object result	= itf.apply(component);
									// Fail if no result feature available
									Component.setResult(component, "result", result, ExecutionFeature.getReturnAnnotations(pojo));
								}
								catch(Exception e)
								{
									// Force exception handling inside component and not in scheduleStep() return future.
									component.handleException(e);
								}
							};
						}
						else if(pojo instanceof Runnable)
						{
							step		= (Runnable) pojo;					
						}
						else //if(pojo instanceof IThrowingConsumer)
						{
							step	= () ->
							{
								try
								{
									@SuppressWarnings("unchecked")
									IThrowingConsumer<IComponent>	itc	= (IThrowingConsumer<IComponent>)pojo;
									itc.accept(component);							
								}
								catch(Exception e)
								{
									// Force exception handling inside component and not in scheduleStep() return future.
									component.handleException(e);
								}
							};
						}
						component.getFeature(IExecutionFeature.class).scheduleStep(step);
					}
					
					return component.getComponentHandle();
				}
				finally
				{
					ComponentManager.get().decreaseCreating(component.getId(), component.getApplication());
				}
			});
		}
	}
	
	@Override
	public int	isCreator(Class<?> pojoclazz)
	{
		return Runnable.class.isAssignableFrom(pojoclazz)
			|| Callable.class.isAssignableFrom(pojoclazz)
			|| IThrowingFunction.class.isAssignableFrom(pojoclazz)
			|| IThrowingConsumer.class.isAssignableFrom(pojoclazz)
			? 1 : -1;
	}
	
	@Override
	public IFuture<IComponentHandle>	create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new Component(pojo, cid, app));
	}	
	
	@Override
	public <T> IFuture<T> run(Object pojo, ComponentIdentifier cid, Application app, boolean async)
	{
		if(pojo instanceof Callable
		|| pojo instanceof IThrowingFunction
		|| pojo instanceof Runnable
		|| pojo instanceof IThrowingConsumer)
		{
			FastLambda<T>	comp	= new FastLambda<>(pojo, cid, app, async);
			Component.createComponent(comp);
			return comp.getResultFuture();
		}
		else
		{
			return new Future<>(new RuntimeException("Cannot run lambda agent from: "+pojo));
		}
	}
}
