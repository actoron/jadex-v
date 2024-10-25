package jadex.bt.decorators;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;

public class RetryDecorator<T> extends RepeatDecorator<T>
{
    public RetryDecorator() 
    {
    	this(0, 0);
    }
    
    public RetryDecorator(long delay) 
    {
    	this(0, delay);
    }
    
    public RetryDecorator(int max, long delay) 
    {
    	super(max, delay);
    }
    
    public boolean isRepeatAllowed(Event event, NodeState state, ExecutionContext<T> context)
    {
    	return NodeState.FAILED==state;
    }
}