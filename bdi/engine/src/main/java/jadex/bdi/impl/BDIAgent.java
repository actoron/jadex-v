package jadex.bdi.impl;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.impl.plan.RPlan;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.Component;

/**
 *  Component subtype to select BDI features only for BDI agents.
 */
public class BDIAgent extends Component
{
	/**
	 *  Create the component.
	 */
	public BDIAgent(Object pojo, ComponentIdentifier id, Application app)
	{
		super(pojo, id, app);
	}
	
	@Override
	public void doTerminate()
	{
		// When called from plan -> abort first and call terminate() again later.
		RPlan	current_plan	= RPlan.RPLANS.get();
		if(current_plan!=null)
		{
			// Set flag so execute plan step action knows it needs to call terminate again
			current_plan.setTerminate(true);
			current_plan.abort();
		}
		else
		{
			// Call @PlanAborted code manually before injection feature cleanup calls @OnEnds
			((BDIAgentFeature) getFeature(IBDIAgentFeature.class)).abortPlans();
			super.doTerminate();
		}
	}
}
