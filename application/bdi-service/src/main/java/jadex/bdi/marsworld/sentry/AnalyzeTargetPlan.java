package jadex.bdi.marsworld.sentry;

import java.util.Collection;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAPI;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanCapability;
import jadex.bdi.annotation.PlanReason;
import jadex.bdi.marsworld.environment.MarsworldEnvironment;
import jadex.bdi.marsworld.environment.Sentry;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability;
import jadex.bdi.marsworld.movement.MovementCapability.Move;
import jadex.bdi.marsworld.producer.IProduceService;
import jadex.bdi.marsworld.sentry.SentryAgent.AnalyzeTarget;
import jadex.bdi.runtime.IPlan;
import jadex.future.IFuture;
import jadex.requiredservice.IRequiredServiceFeature;


/**
 *  Inform the sentry agent about a new target.
 */
@Plan
public class AnalyzeTargetPlan 
{
	//-------- attributes --------

	@PlanCapability
	protected SentryAgent sentry;
	
	@PlanAPI
	protected IPlan rplan;
	
	@PlanReason
	protected AnalyzeTarget goal;
	
	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
//		System.out.println("analyze target plan start");
		
		MovementCapability capa = sentry.getMoveCapa();
		MarsworldEnvironment env = (MarsworldEnvironment)capa.getEnvironment();
		Target target = goal.getTarget();
		

		// Move to the target.
		Move move = sentry.getMoveCapa().new Move(target.getPosition());
		rplan.dispatchSubgoal(move).get();

		// Analyse the target.
		try
		{
			Sentry myself = (Sentry)sentry.getSpaceObject(true);
			
			env.analyzeTarget(myself, target).get();
			
			//System.out.println("Analyzed target: "+getAgentName()+", "+ore+" ore found.");
			if(target.getOre()>0)
				callProducerAgent(target);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			// Fails for one agent, when two agents try to analyze the same target at once.
		}
	}

	/**
	 *  Sending a location to the Producer Agent.
	 *  Therefore it has first to be looked up in the DF.
	 *  @param target
	 */
	private void callProducerAgent(Target target)
	{
//		System.out.println("Calling some Production Agent...");

		try
		{
			IFuture<Collection<IProduceService>> fut = sentry.getAgent().getFeature(IRequiredServiceFeature.class).getServices("produceser");
			Collection<IProduceService> ansers = fut.get();
			
			for(IProduceService anser: ansers)
			{
				anser.doProduce(target);
			}
		}
		catch(RuntimeException e)
		{
			System.out.println("No producer found");
		}
	}
}
