package jadex.mj.feature.execution.impl;

import java.util.Map;
import java.util.function.Supplier;

import jadex.future.IFuture;
import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.IExternalAccess;
import jadex.mj.core.IThrowingConsumer;
import jadex.mj.core.IThrowingFunction;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.IComponentCreator;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.execution.LambdaAgent;

public class MjExecutionFeatureProvider extends MjFeatureProvider<IMjExecutionFeature>	implements IBootstrapping
{
	static
	{
		IComponent.addComponentCreator(new IComponentCreator() 
		{
			public boolean filter(Object obj) 
			{
				return Runnable.class.isAssignableFrom(obj.getClass())
					|| Supplier.class.isAssignableFrom(obj.getClass())
					|| IThrowingFunction.class.isAssignableFrom(obj.getClass());
			}
			
			public void create(Object pojo, ComponentIdentifier cid)
			{
				if(pojo instanceof Runnable)
					LambdaAgent.create((Runnable)pojo, cid);
				else if(pojo instanceof Supplier)
					LambdaAgent.create((Supplier<?>)pojo, cid);
				else if(pojo instanceof IThrowingFunction)
					LambdaAgent.create((IThrowingFunction<IComponent, ?>)pojo, cid);
			}
		});
		
		// Init the component with schedule step functionality (hack?!)
		MjComponent.setExternalAccessFactory(obj ->
		{
			IComponent tmp = obj instanceof IComponent? (IComponent)obj: null;
			
			if(obj instanceof ComponentIdentifier)
			{
				ComponentIdentifier cid = (ComponentIdentifier)obj;
				tmp = MjComponent.getComponent(cid);
			}
			
			final IComponent comp = tmp;
			
			return new IExternalAccess() 
			{
				@Override
				public ComponentIdentifier getId()
				{
					return comp.getId();
				}
				
				@Override
				public <T> IFuture<T> scheduleStep(Supplier<T> step) 
				{
					return comp.getFeature(IMjExecutionFeature.class).scheduleStep(step);
				}
				
				@Override
				public void scheduleStep(Runnable step) 
				{
					comp.getFeature(IMjExecutionFeature.class).scheduleStep(step);
				}
				
				@Override
				public <T> IFuture<T> scheduleStep(IThrowingFunction<IComponent, T> step)
				{
					return comp.getFeature(IMjExecutionFeature.class).scheduleStep(step);
				}
				
				@Override
				public void scheduleStep(IThrowingConsumer<IComponent> step)
				{
					comp.getFeature(IMjExecutionFeature.class).scheduleStep(step);
				}
			};
		});
	}
	
	@Override
	public Class<IMjExecutionFeature> getFeatureType()
	{
		return IMjExecutionFeature.class;
	}

	@Override
	public IMjExecutionFeature createFeatureInstance(MjComponent self)
	{
		MjExecutionFeature	ret;
		// called from outside bootstrap to schedule initial step -> create new feature
		if(self==null)
		{
			ret	= doCreateFeatureInstance();
		}
		else
		{
			// called from component constructor 
			ret	= MjExecutionFeature.LOCAL.get();
			// Component created without bootstrapping
			// TODO: disallow plain component creation?
			if(ret==null)
			{
				ret = doCreateFeatureInstance();				
			}
			// else inside bootstrap -> reuse bootstrap feature
			assert ret.self==null;
			ret.self	= self;
		}
		return ret;
	}

	/**
	 *  Template method allowing subclasses to provide a subclass of the feature implementation.
	 */
	protected MjExecutionFeature doCreateFeatureInstance()
	{
		return new MjExecutionFeature();
	}
	
	@Override
	public <T extends MjComponent> T	bootstrap(Class<T> type, Supplier<T> creator)
	{
		Map<Class<Object>, MjFeatureProvider<Object>>	providers	= SMjFeatureProvider.getProvidersForComponent(type);
		MjFeatureProvider<Object>	exeprovider	= providers.get(IMjExecutionFeature.class);
		IMjExecutionFeature	exe	= (IMjExecutionFeature)exeprovider.createFeatureInstance(null);
		return exe.scheduleStep(() -> creator.get()).get();
	}
}
