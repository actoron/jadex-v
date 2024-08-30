package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.IFuture;

public interface IDecorator<T> 
{
	public IFuture<NodeState> internalExecute(Event event, NodeState result, ExecutionContext<T> context);
}
