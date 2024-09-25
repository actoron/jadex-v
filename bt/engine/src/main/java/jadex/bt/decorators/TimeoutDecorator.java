package jadex.bt.decorators;

import java.lang.System.Logger.Level;

import jadex.bt.impl.Event;
import jadex.bt.impl.ITimerCreator;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

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
    
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> execontext)
	{
		NodeContext<T> context = node.getNodeContext(execontext);
	    node.getNodeContext(execontext).setTimeout(timeout);
	    ITimerCreator<T> tc = execontext.getTimerCreator();
	    
	    ITerminableFuture<Void> fut = context.getTimeout()>0? tc.createTimer(execontext, context.getTimeout()): null;
		if(fut!=null)
		{
			//System.out.println("timeout timer created: "+context.getTimeout());
			context.setTimeoutTimer(fut);
    		fut.then(Void ->
    		{
    			System.getLogger(getClass().getName()).log(Level.INFO, "timeout occurred: "+this);
    			//System.out.println("timeout occurred: "+this);
    			getNode().abort(AbortMode.SELF, NodeState.FAILED, execontext);
    		}).catchEx(ex ->
    		{
    			System.getLogger(getClass().getName()).log(Level.INFO, "timer aborted: "+this);
    			//System.out.println("timer aborted: "+this);
    		});
		}
		
		return null; // allowed as decorator can handle null
	}
	
	public IFuture<NodeState> afterExecute(Event event, NodeState state, ExecutionContext<T> execontext)
	{
		ITerminableFuture<Void> fut = node.getNodeContext(execontext).getTimeoutTimer();
		if(fut!=null)
			fut.terminate();
		return null; //new Future<NodeState>(state);
	}
	
	public IFuture<NodeState> afterAbort(Event event, NodeState state, ExecutionContext<T> context)
	{
		return afterExecute(event, state, context);
	}
}
