package jadex.bt.decorators;

import jadex.bt.impl.Event;
import jadex.bt.impl.ITimerCreator;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.ITriFunction;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class RepeatDecorator<T> extends Decorator<T> 
{
	protected ITriFunction<Node<T>, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition;
    
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
    
    public RepeatDecorator(ITriFunction<Node<T>, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition) 
    {
    	this(condition, 0, 0);
    }
    
    public RepeatDecorator(ITriFunction<Node<T>, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition, int max, long delay) 
    {
    	this.condition = condition==null? (node, state, context) -> new Future<Boolean>(true): condition;
    	this.max = max;
    	this.delay = delay;
    }

    @Override
    public IFuture<NodeState> afterExecute(Event event, NodeState state, ExecutionContext<T> execontext) 
    {
        Future<NodeState> ret = new Future<>();

        NodeContext<T> context = node.getNodeContext(execontext);
        ITimerCreator<T> tc = execontext.getTimerCreator();
        
        if(context.getCall()==null || context.getCall().isDone())
        	System.out.println("after call finished");

        if(max == 0 || getAttempt(context) < max) 
        {
            incAttempt(context);
            condition.apply(node, state, execontext).then(rep -> 
            {
                if(rep) 
                {
                	context.setRepeat(true);

                    if(context.getAborted() != AbortMode.SELF) 
                    {
                        ITerminableFuture<Void> dfut = null;
                        if(delay > 0) 
                        {
                        	context.setRepeatDelay(delay);
                            dfut = tc.createTimer(getNode(), execontext, delay);
                            context.setRepeatDelayTimer(dfut);
                        } 
                        else 
                        {
                            dfut = new TerminableFuture<>();
                            ((TerminableFuture<Void>)dfut).setResult(null);
                        }

                        dfut.then(Void -> 
                        {
                            System.out.println("repeat node: "+getNode());
	            			//reset(execontext); must not reset here, is done in execute on reexecution call
	            			IFuture<NodeState> fut = getNode().execute(event, execontext);
	            			if(fut!=ret && !ret.isDone()) // leads to duplicate result when both already done :-( HACK! todo: fix delegateTo
	            				fut.delegateTo(ret);
                        }).catchEx(e -> 
                        {
                            e.printStackTrace();
                            getNode().reset(execontext, true);
                            ret.setResultIfUndone(NodeState.FAILED);
                        });
                    }
                } 
                else 
                {
                    ret.setResult(state); 
                }
            }).catchEx(ex2 -> ret.setResult(NodeState.FAILED));
        } 
        else 
        {
            ret.setResult(state); 
        }

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