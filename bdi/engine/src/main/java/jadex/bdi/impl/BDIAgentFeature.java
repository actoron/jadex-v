package jadex.bdi.impl;

import jadex.bdi.IBDIAgentFeature;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.rules.eca.RuleSystem;

public class BDIAgentFeature implements IBDIAgentFeature, ILifecycle
{
	/** The component. */
	protected BDIAgent	self;
	
	/** The rule system. */
	protected RuleSystem	rulesystem;
	
	/**
	 *  Create the feature.
	 */
	public BDIAgentFeature(BDIAgent self)
	{
		this.self	= self;
		this.rulesystem	= new RuleSystem(self);
	}
	
	//-------- ILifecycle interface --------
	
	@Override
	public void onStart()
	{
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BDIStepListener(rulesystem));
	}
	
	@Override
	public void onEnd()
	{
	}
	
	//-------- internal methods --------
	
	/**
	 *  Get the rule system.
	 */
	public RuleSystem	getRuleSystem()
	{
		return this.rulesystem;
	}	
}
