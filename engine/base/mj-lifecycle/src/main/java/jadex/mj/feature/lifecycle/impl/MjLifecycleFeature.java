package jadex.mj.feature.lifecycle.impl;

import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.execution.impl.IMjInternalExecutionFeature;
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
				IMjLifecycle lfeature = (IMjLifecycle)feature;
				lfeature.onEnd().get();
			}
		});
		
		// invoke terminate on execution feature
		((IMjInternalExecutionFeature)self.getFeature(IMjExecutionFeature.class)).terminate();
	}
}
