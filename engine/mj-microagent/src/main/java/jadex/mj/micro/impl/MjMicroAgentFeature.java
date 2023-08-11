package jadex.mj.micro.impl;

import jadex.mj.feature.lifecycle.IMjLifecycleFeature;
import jadex.mj.feature.lifecycle.impl.IMjLifecycle;
import jadex.mj.micro.MjMicroAgent;

public class MjMicroAgentFeature	implements IMjLifecycle
{
	protected MjMicroAgent	self;
	
	protected MjMicroAgentFeature(MjMicroAgent self)
	{
		this.self	= self;
	}
	
	@Override
	public void onStart()
	{
		System.out.println("start: "+self);
	}
	
	@Override
	public void onBody()
	{
		System.out.println("body: "+self);
		IMjLifecycleFeature.of(self).terminate();
	}
	
	@Override
	public void onEnd()
	{
		System.out.println("end: "+self);
	}
}
