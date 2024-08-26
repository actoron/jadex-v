package jadex.bt;

import java.util.function.BiFunction;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class RepeatDecorator<T> extends Decorator<T> 
{
	protected BiFunction<Node<T>, ExecutionContext<T>, IFuture<Boolean>> condition;
    
    protected int max;
    
    protected long delay;
    
    public RepeatDecorator() 
    {
    	this(null, 0, 0);
    }
    
    public RepeatDecorator(int max, long delay) 
    {
    	this(null, max, delay);
    }
    
    public RepeatDecorator(BiFunction<Node<T>, ExecutionContext<T>, IFuture<Boolean>> condition) 
    {
    	this(condition, 0, 0);
    }
    
    public RepeatDecorator(BiFunction<Node<T>, ExecutionContext<T>, IFuture<Boolean>> condition, int max, long delay) 
    {
    	this.condition = condition==null? (node, context) -> new Future<Boolean>(true): condition;
    	this.max = max;
    	this.delay = delay;
    }

    @Override
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, ExecutionContext<T> execontext) 
    {
        Future<NodeState> ret = new Future<>();
        
        if(max==0 || getAttempt(node.getNodeContext(execontext))<max)
        {
        	incAttempt(node.getNodeContext(execontext));
	        condition.apply(node, execontext).then(rep ->
	        { 
	        	System.out.println("repeat deco: "+node+" "+rep);
	        	node.getNodeContext(execontext).setRepeat(rep);
	        	if(rep)
	        		node.getNodeContext(execontext).setRepeatDelay(delay);
	        }).catchEx(ex2 -> ret.setResult(NodeState.FAILED));
        }
        
        ret.setResult(state);
        
        return ret;
    }
    
    public void abort(NodeContext<T> context)
    {
    	String name = this+".attempt";
    	context.removeValue(name);
    }
    
    protected int getAttempt(NodeContext<T> context)
    {
    	String name = this+".attempt";
		Object ret = context.getValue(name);
		return ret!=null? (Integer)ret: 0;
    }
    
    protected void setAttempt(int attempt, NodeContext<T> context)
    {
		String name = this+".attempt";
		context.setValue(name, attempt);
    }
    
    protected void incAttempt(NodeContext<T> context)
    {
    	int at = getAttempt(context);
    	setAttempt(at+1, context);
    }
}