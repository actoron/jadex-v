package jadex.bdi.cleanerworld.cleaner;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IPlan;
import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.ExcludeMode;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalContextCondition;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalInhibit;
import jadex.bdi.annotation.GoalMaintainCondition;
import jadex.bdi.annotation.GoalSelectCandidate;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.cleanerworld.environment.Chargingstation;
import jadex.bdi.cleanerworld.environment.Cleaner;
import jadex.bdi.cleanerworld.environment.CleanerworldEnvironment;
import jadex.bdi.cleanerworld.environment.Waste;
import jadex.bdi.cleanerworld.environment.Wastebin;
import jadex.bdi.cleanerworld.ui.SensorGui;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.core.IComponent;
import jadex.environment.Environment;
import jadex.environment.PerceptionProcessor;
import jadex.environment.SpaceObject;
import jadex.execution.ComponentMethod;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.math.IVector2;
import jadex.math.Vector2Double;

/**
 *  Cleaner agent.
 */
@BDIAgent
public class CleanerAgent
{
	//-------- beliefs that can be used in plan and goal conditions --------
	
	@Inject
	private IComponent agent;
	
	/** Set of the known wastes. Managed by SensorActuator object. */
	@Belief
	private Set<Waste> wastes = new LinkedHashSet<>();
	
	/** Set of the known waste bins. Managed by SensorActuator object. */
	@Belief
	private Set<Wastebin> wastebins = new LinkedHashSet<>();
	
	/** Set of the known charging stations. Managed by SensorActuator object. */
	@Belief
	private Set<Chargingstation> stations	= new LinkedHashSet<>();
	
	/** Set of the known other cleaners. Managed by SensorActuator object. */
	@Belief
	private Set<Cleaner> others = new LinkedHashSet<>();
	
	/** Knowledge about myself. */
	@Belief
	private Val<Cleaner> self = new Val<Cleaner>((Cleaner)null);
	
	/** Day or night?. Use updaterate to re-check every second. */
	@Belief(updaterate=1000)
	private Val<Boolean> daytime = new Val<>(() -> getEnvironment().isDaytime().get());
	
	private boolean	sensorgui	= true;
	
	private CleanerworldEnvironment env;
	
	public CleanerAgent()
	{
	}
	
	public CleanerAgent(String envid)
	{
		this.env = (CleanerworldEnvironment)Environment.get(envid);
	}
	
	private IComponent getAgent()
	{
		return agent;
	}
	
	private CleanerworldEnvironment getEnvironment()
	{
		return env;
	}
	
	private Cleaner getSelf()
	{
		return self.get();
	}
	
	//-------- simple example behavior --------
	
	/**
	 *  The body is executed when the agent is started.
	 *  @param bdifeature Provides access to bdi specific methods
	 */
	@OnStart
	private void exampleBehavior()
	{
		//System.out.println("RUNNING ON START");
		
//		SwingUtilities.invokeLater(() -> new BDIViewer(agent.getComponentHandle()).setVisible(true));
		
		Cleaner s = new Cleaner(new Vector2Double(Math.random()*0.4+0.3, Math.random()*0.4+0.3), getAgent().getId().getLocalName(), 0.1, 0.1, 0.8);  
		s = (Cleaner)getEnvironment().addSpaceObject((SpaceObject)s).get();
		self.set(s);
		
		PerceptionProcessor pp = new PerceptionProcessor();
		
		pp.manage(Waste.class, wastes);
		pp.manage(Wastebin.class, wastebins);
		pp.manage(Chargingstation.class, stations);
		pp.manage(Cleaner.class, others, obj -> 
		{
			if(obj.equals(getSelf()))
				getSelf().updateFrom(obj);
			else
				PerceptionProcessor.findAndUpdateOrAdd(obj, others);
		}, obj -> others.remove(obj), obj -> {if(obj.getPosition()==null) others.remove(obj);});
		
		getEnvironment().observeObject((Cleaner)getSelf()).next(e ->
		{
			agent.getFeature(IExecutionFeature.class).scheduleStep(() ->
			{
				pp.handleEvent(e);
			});
		});
		
		// Open a window showing the agent's perceptions
		if(sensorgui)
			new SensorGui(agent.getComponentHandle()).setVisible(true);
		
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new PerformLookForWaste());
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new PerformPatrol());
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new MaintainBatteryLoaded());
	}
	
	/**
	 *  Goal for keeping the battery loaded.
	 */
	@Goal(deliberation=@Deliberation(inhibits={PerformLookForWaste.class, AchieveCleanupWaste.class/*, PerformPatrol.class*/}))
	public class MaintainBatteryLoaded
	{
		/**
		 *  When the chargestate is below 0.2
		 *  the cleaner will activate this goal.
		 */
		@GoalMaintainCondition(beliefs="self")
		public boolean checkMaintain()
		{
			//System.out.println("check maintain: "+getSelf().getChargestate());
			return getSelf().getChargestate()>0.2;
		}
		
		/**
		 *  The target condition determines when
		 *  the goal goes back to idle. 
		 */
		@GoalTargetCondition(beliefs="self")
		public boolean checkTarget()
		{
			//System.out.println("check target: "+getSelf().getChargestate());
			return getSelf().getChargestate()>=1;
		}
	}
	
	@Goal(excludemode=ExcludeMode.Never)
	public class QueryChargingStation
	{
		protected Chargingstation station;
		
		@GoalTargetCondition(beliefs="stations")
		public boolean checkTarget()
		{
			station = findClosestElement(stations, getSelf().getLocation());
			return station!=null;
		}
		
		/**
		 *  Get the station.
		 *  @return The station.
		 */
		public Chargingstation getStation()
		{
			return station;
		}

		/**
		 *  Set the station.
		 *  @param station The station to set.
		 */
		public void setStation(Chargingstation station)
		{
			this.station = station;
		}
	}
	
	/**
	 *  Goal that lets the agent perform patrol rounds.
	 */
	@Goal(excludemode=ExcludeMode.Never, orsuccess=false)
	public class PerformPatrol
	{
		/**
		 *  Suspend the goal when daytime.
		 */
		@GoalContextCondition(beliefs="daytime")
		public boolean checkContext()
		{
			return !daytime.get();
		}
		
		@GoalSelectCandidate
		ICandidateInfo	chooseMove(IBDIAgentFeature bdi, List<ICandidateInfo> cands)
		{
			return cands.get((int)(Math.random()*cands.size()));
		}
	}
	
	//-------- look for waste --------
		
	/**
	 *  Declare a goal using an inner class with @Goal annotation.
	 *  Use ExcludeMode.Never and orsuccess=false to keep executing the same plan(s) over and over.
	 */
	@Goal(excludemode=ExcludeMode.Never, orsuccess=false)
	private class PerformLookForWaste
	{
		/**
		 *  Monitor day time and restart moving when night is gone.
		 */
		@GoalContextCondition(beliefs="daytime")
		private boolean context()
		{
			return daytime.get();
		}
	}
	
	@Goal(excludemode=ExcludeMode.Never)
	private class QueryWastebin
	{
//		@GoalResult
		protected Wastebin wastebin;
		
		@GoalTargetCondition(beliefs="wastebins")
		public boolean checkTarget()
		{
			wastebin = findClosestElement(wastebins, getSelf().getLocation());
			return wastebin!=null;
		}

		public Wastebin getWastebin() 
		{
			return wastebin;
		}

//		public void setWastebin(Wastebin wastebin) 
//		{
//			this.wastebin = wastebin;
//		}
	}
	
	//-------- cleanup waste --------
	
	//@Goal(recur=true, recurdelay=3000,deliberation=@Deliberation(inhibits=PerformPatrol.class, cardinalityone=true))
	@Goal(deliberation=@Deliberation(inhibits={PerformLookForWaste.class/*, AchieveCleanupWaste.class*/}/*, cardinalityone=true*/)/*, unique=true*/)
	private class AchieveCleanupWaste
	{
		private Waste	waste;
		
		@GoalCreationCondition(factadded="wastes")
		public AchieveCleanupWaste(Waste waste)
		{
			//System.out.println("achieve cleanup: "+waste);
			this.waste	= waste;
		}
		
		// The goal is achieved, when the waste is gone.
		@GoalTargetCondition(beliefs={"self", "wastes"})
		boolean	isClean()
		{
			// Test if the waste is not believed to be in the environment
			return !wastes.contains(waste)
				// and also not the waste we just picked up.
				&& !waste.equals(getSelf().getCarriedWaste());
		}
		
		// Goal should only be pursued when carrying no waste
		// or when goal is resumed after recharging and carried waste is of this goal.
		@GoalContextCondition(beliefs={"daytime"/*, "self"*/})
		boolean isPossible()
		{
			return daytime.get() /*&& (self.getCarriedWaste()==null || waste.equals(self.getCarriedWaste()))*/;
		}
		
//		@Override
//		public boolean equals(Object obj)
//		{
//			return obj instanceof AchieveCleanupWaste && ((AchieveCleanupWaste)obj).waste.equals(waste);
//		}
		
		public Waste getWaste() 
		{
			return waste;
		}

//		@Override
//		public int hashCode()
//		{
//			return 31+waste.hashCode();
//		}
		
		@GoalInhibit(AchieveCleanupWaste.class)
		private boolean	inhibitOther(AchieveCleanupWaste other)
		{
			// Inhibit all other goals when currently carrying the waste of this goal
			return waste.equals(getSelf().getCarriedWaste()) || 
				// Otherwise inhibit other goals where waste is further away
				(!other.waste.equals(getSelf().getCarriedWaste()) && other.waste.getLocation()!=null
					&& waste.getLocation().getDistance(getSelf().getLocation()).getAsDouble()
						< other.waste.getLocation().getDistance(getSelf().getLocation()).getAsDouble());
		}
	}
	
	@Plan(trigger=@Trigger(goals=AchieveCleanupWaste.class))
	private void cleanupWaste(IPlan plan, AchieveCleanupWaste goal)
	{
		//System.out.println("Starting cleanupWaste() plan");
		Waste waste = goal.getWaste();
		
		// Move to waste and pick it up, if not yet done
		if(!waste.equals(getSelf().getCarriedWaste()))
		{
			getEnvironment().move(getSelf(), waste.getLocation()).get();
			getEnvironment().pickupWaste(getSelf(), waste).get();
		}
		
		// Dispatch a subgoal to find a waste bin
		QueryWastebin	querygoal = new QueryWastebin();
		plan.dispatchSubgoal(querygoal).get();
		Wastebin wastebin = querygoal.getWastebin();
		
		// Move to waste bin as provided by subgoal
		getEnvironment().move(getSelf(), wastebin.getLocation()).get();
		
		// Finally drop the waste into the bin
		getEnvironment().dropWasteInWastebin(getSelf(), waste, wastebin).get();
	}
	
	@Plan(trigger=@Trigger(goals=MaintainBatteryLoaded.class))
	private void loadBattery(CleanerAgent agentapi, IPlan planapi)
	{
//		QueryChargingStation sg = (QueryChargingStation)planapi.dispatchSubgoal((new QueryChargingStation())).get();	//WTF!?
		QueryChargingStation sg = new QueryChargingStation();
		planapi.dispatchSubgoal(sg).get();
		Chargingstation station = (Chargingstation)sg.getStation();
		
		//System.out.println("Moving to station: "+station);
		getEnvironment().move(getSelf(), station.getLocation()).get();
		
		getEnvironment().loadBattery(getSelf(), (Chargingstation)station).get();
	}
	
	/**
	 *  Declare a plan using an inner class with @Plan anmd @Trigger annotation
	 *  and a method with @PlanBody annotation.
	 */
	@Plan(trigger=@Trigger(goals={PerformLookForWaste.class, QueryWastebin.class, QueryChargingStation.class}))
	private void doMoveAround()
	{
		getEnvironment().move((Cleaner)getSelf(), new Vector2Double(Math.random(), Math.random())).get();
	}
	
	/**
	 *  Declare a plan for the PerformPatrol goal by using a method with @Plan and @Trigger annotation.
	 */
	@Plan(trigger=@Trigger(goals=PerformPatrol.class))	
	private void performPatrolPlan()
	{
		// Follow a simple path around the four corners of the museum and back to the first corner.
		//System.out.println("Starting performPatrolPlan()");
		getEnvironment().move(getSelf(), new Vector2Double(0.1, 0.1)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.1, 0.9)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.9, 0.9)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.9, 0.1)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.1, 0.1)).get();
	}

	/**
	 *  Declare a second plan for the PerformPatrol goal.
	 */
	@Plan(trigger=@Trigger(goals=PerformPatrol.class))
	private void performPatrolPlan2()
	{
		// Follow another path around the middle of the museum.
		//System.out.println("Starting performPatrolPlan2()");
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.7)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.7)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.3)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3)).get();
	}
	
	/**
	 *  Declare a third plan for the PerformPatrol goal.
	 */
	@Plan(trigger=@Trigger(goals=PerformPatrol.class))
	private void performPatrolPlan3()
	{
		// Follow a zig-zag path in the museum.
		//System.out.println("Starting performPatrolPlan3()");
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.7)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.7)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.3)).get();
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3)).get();
	}
	
	public static <T extends SpaceObject> T findClosestElement(Set<T> elements, IVector2 loc) 
	{
		return elements.stream().min(Comparator.comparingDouble(p -> distance(p.getPosition(), loc))).orElse(null);
	}
	
	public static double distance(IVector2 p1, IVector2 p2) 
	{
		double dx = p1.getX().getAsDouble()-p2.getX().getAsDouble();
		double dy = p1.getY().getAsDouble()-p2.getY().getAsDouble();
        //return Math.sqrt(dx*dx + dy*dy);
		return dx*dx+dy*dy; // speed optimized
	}

	@ComponentMethod
	public IFuture<Set<Waste>> getWastes() 
	{
		return new Future<>(wastes);
	}

	@ComponentMethod
	public IFuture<Set<Wastebin>> getWastebins() 
	{
		return new Future<>(wastebins);
	}

	@ComponentMethod
	public IFuture<Set<Chargingstation>> getStations() 
	{
		return new Future<>(stations);
	}

	@ComponentMethod
	public IFuture<Set<Cleaner>> getCleaners() 
	{
		return new Future<>(others);
	}
	
	@ComponentMethod
	public IFuture<Cleaner> getCleaner() 
	{	
		return new Future<>(getSelf());
	}

	@ComponentMethod
	public IFuture<Boolean> isDaytime() 
	{
		return new Future<>(daytime.get());
	}	
	
	@ComponentMethod
	public IFuture<IVector2> getTarget() 
	{
		return getEnvironment().getTarget(getSelf());
	}
}
