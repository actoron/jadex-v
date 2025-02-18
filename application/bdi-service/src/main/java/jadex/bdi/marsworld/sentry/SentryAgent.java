package jadex.bdi.marsworld.sentry;

import javax.swing.SwingUtilities;

import jadex.bdi.annotation.Body;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalContextCondition;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Plans;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.marsworld.BaseAgent;
import jadex.bdi.marsworld.environment.Sentry;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.movement.MovementCapability;
import jadex.bdi.marsworld.producer.IProduceService;
import jadex.bdi.runtime.ChangeEvent;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.tool.BDIViewer;
import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.environment.BaseObject;
import jadex.future.IFuture;
import jadex.math.IVector2;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;

@Agent(type="bdip")
@Service
@Plans(
{
	@Plan(trigger=@Trigger(goals=SentryAgent.AnalyzeTarget.class), body=@Body(AnalyzeTargetPlan.class))
})
@ProvidedServices(@ProvidedService(type=ITargetAnnouncementService.class))
@RequiredServices(@RequiredService(name="produceser", type=IProduceService.class)) //multiple=true,
public class SentryAgent extends BaseAgent implements ITargetAnnouncementService
{
	public SentryAgent(String envid)
	{
		super(envid);
	}
	
	@OnStart
	public void start(IComponent agent)
	{
		super.body();
		SwingUtilities.invokeLater(() -> new BDIViewer(agent.getComponentHandle()).setVisible(true));
	}
	
	public IFuture<Void> announceNewTarget(Target target)
	{
		//System.out.println("Sentry was informed about new target: "+target);
		movecapa.addTarget(target);
		//System.out.println("sentry has targets: "+movecapa.getMyTargets());
		return IFuture.DONE;
	}
	
	protected BaseObject createSpaceObject()
	{
		return new Sentry(getAgent().getId().getLocalName(), getMoveCapa().getHomebase().getPosition());
	}

	@Goal(unique=true, deliberation=@Deliberation(inhibits=MovementCapability.WalkAround.class, cardinalityone=true))
	public static class AnalyzeTarget
	{
		/** The sentry agent. */
		protected SentryAgent outer;
		
		/** The target. */
		protected Target target;

		/**
		 *  Create a new AnalyzeTarget. 
		 */
		// todo: allow direct goal creation on fact added
//		@GoalCreationCondition(events="movecapa.mytargets")
		public AnalyzeTarget(SentryAgent outer, Target target)
		{
			//System.out.println("new analyze target goal: "+target);
//			if(target==null)
//				System.out.println("target nulls");
			this.outer = outer;
			this.target = target;
		}
		
		// todo: support directly factadded etc.
		@GoalCreationCondition(beliefs="movecapa.mytargets")
		public static AnalyzeTarget checkCreate(SentryAgent outer, Target target, ChangeEvent event)
		{
			if(target==null)// ||  outer.getMoveCapa().isMissionend())
				return null;
//			System.out.println(":: "+event);
			
			AnalyzeTarget ret = null;
			if(target.getStatus()==Target.Status.Unknown)
				ret = new AnalyzeTarget(outer, target);
			return ret;
		}

		@GoalContextCondition(beliefs="movecapa.mytargets")
		public boolean checkContext()
		{
			IVector2 mypos = outer.getMoveCapa().getMyself().getPosition();
			Target nearest = null;
			IVector2 npos = null;
			for(Target so: outer.getMoveCapa().getMyTargets())
			{
				if(so.getStatus()==Target.Status.Unknown)
				{
					if(nearest==null)
					{
						nearest = so;
						npos = nearest.getPosition();
					}
					else
					{
						IVector2 spos = so.getPosition();
						if(mypos.getDistance(spos).getAsDouble() < mypos.getDistance(npos).getAsDouble())
						{
							nearest = so;
							npos = nearest.getPosition();
						}
					}
				}
			}
			
			boolean ret = nearest!=null && nearest.equals(target);
			
			if(!ret)
			{
				boolean found = false;
				for(IGoal g: outer.getAgent().getFeature(IBDIAgentFeature.class).getGoals())
				{
					if(g.getPojo() instanceof AnalyzeTarget)
					{
						if(((AnalyzeTarget)g.getPojo()).getTarget().equals(nearest))
						{
							found = true;
							break;
						}
						/*else
						{
							System.out.println("analyze goal: "+((AnalyzeTarget)g.getPojo()).getTarget());
						}*/
					}
				}
				if(!found)
					System.out.println("nearest has no goal: "+nearest+" "+nearest.equals(target)+" "+outer.getMoveCapa().getMyTargets());
			}
			
			//System.out.println("context cond for: "+target+" "+ret+" nearest: "+nearest);
			
			return ret;
			
			// (select one Target $target from $beliefbase.my_targets
			// order by $beliefbase.my_location.getDistance($target.getLocation()))
			// == $goal.target
		}
		
		@GoalDropCondition(beliefs="movecapa.missionend")
		public boolean checkDrop()
		{
			//System.out.println("check ndropping: "+this+" "+outer.getMoveCapa().isMissionend());
			return outer.getMoveCapa().isMissionend();
		}
		
		/*@GoalFinished
		public void finished()
		{
			System.out.println("goal finished: "+this+" "+this.getOuter().getAgent().getId());
		}*/

		/**
		 *  Get the target.
		 *  @return The target.
		 */
		public Target getTarget()
		{
			return target;
		}
		
		/**
		 *  Get the outer.
		 *  @return The outer.
		 */
		public SentryAgent getOuter()
		{
			return outer;
		}
		
		// hashcode and equals implementation for unique flag
		
		/**
		 *  Get the hashcode.
		 */
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + outer.getClass().hashCode();
			result = prime * result + ((target == null) ? 0 : target.hashCode());
			return result;
		}

		/**
		 *  Test if equal to other goal.
		 *  @param obj The other object.
		 *  @return True, if equal.
		 */
		public boolean equals(Object obj)
		{
			boolean ret = false;
			if(obj instanceof AnalyzeTarget)
			{
				AnalyzeTarget other = (AnalyzeTarget)obj;
				ret = outer.getClass().equals(other.getOuter().getClass()) && SUtil.equals(target, other.getTarget());
			}
			return ret;
		}
	}

}

