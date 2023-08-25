package jadex.mj.feature.lifecycle.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;

public class MjLifecycleFeature	implements IMjLifecycleFeature
{
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