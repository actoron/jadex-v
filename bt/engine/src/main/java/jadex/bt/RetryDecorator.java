package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class RetryDecorator<T> extends Decorator<T> 
{
    protected int max;
    
    protected long delay;

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
    	this.max = max;
    	this.delay = delay;
    }

    @Override
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, ExecutionContext<T> execontext) 
    {
        Future<NodeState> ret = new Future<>();
        
        int attempt = getAttempt(node.getNodeContext(execontext));
        
        if(state == NodeState.FAILED && (attempt < max || max==0)) 
        {
        	setAttempt(attempt+1, node.getNodeContext(execontext));
        	node.getNodeContext(execontext).setRepeat(true);
        	System.out.println("retry deco: "+delay);
        	if(delay>0)
        		node.getNodeContext(execontext).setRepeatDelay(delay);
        }
            
        ret.setResult(state);
        
        return ret;
    }
    
    public void abort(NodeContext<T> context)
    {
    	String name = this+".attempt.noreset";
    	context.removeValue(name);
    }
    
    protected int getAttempt(NodeContext<T> context)
    {
    	String name = this+".attempt.noreset";
		Object ret = context.getValue(name);
		return ret!=null? (Integer)ret: 0;
    }
    
    protected void setAttempt(int attempt, NodeContext<T> context)
    {
		String name = this+".attempt.noreset";
		context.setValue(name, attempt);
    }
}