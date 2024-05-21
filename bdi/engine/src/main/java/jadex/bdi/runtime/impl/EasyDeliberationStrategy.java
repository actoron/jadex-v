package jadex.bdi.runtime.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jadex.bdi.model.MDeliberation;
import jadex.bdi.model.MGoal;
import jadex.bdi.runtime.IDeliberationStrategy;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoal.GoalLifecycleState;
import jadex.bdi.runtime.IGoal.GoalProcessingState;
import jadex.common.MethodInfo;
import jadex.common.SAccess;
import jadex.common.UnparsedExpression;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.model.IModelFeature;

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
//		System.out.println(inhibitions.size()+" "+inhibitions);
		
		Collection<RGoal>	others	= getCapability().getGoals();
		for(RGoal other: others)
		{
//			if(other.getLifecycleState().equals(RGoal.GOALLIFECYCLESTATE_ACTIVE) 
//				&& other.getProcessingState().equals(RGoal.GOALPROCESSINGSTATE_INPROCESS)
			
//			if(goal!=other && other.getModelElement().equals(goal.getModelElement()) && goal.getModelElement().getName().toLowerCase().indexOf("achievecleanup")!=-1)
//				System.out.println("achievecleanup");
			
			if(!isInhibitedBy(other, goal) && inhibits(other, goal))
			{
//				if(goal.getModelElement().getName().indexOf("achievecleanup")!=-1)
//					System.out.println("inhibit");
				addInhibitor(goal, other);
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
//		if(goal.getId().indexOf("analyse")!=-1)
//			System.out.println("addinh: "+goal);
		MDeliberation delib = goal.getMGoal().getDeliberation();
		if(delib!=null)
		{
			Set<MGoal> inhs = delib.getInhibitions(getCapability().getMCapability());
			if(inhs!=null)
			{
				for(MGoal inh: inhs)
				{
					Collection<RGoal> goals = getCapability().getGoals(inh);
					for(RGoal other: goals)
					{
//						if(!other.isInhibitedBy(goal) && goal.inhibits(other, getInternalAccess()))
						if(!isInhibitedBy(goal, other) && inhibits(goal, other))
						{
//							if(other.getModelElement().getName().indexOf("achievecleanup")!=-1)
//								System.out.println("inh achieve");
							addInhibitor(other, goal);
						}
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
		MDeliberation delib = goal.getMGoal().getDeliberation();
		if(delib!=null)
		{
			Set<MGoal> inhs = delib.getInhibitions(getCapability().getMCapability());
			if(inhs!=null)
			{
				for(MGoal inh: inhs)
				{
//					if(goal.getId().indexOf("AchieveCleanup")!=-1)
//						System.out.println("reminh: "+goal);
					Collection<RGoal> goals = getCapability().getGoals(inh);
					for(RGoal other: goals)
					{
						if(goal.equals(other))
							continue;
						
						if(isInhibitedBy(other, goal))
							removeInhibitor(other, goal);
					}
				}
			}
			
			// Remove inhibitor from goals of same type if cardinality is used
			if(delib.isCardinalityOne())
			{
				Collection<RGoal> goals = getCapability().getGoals(goal.getMGoal());
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
		}
	
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
//			getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALINHIBITED, goal.getMGoal().getName()}), this));
		}
		
//		if(inhibitor.getId().indexOf("AchieveCleanup")!=-1)
//			System.out.println("add inhibit: "+getId()+" "+inhibitor.getId()+" "+inhibitors);
	}
	
	/**
	 *  Inhibit a goal by making it an option.
	 */
	protected void inhibitGoal(RGoal goal)
	{
		MGoal mgoal = (MGoal)goal.getModelElement();
		if(mgoal!=null && mgoal.getDeliberation()!=null && mgoal.getDeliberation().isDropOnInhibit())
		{
			//System.out.println("drop goal: "+goal);
			goal.drop();
		}
		else
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
//		System.out.println("rem inhibit: "+getId()+" "+inhibitor.getId()+" "+inhibitors);
		
//		if(inhibitor.getId().indexOf("AchieveCleanup")!=-1)
//			System.out.println("kokoko: "+inhibitor);
		
		Set<RGoal> inhibitors = getInhibitions(goal, false);
		
		if(inhibitors!=null)
		{
			if(inhibitors.remove(inhibitor) && inhibitors.size()==0)
			{
				inhibitions.remove(goal);
//				System.err.println("goal not inhibited: "+goal);
//				BDIAgentInterpreter ip = (BDIAgentInterpreter)((BDIAgent)ia).getInterpreter();
				reactivateGoal(goal);
//				getRuleSystem().addEvent(new Event(new EventType(new String[]{ChangeEvent.GOALNOTINHIBITED, goal.getMGoal().getName()}), this));
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
			goal.setLifecycleState(RGoal.GoalLifecycleState.ACTIVE);
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
			MDeliberation delib = goal.getMGoal().getDeliberation();
			if(delib!=null)
			{
//				if(delib.isCardinalityOne() && other.getMGoal().equals(goal.getMGoal()))
//				{
//					ret = true;
//				}
//				else
//				{
//					Set<MGoal> minh = delib.getInhibitions();
					Set<MGoal> minh = delib.getInhibitions(goal.getMCapability());
					MGoal mother = other.getMGoal();
					if(minh!=null && minh.contains(mother))
					{
						ret = true;
						
						// check if instance relation
						Map<String, MethodInfo> dms = delib.getInhibitionMethods();
						if(dms!=null)
						{
							MethodInfo mi = dms.get(mother.getName());
							if(mi!=null)
							{
								Method dm = mi.getMethod(IInternalBDIAgentFeature.get().getClassLoader());
								try
								{
									SAccess.setAccessible(dm, true);
									ret = ((Boolean)dm.invoke(goal.getPojo(), new Object[]{other.getPojo()})).booleanValue();
								}
								catch(Exception e)
								{
									Throwable	t	= e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
									System.err.println("Exception in inhibits expression: "+t);
								}
							}
						}
						
						// xml inhibition expressions
						Map<String, UnparsedExpression> uexps = delib.getInhibitionExpressions();
						if(uexps!=null)
						{
							UnparsedExpression uexp = uexps.get(mother.getName());
							if(uexp!=null && uexp.getValue()!=null && uexp.getValue().length()>0)
							{
								SimpleValueFetcher fet = new SimpleValueFetcher(IExecutionFeature.get().getComponent().getFeature(IModelFeature.class).getFetcher());
								fet.setValue(goal.getFetcherName(), goal);
								fet.setValue("$ref", other);
								
								try
								{
									ret = ((Boolean)SJavaParser.parseExpression(uexp, IInternalBDIAgentFeature.get().getBDIModel().getModelInfo().getAllImports(), IInternalBDIAgentFeature.get().getClassLoader()).getValue(fet)).booleanValue();
//									System.out.println("exp val: "+ret+" "+uexp);
								}
								catch(Exception e)
								{
									Throwable	t	= e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException() : e;
									System.err.println("Exception in inhibits expression: "+t);
								}
							}
						}
//					}
				}
			}
		}
		
		return ret;
	}
	
//	/**
//	 *  Get the inhibitors.
//	 *  @return The inhibitors.
//	 */
//	protected Set<RGoal> getInhibitors(RGoal goal)
//	{
//		return getInhibitions(goal, false);
//	}
//	
//	/**
//	 *  Get the rule system.
//	 */
//	protected RuleSystem getRuleSystem()
//	{
//		return agent.getComponentFeature(IInternalBDIAgentFeature.class).getRuleSystem();
//	}
//	
	/**
	 *  Get the capability.
	 */
	protected RCapability getCapability()
	{
		return IInternalBDIAgentFeature.get().getCapability();
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
