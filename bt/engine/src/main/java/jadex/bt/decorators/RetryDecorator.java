package jadex.bt.decorators;

import jadex.bt.nodes.Node.NodeState;
import jadex.future.Future;

// todo: make this work with repeat condition!
public class RetryDecorator<T> extends RepeatDecorator<T>
{
    public RetryDecorator() 
    {
    	this(0, 0);
    }
    
    public RetryDecorator(int max) 
    {
    	this(max, 0);
    }
    
    public RetryDecorator(int max, long delay) 
    {
    	super((event, state, context) -> new Future<Boolean>(NodeState.FAILED==state), max, delay);
    }
}