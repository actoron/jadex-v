package jadex.execution.impl;

import jadex.core.IComponentHandle;
import jadex.core.ITerminableStep;
import jadex.execution.IExecutionFeature;
import jadex.execution.ITimerCreator;
import jadex.future.ITerminableFuture;

public class ComponentTimerCreator implements ITimerCreator
{
	@Override
	public ITerminableFuture<Void> createTimer(ITimerContext context, long timeout)
	{
		IComponentHandle access = context.getResource(IComponentHandle.class);
		return access.scheduleAsyncStep((ITerminableStep<Void>)comp -> comp.getFeature(IExecutionFeature.class).waitForDelay(timeout));
	}
}

