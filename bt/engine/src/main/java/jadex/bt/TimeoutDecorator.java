package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class TimeoutDecorator<T> extends Decorator<T> 
{
    protected long timeout;
    
    public TimeoutDecorator(long timeout) 
    {
        this.timeout = timeout;
    }
    
    public long getTimeout() 
    {
		return timeout;
	}
    
    @Override
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, ExecutionContext<T> context) 
    {
    	node.getNodeContext(context).setTimeout(timeout);
        return new Future<NodeState>(state);
    }
    
}
