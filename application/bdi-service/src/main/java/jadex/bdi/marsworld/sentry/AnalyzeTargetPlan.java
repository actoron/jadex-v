package jadex.bdi.marsworld.sentry;

import java.util.Collection;

import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.PlanAPI;
import jadex.bdi.annotation.PlanBody;
import jadex.bdi.annotation.PlanCapability;
import jadex.bdi.annotation.PlanFinished;
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
import jadex.future.ITerminableFuture;
import jadex.requiredservice.IRequiredServiceFeature;


/**
 *  Inform the sentry agent about a new target.
 */
@Plan
public class AnalyzeTargetPlan 
{
	//-------- attributes --------

	@PlanCapability
	public SentryAgent sentry;
	
	@PlanAPI
	protected IPlan rplan;
	
	@PlanReason
	protected AnalyzeTarget goal;
	
	protected ITerminableFuture<Void> task;
	
	/**
	 *  The plan body.
	 */
	@PlanBody
	public void body()
	{
		System.out.println("analyze target plan start: "+sentry.getAgent().getId());
		
		MovementCapability capa = sentry.getMoveCapa();
		MarsworldEnvironment env = (MarsworldEnvironment)capa.getEnvironment();
		Target target = goal.getTarget();

		//System.out.println("move to target: "+target);

		// Move to the target.
		Move move = sentry.getMoveCapa().new Move(target.getPosition());
		rplan.dispatchSubgoal(move).get();
		
		//System.out.println("reached target, analyzing: "+target);

		// Analyse the target.
		try
		{
			//Sentry myself = (Sentry)sentry.getSpaceObject(true);
			
			task = env.analyzeTarget((Sentry)sentry.getSpaceObject(), target);
			task.get();
			task = null;
			//sentry.getMoveCapa().updateTarget(target);
			
			//System.out.println("Analyzed target: "+sentry.getAgent().getId()+", "+target.getOre()+" ore found.");
			if(target.getOre()>0)
				callProducerAgent(target);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			// Fails for one agent, when two agents try to analyze the same target at once.
		}
		
		System.out.println("analyze target plan end: "+sentry.getAgent().getId());
	}

	/**
	 *  Sending a location to the Producer Agent.
	 *  Therefore it has first to be looked up in the DF.
	 *  @param target
	 */
	private void callProducerAgent(Target target)
	{
		System.out.println("Calling some Production Agent...");

		IFuture<Collection<IProduceService>> fut = sentry.getAgent().getFeature(IRequiredServiceFeature.class).getServices("produceser");
		Collection<IProduceService> ansers = fut.get();
		
		if(ansers.size()==0)
			System.out.println("No producer found");
		
		for(IProduceService anser: ansers)
		{
			anser.doProduce(target);
		}
	}
	
	@PlanFinished
	protected void finished()
	{
		//System.out.println("plan finished: "+this);
		if(task!=null)
		{
			System.out.println("aborting env task");
			task.terminate();
		}
	}
}
