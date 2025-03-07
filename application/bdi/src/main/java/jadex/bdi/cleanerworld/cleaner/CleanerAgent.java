package jadex.bdi.cleanerworld.cleaner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingUtilities;

import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.annotation.ExcludeMode;
import jadex.bdi.annotation.Goal;
import jadex.bdi.annotation.GoalContextCondition;
import jadex.bdi.annotation.GoalCreationCondition;
import jadex.bdi.annotation.GoalInhibit;
import jadex.bdi.annotation.GoalMaintainCondition;
import jadex.bdi.annotation.GoalResult;
import jadex.bdi.annotation.GoalTargetCondition;
import jadex.bdi.annotation.Plan;
import jadex.bdi.annotation.Trigger;
import jadex.bdi.cleanerworld.environment.Chargingstation;
import jadex.bdi.cleanerworld.environment.Cleaner;
import jadex.bdi.cleanerworld.environment.CleanerworldEnvironment;
import jadex.bdi.cleanerworld.environment.Waste;
import jadex.bdi.cleanerworld.environment.Wastebin;
import jadex.bdi.cleanerworld.ui.SensorGui;
import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IPlan;
import jadex.bdi.runtime.Val;
import jadex.bdi.tool.BDIViewer;
import jadex.core.IComponent;
import jadex.environment.Environment;
import jadex.environment.SpaceObject;
import jadex.environment.SpaceObjectsEvent;
import jadex.environment.VisionEvent;
import jadex.execution.ComponentMethod;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.math.IVector2;
import jadex.math.Vector2Double;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.model.annotation.OnStart;

/**
 *  Cleaner agent.
 */
@Agent(type="bdip")
public class CleanerAgent
{
	//-------- beliefs that can be used in plan and goal conditions --------
	
	@Agent
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
	private Cleaner self = null;
	
	/** Day or night?. Use updaterate to re-check every second. */
	@Belief(updaterate=1000)
	private Val<Boolean> daytime = new Val(() -> getEnvironment().isDaytime().get());
	
	/** The patrol points. */
	@Belief
	protected List<IVector2> patrolpoints = new ArrayList<>();
	
	@AgentArgument
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
		return (Cleaner)self;
		//return (Cleaner)self.get();
	}
	
	//-------- simple example behavior --------
	
	/**
	 *  The body is executed when the agent is started.
	 *  @param bdifeature Provides access to bdi specific methods
	 */
	@OnStart
	private void exampleBehavior(IBDIAgentFeature bdifeature)
	{
		System.out.println("RUNNING ON START");
		
		SwingUtilities.invokeLater(() -> new BDIViewer(agent.getComponentHandle()).setVisible(true));
		
		Cleaner s = new Cleaner(new Vector2Double(Math.random()*0.4+0.3, Math.random()*0.4+0.3), getAgent().getId().getLocalName(), 0.1, 0.1, 0.8);  
		s = (Cleaner)getEnvironment().addSpaceObject((SpaceObject)s).get();
		bdifeature.setBeliefValue("self", s);
		//self.set(s);
		
		getEnvironment().observeObject((Cleaner)getSelf()).next(e ->
		{
			agent.getFeature(IExecutionFeature.class).scheduleStep(() ->
			{
				if(e instanceof VisionEvent)
				{
					Set<SpaceObject> seen = ((VisionEvent)e).getVision().getSeen();
					Set<SpaceObject> disap = ((VisionEvent)e).getVision().getDisappeared();
					
					for(SpaceObject obj: seen)
					{
						//System.out.println("New object seen: "+agent.getId().getLocalName() +", "+obj);
						
						if(obj instanceof Waste)
						{
							findAndUpdateOrAdd(obj, wastes);
						}
						else if(obj instanceof Wastebin)
						{
							findAndUpdateOrAdd(obj, wastebins);
						}
						else if(obj instanceof Chargingstation)
						{
							findAndUpdateOrAdd(obj, stations);
						}
						else if(obj instanceof Cleaner)
						{
							findAndUpdateOrAdd(obj, others);
						}
					}
					for(SpaceObject obj: disap)
					{
						if(obj instanceof Waste)
						{
							wastes.remove(obj);
							//System.out.println("Waste removed: "+agent.getId().getLocalName()+", "+obj);
						}
						else if(obj instanceof Wastebin)
						{
							wastebins.remove(obj);
						}
						else if(obj instanceof Chargingstation)
						{
							stations.remove(obj);
						}
						else if(obj instanceof Cleaner)
						{
							others.remove(obj);
						}
					}
				}
				else if(e instanceof SpaceObjectsEvent)
				{
					Set<SpaceObject> changed = ((SpaceObjectsEvent)e).getObjects();
					
					//System.out.println("update space objects: "+agent.getId()+" "+changed);
					
					for(SpaceObject obj: changed)
					{
						if(obj instanceof Waste)
						{
							findAndUpdateOrAdd(obj, wastes);
							//System.out.println("New target seen: "+agent.getId().getLocalName()+", "+obj);
						}
						else if(obj instanceof Wastebin)
						{
							findAndUpdateOrAdd(obj, wastebins);
						}
						else if(obj instanceof Chargingstation)
						{
							findAndUpdateOrAdd(obj, stations);
						}
						else if(obj instanceof Cleaner)
						{
							if(obj.equals(getSelf()))
							{
								getSelf().updateFrom(obj);
							}
							else
							{
								findAndUpdateOrAdd(obj, others);
							}
						}
					}
				}
			});
		});
		
		// Tell the sensor to update the belief sets
		/*actsense.manageWastesIn(wastes);
		actsense.manageWastebinsIn(wastebins);
		actsense.manageChargingstationsIn(stations);
		actsense.manageCleanersIn(others);*/
		
		// Open a window showing the agent's perceptions
		if(sensorgui)
			new SensorGui(agent.getComponentHandle()).setVisible(true);
		
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new PerformLookForWaste());
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new PerformPatrol());
		agent.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(new MaintainBatteryLoaded());
	}
	
	private void findAndUpdateOrAdd(SpaceObject obj, Collection<?> coll)
	{
		Collection<SpaceObject> sos = (Collection<SpaceObject>)coll;
		boolean found = false;
		    
	    for (SpaceObject item : sos) 
	    {
	        if (item.equals(obj)) 
	        {
	        	item.updateFrom(obj);
	            //spaceObjects.remove(item);
	            //spaceObjects.add(obj);
	            found = true;
	            break; 
	        }
	    }
	    
	    if (!found) 
	        sos.add(obj);
	}
	
	/**
	 *  Goal for keeping the battery loaded.
	 */
	@Goal(deliberation=@Deliberation(inhibits={PerformLookForWaste.class, AchieveCleanupWaste.class, PerformPatrol.class}))
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
			station = getNearestChargingStation();
			return station!=null;
		}
		
		protected Chargingstation getNearestChargingStation()
		{
			Chargingstation ret = null;
			for(Chargingstation cg: stations)
			{
				if(ret==null)
				{
					ret = cg;
				}
				else if(getSelf().getLocation().getDistance(cg.getLocation()).getAsDouble()
					<getSelf().getLocation().getDistance(ret.getLocation()).getAsDouble())
				{
					ret = cg;
				}
			}
			return ret;
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
		@GoalContextCondition
		private boolean context()
		{
			return daytime.get();
		}
	}
	
	
	
	@Goal(excludemode=ExcludeMode.Never)
	private class QueryWastebin
	{
		@GoalResult
		protected Wastebin wastebin;
		
		@GoalTargetCondition(beliefs="wastebins")
		public boolean checkTarget()
		{
			for(Wastebin wb: wastebins)
			{
				if(!wb.isFull())
				{
					if(wastebin==null)
					{
						wastebin = wb;
					}
					else if(getSelf().getLocation().getDistance(wb.getLocation()).getAsDouble()
						<getSelf().getLocation().getDistance(wastebin.getLocation()).getAsDouble())
					{
						wastebin = wb;
					}
				}
			}
			return wastebin!=null;
		}

		public Wastebin getWastebin() 
		{
			return wastebin;
		}

		public void setWastebin(Wastebin wastebin) 
		{
			this.wastebin = wastebin;
		}
	}
	
	//-------- cleanup waste --------
	
	//@Goal(recur=true, recurdelay=3000,deliberation=@Deliberation(inhibits=PerformPatrol.class, cardinalityone=true))
	@Goal(deliberation=@Deliberation(inhibits={PerformLookForWaste.class, AchieveCleanupWaste.class}), unique=true)
	private class AchieveCleanupWaste
	{
		private Waste	waste;
		
		@GoalCreationCondition(factadded="wastes")
		public AchieveCleanupWaste(Waste waste)
		{
			System.out.println("achieve cleanup: "+waste);
			this.waste	= waste;
		}
		
		// The goal is achieved, when the waste is gone.
		/*@GoalTargetCondition(beliefs={"self", "wastes"})
		boolean	isClean()
		{
			// Test if the waste is not believed to be in the environment
			return !wastes.contains(waste)
				// and also not the waste we just picked up.
				&& !waste.equals(self.getCarriedWaste());
		}*/
		
		// Goal should only be pursued when carrying no waste
		// or when goal is resumed after recharging and carried waste is of this goal.
		@GoalContextCondition
		boolean isPossible()
		{
			return self.getCarriedWaste()==null || self.getCarriedWaste().equals(waste);
		}
		
		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof AchieveCleanupWaste && ((AchieveCleanupWaste)obj).waste.equals(waste);
		}
		
		public Waste getWaste() 
		{
			return waste;
		}

		@Override
		public int hashCode()
		{
			return 31+waste.hashCode();
		}
		
		@GoalInhibit(AchieveCleanupWaste.class)
		private boolean	inhibitOther(AchieveCleanupWaste other)
		{
			try
			{
				return waste.equals(getSelf().getCarriedWaste()) || 
					(!other.waste.equals(getSelf().getCarriedWaste()) && other.waste.getLocation()!=null
						&& waste.getLocation().getDistance(getSelf().getLocation()).getAsDouble()
							< other.waste.getLocation().getDistance(getSelf().getLocation()).getAsDouble());
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}
	}
	
	@Plan(trigger=@Trigger(goals=AchieveCleanupWaste.class))
	private void cleanupWaste(IPlan plan, AchieveCleanupWaste goal)
	{
		System.out.println("Starting cleanupWaste() plan");
		Waste waste = goal.getWaste();
		
		// Move to waste and pick it up, if not yet done
		if(!waste.equals(self.getCarriedWaste()))
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
		QueryChargingStation sg = (QueryChargingStation)planapi.dispatchSubgoal((new QueryChargingStation())).get();
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
		System.out.println("Starting performPatrolPlan()");
		getEnvironment().move(getSelf(), new Vector2Double(0.1, 0.1));
		getEnvironment().move(getSelf(), new Vector2Double(0.1, 0.9));
		getEnvironment().move(getSelf(), new Vector2Double(0.9, 0.9));
		getEnvironment().move(getSelf(), new Vector2Double(0.9, 0.1));
		getEnvironment().move(getSelf(), new Vector2Double(0.1, 0.1));
	}

	/**
	 *  Declare a second plan for the PerformPatrol goal.
	 */
	@Plan(trigger=@Trigger(goals=PerformPatrol.class))
	private void performPatrolPlan2()
	{
		// Follow another path around the middle of the museum.
		System.out.println("Starting performPatrolPlan2()");
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3));
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.7));
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.7));
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.3));
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3));
	}
	
	/**
	 *  Declare a third plan for the PerformPatrol goal.
	 */
	@Plan(trigger=@Trigger(goals=PerformPatrol.class))
	private void performPatrolPlan3()
	{
		// Follow a zig-zag path in the museum.
		System.out.println("Starting performPatrolPlan3()");
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3));
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.7));
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.7));
		getEnvironment().move(getSelf(), new Vector2Double(0.7, 0.3));
		getEnvironment().move(getSelf(), new Vector2Double(0.3, 0.3));
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
		return new Future<>(self);
	}

	@ComponentMethod
	public IFuture<Boolean> isDaytime() 
	{
		return new Future<>(daytime.get());
	}	
	
	@ComponentMethod
	public IFuture<IVector2> getTarget() 
	{
		return getEnvironment().getTarget(self);
	}
}
