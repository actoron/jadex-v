package jadex.bt.decorators;

import java.lang.System.Logger.Level;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ITriFunction;
import jadex.future.IFuture;
import jadex.rules.eca.EventType;

public class SuccessDecorator<T> extends ConditionalDecorator<T>
{
	@Override
	public SuccessDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		return (SuccessDecorator<T>)super.setAsyncCondition(condition);
	}
	
	@Override
	public SuccessDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		return (SuccessDecorator<T>)super.setCondition(condition);
	}
	
	public SuccessDecorator<T> observeCondition(EventType[] events)
	{
		super.observeCondition(events, (event, rule, context, condresult) -> // action
		{
			System.getLogger(getClass().getName()).log(Level.INFO, "success condition triggered: "+event);
			//System.out.println("success condition triggered: "+event);
			
			getNode().succeed(getExecutionContext()); 
			
			return IFuture.DONE;
		});
		
		return this;
	}
	
	@Override
	public NodeState mapToNodeState(Boolean state)
	{
		NodeState ret = state!=null && state? NodeState.SUCCEEDED: NodeState.RUNNING; 
		//System.out.println("map: "+state+":"+ret+" "+this);
		return ret;
	}
}
