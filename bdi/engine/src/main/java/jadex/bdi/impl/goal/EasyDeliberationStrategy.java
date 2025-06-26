package jadex.bdi.impl.goal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IGoal;
import jadex.bdi.IGoal.GoalLifecycleState;
import jadex.bdi.IGoal.GoalProcessingState;
import jadex.bdi.annotation.Deliberation;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.IDeliberationStrategy;
import jadex.core.IComponentManager;
import jadex.future.IFuture;
import jadex.injection.impl.IInjectionHandle;

/**
 *  The easy deliberation strategy.
 */
public class EasyDeliberationStrategy implements IDeliberationStrategy
{
	/** The set of inhibitors. */
	protected Map<RGoal, Set<RGoal>> inhibitions;
	
	/**
	 *  Init the strategy.
	 */
	public void init()
	{
		this.inhibitions = new HashMap<RGoal, Set<RGoal>>();
	}
	
	/**
	 *  Called when a goal has been adopted.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsAdopted(RGoal goal)
	{
		for(Class<?> goaltype: getBDIFeature().getModel().getGoaltypes())
		{
			Set<RGoal>	goals	= getBDIFeature().doGetGoals(goaltype);
			if(goals!=null)
			{
				for(RGoal other: goals)
				{
					if(!isInhibitedBy(other, goal) && inhibits(other, goal))
					{
						addInhibitor(goal, other);
					}
				}
			}
		}
		
		return IFuture.DONE;
	}
	
	/**
	 *  Called when a goal has been dropped.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsDropped(RGoal goal)
	{
		// Remove the goal itself
		inhibitions.remove(goal);

		// Remove the goal from all other inhibition goal sets
		for(Set<RGoal> inh: inhibitions.values())
		{
			inh.remove(goal);
		}
		
		return IFuture.DONE;
	}
	
	/**
	 *  Called when a goal becomes an option.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsOption(RGoal goal)
	{
		if(!isInhibited(goal))
			reactivateGoal(goal);
		return IFuture.DONE;
	}
	
	/**
	 *  Called when a goal becomes active.
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsActive(RGoal goal)
	{
		Map<Class<?>, IInjectionHandle>	instanceinhibs	= goal.getMGoal().instanceinhibs();
		
		Deliberation delib = goal.getMGoal().annotation().deliberation();
		Class<?>[] inhs = delib.inhibits();
		for(Class<?> inh: inhs)
		{
			// Do only non-instance inhibits here (others are done below).
			if(instanceinhibs==null || !instanceinhibs.containsKey(inh))
			{
				addInhibitors(goal, inh);
			}
		}
		
		if(delib.cardinalityone())
		{
			// Do only non-instance inhibit here (others are done below).
			if(instanceinhibs==null || !instanceinhibs.containsKey(goal.getPojo().getClass()))
			{
				addInhibitors(goal, goal.getPojo().getClass());
			}
		}
		
		if(instanceinhibs!=null)
		{
			for(Class<?> inh: instanceinhibs.keySet())
			{
				addInhibitors(goal, inh);
			}
		}
		
		return IFuture.DONE;
	}
	
	/**
	 *  Add inhibitors of the given goal type to the goal.
	 */
	protected void addInhibitors(RGoal goal, Class<?> inh)
	{
		Collection<RGoal> goals = getBDIFeature().doGetGoals(inh);
//		System.out.println("Add inhibitors: "+goal+", "+goals);
		if(goals!=null)
		{
			for(RGoal other: goals)
			{
				if(!isInhibitedBy(goal, other) && inhibits(goal, other))
				{
					addInhibitor(other, goal);
				}
			}
		}
	}
	
	/**
	 *  Called when a goal is not active any longer (suspended or option).
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsNotActive(RGoal goal)
	{
		Map<Class<?>, IInjectionHandle>	instanceinhibs	= goal.getMGoal().instanceinhibs();
		
		// Remove inhibitions of this goal 
		Deliberation delib = goal.getMGoal().annotation().deliberation();
		Class<?>[] inhs = delib.inhibits();
		for(Class<?> inh: inhs)
		{
			// Do only non-instance inhibits here (others are done below).
			if(instanceinhibs==null || !instanceinhibs.containsKey(inh))
			{
				removeInhibitors(goal, inh);
			}
		}
		
		if(instanceinhibs!=null)
		{
			for(Class<?> inh: instanceinhibs.keySet())
			{
				removeInhibitors(goal, inh);
			}
		}
		
		// Remove inhibitor from goals of same type if cardinality is used
		if(delib.cardinalityone())
		{
			removeInhibitors(goal, goal.getPojo().getClass());
		}
	
		return IFuture.DONE;
	}
	
	/**
	 *  Remove inhibitor from other goals of given type.
	 */
	protected void removeInhibitors(RGoal goal, Class<?> inh)
	{
		Collection<RGoal> goals = getBDIFeature().doGetGoals(inh);
		if(goals!=null)
		{
			for(RGoal other: goals)
			{
				if(goal.equals(other))
					continue;
				
//				if(isInhibitedBy(other, goal))
				removeInhibitor(other, goal);
			}
		}
	}
	
	/**
	 *  Add an inhibitor to a goal.
	 */
	public void addInhibitor(RGoal goal, RGoal inhibitor)
	{		
		Set<RGoal> inhibitors = getInhibitions(goal, true);

		if(inhibitors.add(inhibitor) && inhibitors.size()==1) // inhibit on first inhibitor
		{
			inhibitGoal(goal);
		}
	}
	
	/**
	 *  Inhibit a goal by making it an option.
	 */
	protected void inhibitGoal(RGoal goal)
	{
//		MGoal mgoal = (MGoal)goal.getModelElement();
//		if(mgoal!=null && mgoal.getDeliberation()!=null && mgoal.getDeliberation().isDropOnInhibit())
//		{
//			goal.drop();
//		}
//		else
		{
			if(IGoal.GoalLifecycleState.ACTIVE.equals(goal.getLifecycleState()))
				goal.setLifecycleState(RGoal.GoalLifecycleState.OPTION);
		}
	}
	
	/**
	 *  Remove an inhibitor from a goal.
	 */
	protected void removeInhibitor(RGoal goal, RGoal inhibitor)
	{
		Set<RGoal> inhibitors = getInhibitions(goal, false);
		
		if(inhibitors!=null)
		{
			if(inhibitors.remove(inhibitor) && inhibitors.size()==0)
			{
				inhibitions.remove(goal);
				reactivateGoal(goal);
			}
		}
	}
	
	/**
	 *  (Re)activate a goal.
	 */
	protected void reactivateGoal(RGoal goal)
	{
		// Only reactivate when not dropping/dropped
		if(!GoalLifecycleState.DROPPED.equals(goal.getLifecycleState()) && !GoalLifecycleState.DROPPING.equals(goal.getLifecycleState()))
		{
			goal.setLifecycleState(RGoal.GoalLifecycleState.ACTIVE);
		}
	}
	
	/**
	 *  Test if goal is inhibited.
	 */
	protected boolean isInhibited(RGoal goal)
	{
		Set<RGoal> inhibitors = getInhibitions(goal, false);
		return inhibitors!=null && !inhibitors.isEmpty();
	}
	
	/**
	 * Test if goal is inhibited by another goal.
	 */
	protected boolean isInhibitedBy(RGoal goal, RGoal other)
	{
		Set<RGoal> inhibitors = getInhibitions(goal, false);
		return !goal.isFinished() && inhibitors!=null && inhibitors.contains(other);
	}
	
	/**
	 *  Test if this goal inhibits the other.
	 */
	protected boolean inhibits(RGoal goal, RGoal other)
	{
		if(goal.equals(other))
			return false;
		
		boolean ret = false;
		
		if(goal.getLifecycleState().equals(GoalLifecycleState.ACTIVE) && goal.getProcessingState().equals(GoalProcessingState.INPROCESS))
		{
			MGoal	mgoal	= goal.getMGoal();
			
			// check if instance relation
			Boolean	instanceinhibit	= null;
			Map<Class<?>, IInjectionHandle>	instanceinhibs	= mgoal.instanceinhibs();
			if(instanceinhibs!=null)
			{
				IInjectionHandle	inhib	= instanceinhibs.get(other.getPojo().getClass());
				if(inhib!=null)
				{
					instanceinhibit	= (Boolean)inhib.apply(goal.getComponent(), goal.getAllPojos(), other, null);
				}				
			}
			
			ret	= instanceinhibit!=null && instanceinhibit;
			
			if(!ret)
			{
				Deliberation delib = mgoal.annotation().deliberation();
				
				// Check if cardinality.
				if(delib.cardinalityone() && goal.getPojo().getClass().equals(other.getPojo().getClass()))
				{
					// Only inhibit, when not inverse instance relation  
					Boolean	reverseinhibit	= null;
					if(instanceinhibs!=null)
					{
						IInjectionHandle	inhib	= instanceinhibs.get(goal.getPojo().getClass());
						if(inhib!=null)
						{
							reverseinhibit	= (Boolean)inhib.apply(other.getComponent(), other.getAllPojos(), goal, null);
						}
					}
					
					ret	= reverseinhibit==null || !reverseinhibit;
				}
			
				if(!ret)
				{
					// Check if type inhibit.
					Class<?>[] inhs = delib.inhibits();
					if(Arrays.asList(inhs).contains(other.getPojo().getClass()))
					{
						// Inhibit other when instance inhibit is not present or explicitly true
						ret = instanceinhibit==null || instanceinhibit;
					}
					else
					{
						ret	= instanceinhibit!=null && instanceinhibit;
					}
					
				}
			}
		}
//		System.out.println("Inhibits: "+goal+", "+other+" = "+ret);
		
		return ret;
	}
	
	/**
	 *  Get the capability.
	 */
	protected BDIAgentFeature	getBDIFeature()
	{
		return (BDIAgentFeature) IComponentManager.get().getCurrentComponent().getFeature(IBDIAgentFeature.class);
	}
	
	/**
	 *  Get or create the inhibition set.
	 */
	protected Set<RGoal> getInhibitions(RGoal goal, boolean create)
	{
		Set<RGoal> inhibitors = inhibitions.get(goal);
		if(create)
		{
			if(inhibitors==null)
			{
				inhibitors = new HashSet<RGoal>();
				inhibitions.put(goal, inhibitors);
			}
		}
		return inhibitors;
	}
}
