package jadex.bdi.marsworld.carry;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.marsworld.BaseAgent;
import jadex.bdi.marsworld.environment.BaseObject;
import jadex.bdi.marsworld.environment.Carry;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability.WalkAround;
import jadex.future.IFuture;

@BDIAgent
//@RequiredServices(@RequiredService(name="targetser", type=ITargetAnnouncementService.class)) 
@Plan(trigger=@Trigger(goals=CarryAgent.CarryOre.class), impl=CarryOrePlan.class)
@Plan(trigger=@Trigger(factadded="movecapa.mytargets"), impl=InformNewTargetPlan.class)
public class CarryAgent extends BaseAgent implements ICarryService
{
	public CarryAgent(String envid)
	{
		super(envid);
	}
	
	@Goal(deliberation=@Deliberation(inhibits=WalkAround.class, cardinalityone=true))
	public class CarryOre
	{
		protected Target target;

		public CarryOre(Target target)
		{
			this.target = target;
		}
		
		@GoalDropCondition(beliefs="movecapa.missionend")
		public boolean checkDrop()
		{
			return movecapa.isMissionend();
		}

		public Target getTarget()
		{
			return target;
		}
		
	}
	
	public IFuture<Void> doCarry(Target target)
	{
		// It is important to add the target to the capability as the targets are updated
		// from the environment
		movecapa.addTarget(target);
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new CarryOre(target));
		return IFuture.DONE;
	}
	
	protected BaseObject createSpaceObject()
	{
		return new Carry(getAgent().getId().getLocalName(), getMoveCapa().getHomebase().getPosition(), 20);
	}
}



