package jadex.mj.feature.lifecycle.impl;

import java.util.function.Supplier;

import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.IBootstrapping;
import jadex.mj.core.impl.IComponentTerminator;
import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;

public class MjLifecycleFeatureProvider extends MjFeatureProvider<IMjLifecycleFeature>	implements IBootstrapping
{
	static
	{
		IComponent.addComponentTerminator(new IComponentTerminator() 
		{
			public boolean filter(MjComponent component) 
			{
				return component.getClass().equals(MjComponent.class);
			}
			
			@Override
			public void terminate(IComponent component) 
			{
				component.getFeature(IMjLifecycleFeature.class).terminate();
			}
		});
	}
	
	@Override
	public Class<IMjLifecycleFeature> getFeatureType()
	{
		return IMjLifecycleFeature.class;
	}

	@Override
	public IMjLifecycleFeature createFeatureInstance(MjComponent self)
	{
		return new MjLifecycleFeature(self);
	}
	
	public <T extends MjComponent> T	bootstrap(Class<T> type, Supplier<T> creator)
	{
		T	self	= creator.get();
		self.getFeatures().forEach(feature ->
		{
			if(feature instanceof IMjLifecycle)
			{
				self.getFeature(IMjExecutionFeature.class)
					.scheduleStep(() ->
				{
					IMjLifecycle lfeature = (IMjLifecycle)feature;
					lfeature.onStart();
				});
			}
		});
		return self;
	}
}
