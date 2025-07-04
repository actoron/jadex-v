package jadex.bdi.marsworld.movement;

import java.util.ArrayList;
import java.util.List;

import jadex.bdi.Val;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.ExcludeMode;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalDropCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.marsworld.BaseAgent;
import jadex.bdi.marsworld.environment.BaseObject;
import jadex.bdi.marsworld.environment.Homebase;
import jadex.bdi.marsworld.environment.Target;
import jadex.core.IComponent;
import jadex.environment.Environment;
import jadex.environment.SpaceObject;
import jadex.injection.annotation.Inject;
import jadex.math.IVector2;

/**
 * 
 */
// TODO: use BDI annotation?
//@BDIAgent
@Plan(trigger=@Trigger(goals={MovementCapability.Move.class, MovementCapability.Missionend.class}), impl=MoveToLocationPlan.class)
@Plan(trigger=@Trigger(goals=MovementCapability.WalkAround.class), impl=RandomWalkPlan.class)
public class MovementCapability
{
	//-------- attributes --------

	/** The agent. */
	@Inject
	protected IComponent agent;
	
	// Annotation to inform FindBugs that the uninitialized field is not a bug.
	//@SuppressFBWarnings(value="UR_UNINIT_READ", justification="Agent field injected by interpreter")
	
//	/** The capability. */
//	@Inject
//	protected ICapability capa;
	
	/** The mission end. */
//	@Belief(dynamic=true, updaterate=1000) 
	@Belief(updaterate=1000)
	protected final Val<Boolean> missionend = new Val<Boolean>(() -> 
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
		@GoalDropCondition
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
	@Goal/*(unique=true)*/
	public static class Missionend implements IDestinationGoal
	{
		/** The movement capability. */
		protected MovementCapability capa;
		
		/**
		 *  Create a new goal.
		 */
		public Missionend(MovementCapability capa)
		{
//			System.out.println("Missionend: "+capa.getMyself());
			this.capa = capa;
		}
		
		/**
		 *  Create a new Move. 
		 */
		@GoalCreationCondition(factchanged="missionend")
		public static boolean checkCreate(MovementCapability capa)
		{
			return capa.missionend.get();// && !capa.getMyself().getPosition().equals(capa.getHomebase().getPosition());
		}
		
		/**
		 *  Get the destination.
		 *  @return The destination.
		 */
		public IVector2 getDestination()
		{
			return capa.getHomebase().getPosition();
		}
	}

	/**
	 *  Get the homebase.
	 *  @return The homebase.
	 */
	public Homebase getHomebase()
	{
		//System.out.println("homebase: "+getEnvironment().getSpaceObjectsByType(Homebase.class).get());
		return getEnvironment().getSpaceObjectsByType(Homebase.class).get().toArray(new Homebase[0])[0];
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

//	/**
//	 *  Get the capa.
//	 *  @return The capa.
//	 */
//	public ICapability getCapability()
//	{
//		return capa;
//	}

	/**
	 *  Get the my_targets.
	 *  @return The my_targets.
	 */
	public List<Target> getMyTargets()
	{
		return mytargets;
	}
	
	/**
	 *  Check if the mission is over.
	 */
	public boolean isMissionEnd()
	{
		return missionend.get();
	}

	public void addTarget(Target target)
	{
		if(!mytargets.contains(target))
		{
			//System.out.println("added target: "+agent.getId()+" "+target);
			mytargets.add(target);
		}
		/*else
		{
			System.out.println("target known: "+agent.getId()+" "+target);//+" "+mytargets);
		}*/
	}
	
	public void updateTarget(Target target)
	{
		//mytargets.remove(target);  // does not work, creates another goal
		Target t = getTarget(target);
		t.updateFrom((Target)updateSpaceObject(target));
		//mytargets.add(target);
	}
	
	public SpaceObject updateSpaceObject(SpaceObject so)
	{
		SpaceObject ret = (Target)getEnvironment().getSpaceObject(so.getId()).get();
		return ret;
	}
	
	protected Target getTarget(Target target)
	{
		Target ret = null;
		for(Target t: mytargets)
		{
			if(t.equals(target))
			{
				ret = t;
				break;
			}
		}
		return ret;
	}
}
