package jadex.bt.decorators;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ITriFunction;
import jadex.future.IFuture;
import jadex.rules.eca.EventType;

public class FailureDecorator <T> extends ConditionalDecorator<T>
{
	public FailureDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		return (FailureDecorator<T>)super.setAsyncCondition(condition);
	}
	
	public FailureDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		return (FailureDecorator<T>)super.setCondition(condition);
	}
	
	public FailureDecorator<T> observeCondition(EventType[] events)
	{
		super.observeCondition(events, (event, rule, context, condresult) -> // action
		{
			System.out.println("failure condition triggered: "+event);
			
			getNode().abort(AbortMode.SELF, NodeState.FAILED, getExecutionContext());
			
			return IFuture.DONE;
		});
		return this;
	}
	
	public NodeState mapToNodeState(Boolean state)
	{
		NodeState ret = state!=null && state? NodeState.FAILED: NodeState.RUNNING; 
		//System.out.println("map: "+state+":"+ret+" "+this);
		return ret;
	}
}

