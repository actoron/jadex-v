package jadex.bdi.marsworld.carry;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAPI;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanCapability;
import jadex.bdi.annotation.PlanReason;
import jadex.bdi.marsworld.carry.CarryAgent.CarryOre;
import jadex.bdi.marsworld.environment.Carry;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability;
import jadex.bdi.marsworld.movement.MovementCapability.Move;
import jadex.bdi.runtime.IPlan;

/**
 *  Inform the sentry agent about a new target.
 */
@Plan
public class CarryOrePlan 
{
	//-------- attributes --------

	@PlanCapability
	protected CarryAgent carry;
	
	@PlanAPI
	protected IPlan rplan;
	
	@PlanReason
	protected CarryOre goal;
	
	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{	
		Target target = (Target)goal.getTarget();
		boolean	finished = false;
		MovementCapability capa = carry.getMoveCapa();
		
		while(!finished)
		{
			MarsworldEnvironment env = (MarsworldEnvironment)capa.getEnvironment();
			
			// Move to the target.
			Move move = capa.new Move(target.getPosition());
			rplan.dispatchSubgoal(move).get();
	
			// Load ore at the target.
			Carry myself = (Carry)capa.getMyself();
			
			env.load(myself, target).get();
			
			env.unload(myself, target).get();
			
			/*Future<Void> fut = new Future<Void>();
			DelegationResultListener<Void> lis = new DelegationResultListener<Void>(fut, true);
//			myself.addTask(new LoadOreTask(target, true, res));
			Map props = new HashMap();
			props.put(LoadOreTask.PROPERTY_TARGET, target);
			props.put(LoadOreTask.PROPERTY_LOAD, Boolean.TRUE);
			props.put(AbstractTask.PROPERTY_CONDITION, new PlanFinishedTaskCondition(rplan));
			Object	taskid	= env.createObjectTask(LoadOreTask.PROPERTY_TYPENAME, props, myself.getId());
			env.addTaskListener(taskid, myself.getId(), lis);
			fut.get();*/
			
			/*System.out.println("Loaded ore at target: "+getAgentName()+", "+ore+" ore loaded.");
			// Todo: use return value to determine finished state?
			finished = ((Number)target.getProperty(ProduceOreTask.PROPERTY_CAPACITY)).intValue()==0;
			if(((Number)myself.getProperty(AnalyzeTargetTask.PROPERTY_ORE)).intValue()==0)
				break;
	
			// Move to the homebase.
			move = capa.new Move(capa.getHomebasePosition());
			rplan.dispatchSubgoal(move).get();
			
			// Unload ore at the homebase.
			fut = new Future<Void>();
			lis = new DelegationResultListener<Void>(fut, true);
			props = new HashMap();
			props.put(LoadOreTask.PROPERTY_TARGET, capa.getHomebase());
			props.put(LoadOreTask.PROPERTY_LOAD, Boolean.FALSE);
			props.put(AbstractTask.PROPERTY_CONDITION, new PlanFinishedTaskCondition(rplan));
			taskid	= env.createObjectTask(LoadOreTask.PROPERTY_TYPENAME, props, myself.getId());
			env.addTaskListener(taskid, myself.getId(), lis);
			fut.get();
//			System.out.println("Unloaded ore at homebase: "+getAgentName()+", "+ore+" ore unloaded.");*/
		}
	}
}
