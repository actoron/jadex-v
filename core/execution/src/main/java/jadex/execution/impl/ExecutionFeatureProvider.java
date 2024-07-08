package jadex.execution.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.core.IThrowingConsumer;
import jadex.core.IThrowingFunction;
import jadex.core.LambdaPojo;
import jadex.core.impl.Component;
import jadex.core.impl.FeatureProvider;
import jadex.core.impl.IBootstrapping;
import jadex.core.impl.IComponentLifecycleManager;
import jadex.core.impl.SFeatureProvider;
import jadex.execution.IExecutionFeature;
import jadex.execution.LambdaAgent;
import jadex.future.Future;
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
				public String getAppId()
				{
					return comp.getAppId();
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
		Future<T>	ret	= new Future<>();
		exe.scheduleStep(() -> 
		{
			T self = creator.get();
			for(Object feature:	self.getFeatures())
			{
				if(feature instanceof ILifecycle)
				{
					exe.scheduleStep(() ->
					{
						ILifecycle lfeature = (ILifecycle)feature;
						//System.out.println("starting: "+lfeature);
						lfeature.onStart();
						return null;
					}).get();
				}
				/*else
				{
					System.out.println("feature without lifecycle: "+feature);
				}*/
			}
			ret.setResult(self);
		});
		return ret.get();
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
	public IExternalAccess create(Object pojo, ComponentIdentifier cid)
	{
		IExternalAccess ret;
		
		if(pojo instanceof Runnable)
			ret = LambdaAgent.create((Runnable)pojo, cid);
		else if(pojo instanceof Callable)
			ret = LambdaAgent.create((Callable<?>)pojo, cid).component();
		else if(pojo instanceof IThrowingFunction)
			ret = LambdaAgent.create((IThrowingFunction<IComponent, ?>)pojo, cid).component();
		else if(pojo instanceof LambdaPojo)
			ret = LambdaAgent.create((LambdaPojo<?>)pojo, cid);
		else
			throw new RuntimeException("Cannot create lambda agent from: "+cid);
	
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
