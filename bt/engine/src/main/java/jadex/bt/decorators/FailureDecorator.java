package jadex.bt.decorators;

import java.lang.System.Logger.Level;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ITriFunction;
import jadex.future.IFuture;
import jadex.rules.eca.EventType;

public class FailureDecorator <T> extends ConditionalDecorator<T>
{
	public FailureDecorator()
	{
		this.action	= (event, rule, context, condresult) -> // action
		{
			//System.out.println("failure condition triggered: "+event);
			
			if(getNode().getNodeContext(getExecutionContext())!=null)
			{
				System.getLogger(getClass().getName()).log(Level.INFO, "failure condition triggered: "+event);
				getNode().abort(AbortMode.SELF, NodeState.FAILED, getExecutionContext());
			}
			else
			{
				System.getLogger(getClass().getName()).log(Level.INFO, "failure condition triggered, but node not active: "+event);
			}
			return IFuture.DONE;
		};
	}
	
	@Override
	public FailureDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		return (FailureDecorator<T>)super.setAsyncCondition(condition);
	}
	
	@Override
	public FailureDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		return (FailureDecorator<T>)super.setCondition(condition);
	}
	
	@Override
	public FailureDecorator<T> setEvents(EventType... events)
	{
		return (FailureDecorator<T>) super.setEvents(events);
	}
	
	@Override
	public NodeState mapToNodeState(Boolean state, NodeState nstate)
	{
		NodeState ret = state!=null && state? NodeState.FAILED: nstate; 
		//System.out.println("map: "+state+":"+ret+" "+this);
		return ret;
	}
}

