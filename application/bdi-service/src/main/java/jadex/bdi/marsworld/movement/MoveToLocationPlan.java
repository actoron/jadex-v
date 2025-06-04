package jadex.bdi.marsworld.movement;

import jadex.bdi.IPlan;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.marsworld.environment.BaseObject;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.math.IVector2;

/**
 *  The move to a location plan.
 */
@Plan
public class MoveToLocationPlan 
{
	//-------- attributes --------

	@Inject
	protected MovementCapability capa;
	
	@Inject
	protected IPlan rplan;
	
	@Inject
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

		//System.out.println("MoveToLocation start: "+capa.getMyself()+" "+dest);
		
		//env.rotate(myself, dest).get();
		
		env.move(myself, dest, myself.getSpeed()).get();
		
		//System.out.println("MoveToLocation end: "+capa.getMyself()+" "+((BaseAgent)agent.getPojo()).getSpaceObject(true));
		
		return IFuture.DONE;
	}
}