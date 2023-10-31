package jadex.bdiv3.features.impl;

import jadex.bdiv3.runtime.impl.RPlan;
import jadex.feature.execution.impl.IStepListener;
import jadex.future.IFuture;

/**
 *  BDI step listener adds rule engine behavior to the component execution.
 */
public class BDIStepListener implements IStepListener
{
	@Override
	public void	afterStep()
	{
		// Evaluate conditions in addition to executing steps.
		IInternalBDIAgentFeature bdif = IInternalBDIAgentFeature.get();
//		boolean inited = ((IInternalBDILifecycleFeature)getComponent().getFeature(ILifecycleComponentFeature.class)).isInited();
		// Process all events until quiescence
		while(bdif.getRuleSystem()!=null && bdif.getRuleSystem().isEventAvailable())
		{
//			System.out.println("executeCycle.PAE start");
			IFuture<Void> fut = bdif.getRuleSystem().processAllEvents();
			if(!fut.isDone())
				System.err.println("No async actions allowed.");
		}
	}

	@Override
	public void beforeBlock()
	{
		RPlan rplan = RPlan.RPLANS.get();
		if(rplan!=null)
		{
			rplan.beforeBlock();
		}
	}
	
	@Override
	public void afterBlock()
	{
//		if(getComponent().toString().indexOf("Leaker")!=-1)
//		{
//			System.out.println("afterBlock "+Thread.currentThread());
//		}
		RPlan rplan = RPlan.RPLANS.get();
		if(rplan!=null)
		{
//			if(getComponent().toString().indexOf("Leaker")!=-1)
//			{
//				System.out.println("afterBlock 1"+Thread.currentThread());
//			}
			rplan.afterBlock();
		}
	}
	
//	/**
//	 *  Check if the execution kernel supports semantic steps.
//	 *  @return True, if semantic steps are supported and the kernel
//	 *  uses events to setSemanticEffect on current steps.
//	 */
//	public boolean isSemanticStepped()
//	{
//		return true;
//	}
	
//	/**
//	 *  Execute a component step.
//	 */
//	public <T>	IFuture<T> scheduleStep(IComponentStep<T> step)
//	{
//		return scheduleImmediate(step);
//	}
}
