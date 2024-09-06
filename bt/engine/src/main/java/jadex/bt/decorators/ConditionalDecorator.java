package jadex.bt.decorators;

import jadex.bt.impl.BTAgentFeature;
import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ITriFunction;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.rules.eca.EventType;
import jadex.rules.eca.IAction;

public class ConditionalDecorator<T> extends Decorator<T>
{
	protected ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> function;
	protected ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition;
	
	protected EventType[] events;
	protected IAction<Void> action;

	public ConditionalDecorator<T> setAsyncFunction(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> execute)
	{
		this.function = execute;
		return this;
	}
	
	public ConditionalDecorator<T> setFunction(ITriFunction<Event, NodeState, ExecutionContext<T>, NodeState> execute) 
	{
		this.function = (event, state, context) -> new Future<>(execute.apply(event, state, context));
	    return this;
	}
	
	public ConditionalDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		this.condition = condition;
		return this;
	}
	
	public ConditionalDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		this.condition = (event, state, context) -> new Future<>(condition.apply(event, state, context));
	    return this;
	}
	
	@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		if(function!=null)
		{
			return function.apply(event, state, context);
		}
		else if(condition!=null)
		{
			Future<NodeState> ret = new Future<>();
			IFuture<Boolean> fut = condition.apply(event, state, context);
			fut.then(triggered ->
			{
				ret.setResult(mapToNodeState(triggered));
			}).catchEx(ex -> ret.setResult(NodeState.FAILED));
			return ret;
		}
		else
		{
			return null;
		}
	}
	
	public void observeCondition(EventType[] events, IAction<Void> action)
	{
		this.events = events;
		this.action = action;
	}
	
	public ExecutionContext<T> getExecutionContext()
	{
		return (ExecutionContext)BTAgentFeature.get().getExecutionContext(); // todo: remove cast hack
	}

	public EventType[] getEvents() 
	{
		return events;
	}

	/*public ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> getCondition() 
	{
		return function;
	}*/

	public IAction<Void> getAction() 
	{
		return action;
	}
	
	public ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> getFunction() 
	{
		return function;
	}

	public ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> getCondition() 
	{
		return condition;
	}

	public boolean mapToBoolean(NodeState state)
	{
		return NodeState.RUNNING!=state;
	}
	
	public NodeState mapToNodeState(Boolean state)
	{
		throw new UnsupportedOperationException();
	}
}
