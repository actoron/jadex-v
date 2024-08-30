package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.common.ITriFunction;
import jadex.future.Future;
import jadex.future.IFuture;

public class ConditionalDecorator<T> extends Decorator<T>
{
	protected ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> execute;
	
	public ConditionalDecorator()
	{
	}

	public ConditionalDecorator<T> setFuntion(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> execute)
	{
		this.execute = execute;
		return this;
	}
	
	public ConditionalDecorator<T> setFunction(ITriFunction<Event, NodeState, ExecutionContext<T>, NodeState> execute) 
	{
		this.execute = (event, state, context) -> new Future<>(execute.apply(event, state, context));
	    return this;
	}
	
	@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		return execute.apply(event, state, context);
	}
}
