package jadex.bdi.marsworld.movement;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAPI;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanCapability;
import jadex.bdi.annotation.PlanReason;
import jadex.bdi.marsworld.BaseAgent;
import jadex.bdi.marsworld.environment.BaseObject;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.future.IFuture;

/**
 *  The move to a location plan.
 */
@Plan
public class MoveToLocationPlan 
{
	//-------- attributes --------

	@PlanCapability
	protected MovementCapability capa;
	
	@PlanAPI
	protected IPlan rplan;
	
	@PlanReason
	protected IDestinationGoal goal;
	
	/**
	 *  The plan body.
	 */
	@PlanBody
	public IFuture<Void> body(IComponent agent)
	{
		BaseObject myself = capa.getMyself();
		MarsworldEnvironment env = (MarsworldEnvironment)capa.getEnvironment();
		IVector2 dest = goal.getDestination();

		System.out.println("MoveToLocation start: "+capa.getMyself()+" "+dest);
		
		//env.rotate(myself, dest).get();
		
		env.move(myself, dest, myself.getSpeed()).get();
		
		System.out.println("MoveToLocation end: "+capa.getMyself()+" "+((BaseAgent)agent.getPojo()).getSpaceObject(true));

		
		return IFuture.DONE;
	}
	
//	@PlanAborted
//	public void aborted()
//	{
//		System.out.println("aborted: "+this);
//	}
//	
//	@PlanFailed
//	public void failed(Exception e)
//	{
//		if(e!=null)
//			e.printStackTrace();
//		System.out.println("failed: "+this);
//	}
//	
//	@PlanPassed
//	public void passed()
//	{
//		System.out.println("passed: "+this);
//	}
}