package jadex.bt;

import java.util.function.BiFunction;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class Decorator<T> 
{
	protected BiFunction<Event, NodeState, IFuture<NodeState>> execute;
	
	public Decorator()
	{
	}

	public Decorator<T> setFuntion(BiFunction<Event, NodeState, IFuture<NodeState>> execute)
	{
		this.execute = execute;
		return this;
	}
	
	public Decorator<T> setFunction(BiFunction<Event, NodeState, NodeState> execute) 
	{
		this.execute = (event, state) -> new Future<>(execute.apply(event, state));
	    return this;
	}
	
	public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, T Context)
	{
		return execute.apply(event, state);
	}
}
