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
			Set<RGoal>	goals	= getBDIFeature().getGoals(goaltype);
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
		Deliberation delib = getBDIFeature().getModel().getGoalInfo(goal.getPojo().getClass()).annotation().deliberation();
		Class<?>[] inhs = delib.inhibits();
		for(Class<?> inh: inhs)
		{
			Collection<RGoal> goals = getBDIFeature().getGoals(inh);
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
		return IFuture.DONE;
	}
	
	/**
	 *  Called when a goal is not active any longer (suspended or option).
	 *  @param goal The goal.
	 */
	public IFuture<Void> goalIsNotActive(RGoal goal)
	{
		// Remove inhibitions of this goal 
		Deliberation delib = getBDIFeature().getModel().getGoalInfo(goal.getPojo().getClass()).annotation().deliberation();
		Class<?>[] inhs = delib.inhibits();
		for(Class<?> inh: inhs)
		{
			Collection<RGoal> goals = getBDIFeature().getGoals(inh);
			if(goals!=null)
			{
				for(RGoal other: goals)
				{
					if(goal.equals(other))
						continue;
					
					if(isInhibitedBy(other, goal))
						removeInhibitor(other, goal);
				}
			}
		}
			
//		// Remove inhibitor from goals of same type if cardinality is used
//		if(delib.isCardinalityOne())
//		{
//			Collection<RGoal> goals = getCapability().getGoals(goal.getMGoal());
//			if(goals!=null)
//			{
//				for(RGoal other: goals)
//				{
//					if(goal.equals(other))
//						continue;
//					
//					if(isInhibitedBy(other, goal))
//						removeInhibitor(other, goal);
//				}
//			}
//		}
	
		return IFuture.DONE;
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
		
		// todo: cardinality
		boolean ret = false;
		
		if(goal.getLifecycleState().equals(GoalLifecycleState.ACTIVE) && goal.getProcessingState().equals(GoalProcessingState.INPROCESS))
		{
			Deliberation delib = getBDIFeature().getModel().getGoalInfo(goal.getPojo().getClass()).annotation().deliberation();
			Class<?>[] inhs = delib.inhibits();
			if(Arrays.asList(inhs).contains(other.getPojo().getClass()))
			{
				ret = true;
				
//				// check if instance relation
//				Map<String, MethodInfo> dms = delib.getInhibitionMethods();
//				if(dms!=null)
//				{
//					MethodInfo mi = dms.get(mother.getName());
//					if(mi!=null)
//					{
//						Method dm = mi.getMethod(IInternalBDIAgentFeature.get().getClassLoader());
//						try
//						{
//							SAccess.setAccessible(dm, true);
//							ret = ((Boolean)dm.invoke(goal.getPojo(), new Object[]{other.getPojo()})).booleanValue();
//						}
//						catch(Exception e)
//						{
//							Throwable	t	= e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
//							System.err.println("Exception in inhibits expression: "+t);
//						}
//					}
//				}
			}
		}
		
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
