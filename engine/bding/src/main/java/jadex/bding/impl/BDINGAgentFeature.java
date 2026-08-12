package jadex.bding.impl;

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

import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.impl.BDINGAgent;
import jadex.bding.Goal;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIntention;
import jadex.core.IChangeListener;
import jadex.core.impl.ILifecycle;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.future.ITerminableFuture;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.IValueFetcherCreator;
import jadex.injection.impl.InjectionFeature;

import jadex.bding.Plan;
import jadex.future.IFuture;
import jadex.future.Future;


public class BDINGAgentFeature implements IBDINGAgentFeature, ILifecycle
{
	/** The component. */
	protected BDINGAgent self;

	protected IReasoner reasoner;
	
	/**
	 *  Create the feature.
	 */
	public BDINGAgentFeature(BDINGAgent self)
	{
		this.self	= self;
		this.reasoner = new IReasoner()
		{
			@Override
			public IFuture<Set<Intention>> generateIntentions(RGoal goal)
			{
				return new Future<Set<Intention>>(Collections.emptySet());
			}

			@Override
			public IFuture<Intention> selectIntention(RGoal goal, Set<Intention> intentions)
			{
				return new Future<Intention>((Intention)null);
			}

			@Override
			public IFuture<Plan> generatePlan(RGoal goal)
			{
				return new Future<Plan>((Plan)null);
			}

			public IFuture<Boolean> equals(String str1, String str2)
			{
				return new Future<Boolean>(str1.equals(str2));
			}

		};
	}

	public ITerminableFuture<Void> dispatchTopLevelGoal(Goal goal)
	{
		final RGoal rgoal = new RGoal(goal, null, self);
		
		rgoal.adopt();
		
		//@SuppressWarnings("unchecked")
		ITerminableFuture<Void>	ret	= (ITerminableFuture<Void>) rgoal.getFinished();
		return ret;
	}
	
	public IReasoner getReasoner()
	{
		return reasoner;
	}

	//-------- ILifecycle interface --------
	
	@Override
	public void init()
	{
	}
	
	@Override
	public void cleanup()
	{
		// plan abort moved to overridden BDIAgent.terminate()
		// to call abort() before onend() for plans
	}
}