package jadex.bdi.marsworld.carry;

import java.util.Collection;

import jadex.bdi.IPlan;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.sentry.ITargetAnnouncementService;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Inform the sentry agent about a new target.
 */
@Plan
public class InformNewTargetPlan 
{
	@Inject
	protected CarryAgent carry;
	
	@Inject
	protected IPlan rplan;
	
	@Inject
	protected Target target;
	
	//-------- methods --------

	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
		//System.out.println("inform new target: "+carry.getAgent().getId()+" "+target);
		
		IFuture<Collection<ITargetAnnouncementService>> fut = carry.getAgent().getFeature(IRequiredServiceFeature.class).searchServices(ITargetAnnouncementService.class);
		Collection<ITargetAnnouncementService> ansers = fut.get();
		
		if(ansers.size()==0)
			System.out.println("No target announcement services found");
		
		for(ITargetAnnouncementService anser: ansers)
		{
			anser.announceNewTarget(target);
		}
		
//		System.out.println("Informing sentries: "+getScope().getAgentName());
	}
}
