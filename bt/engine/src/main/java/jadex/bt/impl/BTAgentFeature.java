package jadex.bt.impl;

import jadex.bt.BTAgent;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.micro.impl.MicroAgentFeature;

public class BTAgentFeature	extends MicroAgentFeature implements ILifecycle
{
	public static BTAgentFeature get()
	{
		return IExecutionFeature.get().getComponent().getFeature(BTAgentFeature.class);
	}

	protected BTAgentFeature(BTAgent self)
	{
		super(self);
	}
	
	@Override
	public void	onStart()
	{
		super.onStart();
	}
	
	@Override
	public void	onEnd()
	{
		super.onEnd();
	}

}
