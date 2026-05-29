package jadex.bdi.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IGoal;
import jadex.bdi.impl.goal.EasyDeliberationStrategy;
import jadex.bdi.impl.goal.RGoal;
import jadex.bdi.impl.plan.IPlanBody;
import jadex.bdi.impl.plan.RPlan;
import jadex.core.IChangeListener;
import jadex.core.impl.ILifecycle;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.ITerminableFuture;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.IValueFetcherCreator;
import jadex.injection.impl.InjectionFeature;

public class BDIAgentFeature implements IBDIAgentFeature, ILifecycle
{
	/** The component. */
	protected BDIAgent	self;
	
	/** The BDI model. */
	protected BDIModel	model;
	
	/** The currently running plans. */
	protected Map<IPlanBody, Set<RPlan>>	plans;
	
	/** The currently adopted goals. */
	protected Map<Class<?>, Set<RGoal>>	goals;
	
	/** The capabilities. */
	protected Map<List<Class<?>>, List<Object>>	capabilities;
	
	/** The deliberation strategy, if any (null if no goals in model). */
	protected IDeliberationStrategy delstr;
	
	/**
	 *  Create the feature.
	 */
	public BDIAgentFeature(BDIAgent self)
	{
		this.self	= self;
		this.model	= BDIModel.getModel(self.getPojo().getClass());
	}
	
	//-------- ILifecycle interface --------
	
	@Override
	public void init()
	{
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BDIStepListener(/*rulesystem*/));
	}
	
	@Override
	public void cleanup()
	{
		// plan abort moved to overridden BDIAgent.terminate()
		// to call abort() before onend() for plans
	}
	
	protected void	abortPlans()
	{
		// Abort all plans (terminates wait futures if any)
		// TODO: generic future handler to terminate any futures on component end
		if(plans!=null)
		{
			for(Set<RPlan> planset: plans.values())
			{
				if(!planset.isEmpty())
				{
					for(RPlan plan: planset.toArray(new RPlan[planset.size()]))
					{
						plan.abort();
						// call aborted explicitly, because any wakeup code from plan.abort()
						// is cancelled by execution feature with StepAborted
						plan.getBody().callAborted(plan);
					}
				}
			}
		}
	}
	
	//-------- IBDIAgentFeature interface --------

	@Override
	public <T> ITerminableFuture<T> dispatchTopLevelGoal(Supplier<T> goal)
	{
		@SuppressWarnings("unchecked")
		ITerminableFuture<T>	ret	= (ITerminableFuture<T>)dispatchTopLevelGoal((Object)goal);
		return ret;
	}
	
	@Override
	public ITerminableFuture<Void> dispatchTopLevelGoal(Object goal)
	{
		final RGoal rgoal = new RGoal(goal, null, self);
		
		// Adopt directly (no schedule step)
		// TODO: why step for subgoal and not for top-level!?
		rgoal.adopt();
		
		@SuppressWarnings("unchecked")
		ITerminableFuture<Void>	ret	= (ITerminableFuture<Void>) rgoal.getFinished();
		return ret;
	}
	
	@Override
	public void dropGoal(Object goal)
	{
		for(RGoal rgoal: doGetGoals(goal.getClass()))
		{
			if(goal.equals(rgoal.getPojo()))
			{
				rgoal.drop();
				break;
			}
		}
	}
	
	@Override
	public <T> Set<T> getGoals(Class<T> clazz)
	{
		Set<T> ret	= null;
		Set<RGoal>	goals	= doGetGoals(clazz);
		if(goals!=null)
		{
			ret	= new LinkedHashSet<>();
			for(RGoal goal: goals)
			{
				@SuppressWarnings("unchecked")
				T	pojo	= (T)goal.getPojo();
				ret.add(pojo);
			}
		}
		return ret==null ? Collections.emptySet() : ret;
	}
	
	@Override
	public Collection<IGoal> getGoals()
	{
		List<IGoal>	ret	= new ArrayList<>();
		if(goals!=null)
		{
			for(Class<?> type: goals.keySet())
			{
				ret.addAll(goals.get(type));
			}
		}
		return ret;
	}
	
	//-------- ICapability interface --------
	
	@Override
	public <T> void addChangeListener(String name, IChangeListener listener)
	{
		self.getFeature(IInjectionFeature.class).addListener(name, listener);
	}
	
	@Override
	public <T> void removeChangeListener(String name, IChangeListener listener)
	{
		self.getFeature(IInjectionFeature.class).removeListener(name, listener);
	}
	
	//-------- internal methods --------
	
	/**
	 *  Get the model.
	 */
	public BDIModel getModel()
	{
		return model;
	}
	
	/**
	 *  Add a plan.
	 *  Called when the plan is first executed.
	 */
	public void addPlan(RPlan rplan, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers)
	{		
		if(plans==null)
		{
			plans	= new LinkedHashMap<>();
		}
		Set<RPlan>	planset	= plans.get(rplan.getBody());
		if(planset==null)
		{
			planset	= new LinkedHashSet<>();
			plans.put(rplan.getBody(), planset);
		}
		planset.add(rplan);
		
		if(rplan.getPojo()!=null)
		{
			((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(rplan.getAllPojos(), rplan);
		}
	}
	
	/**
	 *  Get the plans, if any.
	 */
	public Map<IPlanBody, Set<RPlan>>	getPlans()
	{
		return plans;
	}
	
	/**
	 *  Remove a plan after it has finished.
	 */
	public void removePlan(RPlan rplan)
	{
		Set<RPlan>	planset	= plans.get(rplan.getBody());
		if(planset!=null)
		{
			planset.remove(rplan);
			// Do not remove set to avoid continuous recreation if a plan gets executed repeatedly.
		}

		if(rplan.getPojo()!=null)
		{
			((InjectionFeature)self.getFeature(IInjectionFeature.class)).removeExtraObject(rplan.getAllPojos(), rplan);
		}
	}

	
	/**
	 *  Add a Goal.
	 *  Called when the goal is adopted.
	 */
	public void addGoal(RGoal rgoal)
	{		
		if(goals==null)
		{
			goals	= new LinkedHashMap<>();
		}
		Set<RGoal>	typedgoals	= goals.get(rgoal.getPojo().getClass()); 
		if(typedgoals==null)
		{
			typedgoals	= new LinkedHashSet<>();
			goals.put(rgoal.getPojo().getClass(), typedgoals);
		}
		typedgoals.add(rgoal);
		
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(rgoal.getAllPojos(), rgoal);
	}
	
	/**
	 *  Get the goals of the given type, if any.
	 */
	public Set<RGoal>	doGetGoals(Class<?> goaltype)
	{
		return goals!=null ? goals.get(goaltype) : null;
	}
	
	/**
	 *  Remove a Goal after it is dropped.
	 */
	public void removeGoal(RGoal rgoal)
	{
		goals.get(rgoal.getPojo().getClass()).remove(rgoal);
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).removeExtraObject(rgoal.getAllPojos(), rgoal);
	}	
	
	/**
	 *  Add a capability.
	 */
	protected void	addCapability(List<Object> pojos)
	{
		// Add to known sub-objects
		List<Class<?>>	pojoclazzes	= new ArrayList<>();
		for(Object pojo: pojos)
		{
			pojoclazzes.add(pojo.getClass());
		}
		if(capabilities==null)
		{
			capabilities	= new LinkedHashMap<>();
		}
		// TODO: support multiple instances of same capability?
		capabilities.put(pojoclazzes, pojos);
		
		((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(pojos, null);
	}
	
	public List<Object> getCapability(List<Class<?>> parentclazzes)
	{
		return parentclazzes.size()==1 ? Collections.singletonList(self.getPojo()) : capabilities.get(parentclazzes);
	}

	public IDeliberationStrategy getDeliberationStrategy()
	{
		if(delstr==null)
		{
			this.delstr = new EasyDeliberationStrategy();
			delstr.init();
		}
		return delstr;
	}
}
