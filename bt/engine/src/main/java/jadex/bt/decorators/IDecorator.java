package jadex.bt.decorators;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.future.IFuture;

public interface IDecorator<T> 
{
	public String getType();
	
	public IFuture<NodeState> internalExecute(Event event, NodeState result, ExecutionContext<T> context);
}
