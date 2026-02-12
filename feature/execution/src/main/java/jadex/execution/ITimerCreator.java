package jadex.execution;

import jadex.execution.impl.ITimerContext;
import jadex.future.ITerminableFuture;

/**
 *  Interface for an element that can create timers (such as a component).
 */
public interface ITimerCreator
{
	 public ITerminableFuture<Void> createTimer(ITimerContext context, long timeout);
}

