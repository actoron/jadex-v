package jadex.bt.impl;

import jadex.bt.state.ExecutionContext;
import jadex.future.ITerminableFuture;

public interface ITimerCreator<T> 
{
	 public ITerminableFuture<Void> createTimer(ExecutionContext<T> context, long timeout);
}
