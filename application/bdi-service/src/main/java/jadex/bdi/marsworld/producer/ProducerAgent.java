package jadex.bdi.marsworld.producer;

import jadex.bdi.annotation.Body;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Plans;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.marsworld.BaseAgent;
import jadex.bdi.marsworld.carry.ICarryService;
import jadex.bdi.marsworld.environment.BaseObject;
import jadex.bdi.marsworld.environment.Producer;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability.WalkAround;
import jadex.bdi.marsworld.sentry.ITargetAnnouncementService;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

@Agent(type="bdip")
@Service
@ProvidedServices(@ProvidedService(type=IProduceService.class))
@RequiredServices({
	@RequiredService(name="targetser", type=ITargetAnnouncementService.class), 
	@RequiredService(name="carryser", type=ICarryService.class) 
})
@Plans({
	@Plan(trigger=@Trigger(goals=ProducerAgent.ProduceOre.class), body=@Body(ProduceOrePlan.class)),
	@Plan(trigger=@Trigger(factadded="movecapa.mytargets"), body=@Body(InformNewTargetPlan.class))
})
public class ProducerAgent extends BaseAgent implements IProduceService
{
	public ProducerAgent(String envid)
	{
		super(envid);
	}
	
	/**
	 *  Produce ore goal.
	 */
	@Goal(deliberation=@Deliberation(inhibits=WalkAround.class, cardinalityone=true))
	public class ProduceOre
	{
		/** The target. */
		protected Target target;

		/**
		 *  Create a new CarryOre. 
		 */
		public ProduceOre(Target target)
		{
			this.target = target;
		}
		
		@GoalDropCondition(beliefs="movecapa.missionend")
		public boolean checkDrop()
		{
			return movecapa.isMissionend();
		}

		/**
		 *  Get the target.
		 *  @return The target.
		 */
		public Target getTarget()
		{
			return target;
		}
	}
	
	/**
	 *  Produce ore.
	 */
	public IFuture<Void> doProduce(Target target)
	{
		// It is important to add the target to the capability as the targets are updated
		// from the environment
		movecapa.addTarget(target);
//		System.out.println("producer received produce command: "+target);
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new ProduceOre(target));
		return IFuture.DONE;
	}
	
	protected BaseObject createSpaceObject()
	{
		return new Producer(getAgent().getId().getLocalName(), getMoveCapa().getHomebase().getPosition());
	}
}
