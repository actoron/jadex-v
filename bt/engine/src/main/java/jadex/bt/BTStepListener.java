package jadex.bt;

import jadex.bt.impl.BTAgentFeature;
import jadex.execution.impl.IStepListener;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  BT step listener adds rule engine behavior to the component execution.
 */
public class BTStepListener implements IStepListener
{
	@Override
	public void	afterStep()
	{
		// Evaluate conditions in addition to executing steps.
		BTAgentFeature btf = BTAgentFeature.get();
		
		//System.out.println("afterstep: "+btf.getRuleSystem().isEventAvailable());
		while(btf.getRuleSystem()!=null && btf.getRuleSystem().isEventAvailable())
		{
			System.out.println("executeCycle.PAE start");
			IFuture<Void> fut = btf.getRuleSystem().processAllEvents();
			if(!fut.isDone())
				System.err.println("No async actions allowed.");
		}
	}

	@Override
	public <T> void beforeBlock(Future<T> fut)
	{
	}
	
	@Override
	public void afterBlock()
	{
	}
}
