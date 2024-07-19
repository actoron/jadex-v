package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public abstract class TimeoutDecorator<T> extends Decorator<T> 
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
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, T context) 
    {
        Future<NodeState> ret = new Future<>();
       
        Runnable abort = scheduleTimer(ret);
        
        node.internalExecute(event, context).then(s -> 
        {
        	abort.run();
        	ret.setResultIfUndone(s);
        })
        .catchEx(ex -> 
        {
        	abort.run();
        	ret.setResultIfUndone(NodeState.FAILED);
        });

        return ret;
    }
    
    protected abstract Runnable scheduleTimer(Future<NodeState> ret);
    	
}
