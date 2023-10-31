package jadex.execution.impl;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SFeatureProvider;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.future.IFuture;

public class ExecutionFeatureProvider extends FeatureProvider<IExecutionFeature>	implements IBootstrapping, IComponentLifecycleManager
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
				@Override
				public ComponentIdentifier getId()
				{
					return comp.getId();
				}
				
				@Override
				public boolean isExecutable()
				{
					return true;
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
			};
		});
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
		Map<Class<Object>, FeatureProvider<Object>>	providers	= SFeatureProvider.getProvidersForComponent(type);
		Object	exeprovider	= providers.get(IExecutionFeature.class);	// Hack!!! cannot cast wtf???
		IExecutionFeature	exe	= ((ExecutionFeatureProvider)exeprovider).doCreateFeatureInstance();
		return exe.scheduleStep(() -> 
		{
			T	self	= creator.get();
			self.getFeatures().forEach(feature ->
			{
				if(feature instanceof ILifecycle)
				{
					exe.scheduleStep(() ->
					{
						ILifecycle lfeature = (ILifecycle)feature;
						lfeature.onStart();
					});
				}
			});
			return self;
		}).get();
	}
	
	
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
}
