package jadex.bdi.impl;

import jadex.bdi.impl.plan.RPlan;
import jadex.execution.impl.IStepListener;
import jadex.future.Future;

/**
 *  BDI step listener adds rule engine behavior to the component execution.
 */
public class BDIStepListener implements IStepListener
{
//	/** The rule system. */
//	protected RuleSystem	rulesystem;
//	
//	/**
//	 *  Create the feature.
//	 */
//	public BDIStepListener(RuleSystem rulesystem)
//	{
//		this.rulesystem	= rulesystem;
//	}
	
//	@Override
//	public void	afterStep()
//	{
//		// Evaluate conditions in addition to executing steps.
//		// Process all events until quiescence
//		while(rulesystem.isEventAvailable())
//		{
////			System.out.println("executeCycle.PAE start");
//			IFuture<Void> fut = rulesystem.processAllEvents();
//			if(!fut.isDone())
//				System.err.println("No async actions allowed.");
//		}
//	}

	@Override
	public <T> void beforeBlock(Future<T> fut)
	{
		RPlan rplan = RPlan.RPLANS.get();
		if(rplan!=null)
		{
			rplan.beforeBlock(fut);
		}
	}
	
	@Override
	public void afterBlock()
	{
		RPlan rplan = RPlan.RPLANS.get();
		if(rplan!=null)
		{
			rplan.afterBlock();
		}
	}
}
