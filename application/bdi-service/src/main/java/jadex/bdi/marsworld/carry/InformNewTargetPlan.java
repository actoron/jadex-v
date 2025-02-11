package jadex.bdi.marsworld.carry;

import java.util.Collection;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAPI;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanCapability;
import jadex.bdi.annotation.PlanReason;
import jadex.bdi.marsworld.ITargetAnnouncementService;
import jadex.bdi.marsworld.environment.SpaceObject;
import jadex.bdi.runtime.IPlan;
import jadex.future.IFuture;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Inform the sentry agent about a new target.
 */
@Plan
public class InformNewTargetPlan 
{
	@PlanCapability
	protected CarryAgent carry;
	
	@PlanAPI
	protected IPlan rplan;
	
	@PlanReason
	protected SpaceObject target;
	
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
		try
		{
			IFuture<Collection<ITargetAnnouncementService>> fut = carry.getAgent().getFeature(IRequiredServiceFeature.class).getServices("targetser");
			Collection<ITargetAnnouncementService> ansers = fut.get();
			
			for(ITargetAnnouncementService anser: ansers)
			{
				anser.announceNewTarget(target);
			}
		}
		catch(Exception e)
		{
			System.out.println("No target announcement services found");
		}
		
//		System.out.println("Informing sentries: "+getScope().getAgentName());
	}
}
