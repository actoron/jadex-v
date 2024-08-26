package jadex.bt;

import jadex.future.ITerminableFuture;

public interface ITimerCreator<T> 
{
	 public ITerminableFuture<Void> createTimer(Node<T> node, ExecutionContext<T> context, long timeout);
}
