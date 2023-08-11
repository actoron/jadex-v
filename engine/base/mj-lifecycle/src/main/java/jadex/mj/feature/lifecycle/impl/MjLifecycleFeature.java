package jadex.mj.feature.lifecycle.impl;

import java.util.function.Supplier;

import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.execution.impl.MjExecutionFeature;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;

public class MjLifecycleFeature	implements IMjLifecycleFeature
{
	public static void	bootstrap(Class<? extends MjComponent> type, Supplier<? extends MjComponent> creator)
	{
		MjExecutionFeature.bootstrap(type, () ->
		{
			MjComponent	self	= creator.get();
			self.getFeatures().forEach(feature ->
			{
				if(feature instanceof IMjLifecycle)
				{
					self.getFeature(IMjExecutionFeature.class)
						.scheduleStep(() ->
					{
						IMjLifecycle	lfeature	= (IMjLifecycle)feature;
						lfeature.onStart();

						self.getFeature(IMjExecutionFeature.class)
							.scheduleStep(() ->
						{
							lfeature.onBody();
						});
					});
				}
			});
			return self;
		});
	}
	
	protected MjComponent	self;
	
	protected MjLifecycleFeature(MjComponent self)
	{
		this.self	= self;		
	}
		
	public void terminate()
	{
		self.getFeatures().forEach(feature ->
		{
			if(feature instanceof IMjLifecycle) 
			{
				self.getFeature(IMjExecutionFeature.class).scheduleStep(()->
				{
					IMjLifecycle	lfeature	= (IMjLifecycle)feature;
					lfeature.onEnd();
				});
			}
		});
	}
}
