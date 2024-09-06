package jadex.bt.impl;

import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;
import jadex.future.ITerminableFuture;

public interface ITimerCreator<T> 
{
	 public ITerminableFuture<Void> createTimer(Node<T> node, ExecutionContext<T> context, long timeout);
}
