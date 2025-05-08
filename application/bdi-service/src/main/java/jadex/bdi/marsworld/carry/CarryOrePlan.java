package jadex.bdi.marsworld.carry;

import jadex.bdi.IPlan;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.marsworld.carry.CarryAgent.CarryOre;
import jadex.bdi.marsworld.environment.Carry;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability;
import jadex.bdi.marsworld.movement.MovementCapability.Move;
import jadex.future.ITerminableFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;

/**
 *  Inform the sentry agent about a new target.
 */
@Plan
public class CarryOrePlan 
{
	//-------- attributes --------

	@Inject
	protected CarryAgent carry;
	
	@Inject
	protected IPlan rplan;
	
	@Inject
	protected CarryOre goal;
	
	protected ITerminableFuture<Void> task;
	
	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{	
		Target target = (Target)goal.getTarget();
		boolean	finished = false;
		MovementCapability capa = carry.getMoveCapa();
		
		Carry myself = (Carry)capa.getMyself();
		
		while(!finished)
		{
			MarsworldEnvironment env = (MarsworldEnvironment)capa.getEnvironment();
			
			// Move to the target.
			Move move = capa.new Move(target.getPosition());
			rplan.dispatchSubgoal(move).get();
	
			// Load ore at the target.
			//System.out.println("load start");
			task = env.load(myself, target);
			task.get();
			//System.out.println("load end");
			//System.out.println("myself: "+myself.getPosition());
			
			// Move to the homebase.
			move = capa.new Move(capa.getHomebase().getPosition());
			rplan.dispatchSubgoal(move).get();
			//System.out.println("myself: "+myself.getPosition());
			
			// Unload ore at homebase
			//System.out.println("unload start");
			task = env.unload(myself, capa.getHomebase());
			task.get();
			task = null;
			//System.out.println("unload end");
			
			finished = target.getOre()==0;
			//if(finished)
			//	System.out.println("carry ore plan finished: "+carry.getAgent().getId());
		}
	}
	
	@OnEnd
	protected void finished()
	{
		//carry.getMoveCapa().updateSelf();
		//carry.getMoveCapa().updateTarget(goal.getTarget());
		
		//System.out.println("plan finished: "+this);
		if(task!=null)
		{
			//System.out.println("aborting env task");
			task.terminate();
		}
	}
}
