package jadex.bdi.marsworld.producer;

import java.util.Collection;

import jadex.bdi.IPlan;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.marsworld.carry.ICarryService;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Producer;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability;
import jadex.bdi.marsworld.movement.MovementCapability.Move;
import jadex.bdi.marsworld.producer.ProducerAgent.ProduceOre;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.requiredservice.IRequiredServiceFeature;


/**
 *  Inform the sentry agent about a new target.
 */
@Plan
public class ProduceOrePlan 
{
	//-------- attributes --------

	@Inject
	protected ProducerAgent producer;
	
	@Inject
	protected IPlan rplan;
	
	@Inject
	protected ProduceOre goal;
	
	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
		Target target = goal.getTarget();

		MovementCapability capa = producer.getMoveCapa();
		
		MarsworldEnvironment env = (MarsworldEnvironment)capa.getEnvironment();
		
		// Move to the target.
		Move move = capa.new Move(target.getPosition());
		rplan.dispatchSubgoal(move).get();
		
		Producer myself = (Producer)capa.getMyself();
		
		env.produce(myself, target).get();
		//producer.getMoveCapa().updateTarget(target);
		
		//System.out.println("Produced ore at target: "+producer.getAgent().getId()+", "+target.getCapacity()+" ore produced.");
		
		callCarryAgent(target);
	}

	/**
	 *  Sending a location to the Producer Agent.
	 *  Therefore it has first to be looked up in the DF.
	 *  @param target
	 */
	private void callCarryAgent(Target target)
	{
//		System.out.println("Calling some Production Agent...");

		try
		{
			IFuture<Collection<ICarryService>> fut = producer.getAgent().getFeature(IRequiredServiceFeature.class).searchServices(ICarryService.class);
			Collection<ICarryService> ansers = fut.get();
			
			for(ICarryService anser: ansers)
			{
				anser.doCarry(target);
			}
		}
		catch(RuntimeException e)
		{
			System.out.println("No carry found");
		}
	}
}
