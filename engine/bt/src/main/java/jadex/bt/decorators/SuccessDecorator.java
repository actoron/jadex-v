package jadex.bt.decorators;

import java.lang.System.Logger.Level;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ITriFunction;
import jadex.core.ChangeEvent;
import jadex.future.IFuture;

public class SuccessDecorator<T> extends ConditionalDecorator<T>
{
	public SuccessDecorator()
	{
		this.action	= event ->
		{
			//System.out.println("success condition triggered: "+event);
			
			if(getNode().getNodeContext(getExecutionContext())!=null)
			{
				System.getLogger(getClass().getName()).log(Level.INFO, "success condition triggered: "+event);
				getNode().succeed(getExecutionContext()); 
			}
			else
			{
				System.getLogger(getClass().getName()).log(Level.INFO, "success condition triggered but node not active: "+event);
			}
			
		};
	}
	
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
	
	@Override
	public SuccessDecorator<T> setEvents(ChangeEvent... events)
	{
		return (SuccessDecorator<T>) super.setEvents(events);
	}
	
	@Override
	public NodeState mapToNodeState(Boolean state, NodeState nstate)
	{
		NodeState ret = state!=null && state? NodeState.SUCCEEDED: nstate; 
		//System.out.println("map: "+state+":"+ret+" "+this);
		return ret;
	}
}
