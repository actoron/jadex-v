package jadex.bdi.marsworld.movement;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAPI;
import jadex.bdi.annotation.PlanAborted;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanCapability;
import jadex.bdi.annotation.PlanFinished;
import jadex.bdi.annotation.PlanReason;
import jadex.bdi.marsworld.BaseAgent;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.runtime.IPlan;
import jadex.core.IComponent;
import jadex.environment.BaseObject;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.math.IVector2;

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
	
	protected ITerminableFuture<Void> task;
	
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
		
		task = env.move(myself, dest, myself.getSpeed());
		task.get();
		task = null;
		
		//System.out.println("MoveToLocation end: "+capa.getMyself()+" "+((BaseAgent)agent.getPojo()).getSpaceObject(true));
		
		return IFuture.DONE;
	}
	
	@PlanFinished
	public void finished()
	{
		//System.out.println("fini: "+this);
		//capa.updateSelf(); // update the position in myself
		
		if(task!=null)
		{
			//System.out.println("terminate task: "+task);
			task.terminate();
		}
	}
//	
//	@PlanFailed
//	public void failed(Exception e)
//	{
//		if(e!=null)
//			e.printStackTrace();
//		System.out.println("failed: "+this);
//	}

}