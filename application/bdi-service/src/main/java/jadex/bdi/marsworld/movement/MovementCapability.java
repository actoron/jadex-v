package jadex.bdi.marsworld.movement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Body;
import jadex.bdi.annotation.Capability;
import jadex.bdi.annotation.ExcludeMode;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Plans;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.marsworld.BaseAgent;
import jadex.bdi.marsworld.environment.BaseObject;
import jadex.bdi.marsworld.environment.Environment;
import jadex.bdi.marsworld.environment.Homebase;
import jadex.bdi.marsworld.environment.Target;
import jadex.bdi.marsworld.math.IVector2;
import jadex.bdi.runtime.ICapability;
import jadex.bdi.runtime.Val;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;

/**
 * 
 */
@Capability
@Plans({
	@Plan(trigger=@Trigger(goals={MovementCapability.Move.class, MovementCapability.Missionend.class}), body=@Body(MoveToLocationPlan.class)),
	@Plan(trigger=@Trigger(goals=MovementCapability.WalkAround.class), body=@Body(RandomWalkPlan.class))
})
public class MovementCapability
{
	//-------- attributes --------

	/** The agent. */
	@Agent
	protected IComponent agent;
	
	// Annotation to inform FindBugs that the uninitialized field is not a bug.
	//@SuppressFBWarnings(value="UR_UNINIT_READ", justification="Agent field injected by interpreter")
	
	/** The capability. */
	@Agent
	protected ICapability capa;
	
	/** The mission end. */
//	@Belief(dynamic=true, updaterate=1000) 
	@Belief(updaterate=1000) 
	protected Val<Boolean> missionend = new Val<Boolean>(() -> 
	{
		//System.out.println("missionend: "+getHomebase()+" "+getHomebase().getMissionTime());
		return getHomebase().getMissionTime()<=System.currentTimeMillis();
	}); // todo: clock
	//protected boolean missionend = ((Long)env.getSpaceObjectsByType(Homebase.class).get(0).getProperty("missiontime")).longValue()<=getTime();

	/** The targets. */
	@Belief
	protected List<Target> mytargets = new ArrayList<Target>();
	
	/**
	 *  The move goal.
	 *  Move to a certain location.
	 */
	@Goal
	public class Move implements IDestinationGoal
	{
		/** The destination. */
		protected IVector2 destination;

		/**
		 *  Create a new Move. 
		 */
		public Move(IVector2 destination)
		{
			this.destination = destination;
		}

		/**
		 *  Get the destination.
		 *  @return The destination.
		 */
		public IVector2 getDestination()
		{
			return destination;
		}
	}
	
	/**
	 *  The walk goal.
	 *  Walk around without target when nothing else to do.
	 */
	@Goal(orsuccess=false, excludemode=ExcludeMode.Never)
	public class WalkAround
	{
		/**
		 *  Drop condition.
		 *  @return True if should be dropped.
		 */
		@GoalDropCondition(beliefs="missionend")
		public boolean checkDrop()
		{
			//System.out.println("walk around drop check: "+agent.getId().getLocalName());
			return missionend.get();
		}
	}
	
	/**
	 *  The mission end goal.
	 *  Move to homebase on end.
	 */
	@Goal(unique=true)
	public static class Missionend implements IDestinationGoal
	{
		/** The movement capability. */
		protected MovementCapability capa;
		
		/**
		 *  Create a new goal.
		 */
		public Missionend(MovementCapability capa)
		{
			this.capa = capa;
		}
		
		/**
		 *  Create a new Move. 
		 */
		@GoalCreationCondition(beliefs="missionend")
		public static boolean checkCreate(MovementCapability capa)
		{
			return capa.missionend.get() && !capa.getMyself().getPosition().equals(capa.getHomebasePosition());
		}
		
		/**
		 *  Get the destination.
		 *  @return The destination.
		 */
		public IVector2 getDestination()
		{
			return capa.getHomebasePosition();
		}
	}

	/**
	 *  Get the homebase position.
	 *  @return The homebase position.
	 */
	public IVector2 getHomebasePosition()
	{
		return getHomebase().getPosition();
	}
	
	/**
	 *  Get the homebase.
	 *  @return The homebase.
	 */
	public Homebase getHomebase()
	{
		//System.out.println("homebase: "+getEnvironment().getSpaceObjectsByType(Homebase.class));
		return getEnvironment().getSpaceObjectsByType(Homebase.class).toArray(new Homebase[0])[0];
	}
	
	/**
	 * 
	 * /
	protected long getTime()
	{
		IClockService cs = capa.getAgent().getFeature(IRequiredServicesFeature.class).getLocalService(IClockService.class);
		// todo: capa.getAgent().getComponentFeature().getService() does not work in init expressions only from plans :-(
//		IClockService cs =  (IClockService)capa.getAgent().getComponentFeature(IRequiredServicesFeature.class).getService("clockser").get();
		return cs.getTime();
	}*/
	
	/**
	 *  Get the env.
	 *  @return The env.
	 */
	public Environment getEnvironment()
	{
		return ((BaseAgent)agent.getPojo()).getEnvironment();
	}

	/**
	 *  Get the myself.
	 *  @return The myself.
	 */
	public BaseObject getMyself()
	{
		return ((BaseAgent)agent.getPojo()).getSpaceObject();
	}

	/**
	 *  Get the capa.
	 *  @return The capa.
	 */
	public ICapability getCapability()
	{
		return capa;
	}

	/**
	 *  Get the my_targets.
	 *  @return The my_targets.
	 */
	public List<Target> getMyTargets()
	{
		return mytargets;
	}

	/**
	 *  Get the missionend.
	 *  @return The missionend.
	 */
	public boolean isMissionend()
	{
		return missionend.get();
	}
	
	public void addTarget(Target target)
	{
		if(!mytargets.contains(target))
		{
			//System.out.println("added target: "+get+" "+target);
			mytargets.add(target);
		}
	}
}
