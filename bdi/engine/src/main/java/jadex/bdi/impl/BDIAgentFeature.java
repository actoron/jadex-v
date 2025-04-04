package jadex.bdi.impl;

import jadex.bdi.IBDIAgentFeature;
import jadex.execution.IExecutionFeature;
import jadex.execution.impl.IInternalExecutionFeature;
import jadex.execution.impl.ILifecycle;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.rules.eca.RuleSystem;

public class BDIAgentFeature implements IBDIAgentFeature, ILifecycle
{
	/** The component. */
	protected BDIAgent	self;
	
	/** The rule system. */
	protected RuleSystem	rulesystem;
	
	/**
	 *  Create the feature.
	 */
	public BDIAgentFeature(BDIAgent self)
	{
		this.self	= self;
		this.rulesystem	= new RuleSystem(self);
	}
	
	//-------- ILifecycle interface --------
	
	@Override
	public void onStart()
	{
		((IInternalExecutionFeature)self.getFeature(IExecutionFeature.class)).addStepListener(new BDIStepListener(rulesystem));
	}
	
	@Override
	public void onEnd()
	{
	}
	
	//-------- IBDIAgentFeature interface --------

	/**
	 *  Dispatch a pojo goal wait for its result.
	 *  @param goal The pojo goal.
	 *  @return The goal result.
	 */
	public <T, E> IFuture<E> dispatchTopLevelGoal(final T goal)
	{
		final Future<E> ret = new Future<E>();
		
//		final MGoal mgoal = ((MCapability)capa.getModelElement()).getGoal(goal.getClass().getName());
//		if(mgoal==null)
//			throw new RuntimeException("Unknown goal type: "+goal);
//		final RGoal rgoal = new RGoal(mgoal, goal, null, null, null);
//		rgoal.addListener(new ExceptionDelegationResultListener<Void, E>(ret)
//		{
//			public void customResultAvailable(Void result)
//			{
//				Object res = RGoal.getGoalResult(rgoal, self.getPojo().getClass().getClassLoader());
//				ret.setResult((E)res);
//			}
//		});
//
////		System.out.println("adopt goal");
//		RGoal.adoptGoal(rgoal);
		
		return ret;
	}
	
	//-------- internal methods --------
	
	/**
	 *  Get the rule system.
	 */
	public RuleSystem	getRuleSystem()
	{
		return this.rulesystem;
	}	
}
