package jadex.bt.decorators;

import java.lang.System.Logger.Level;

import jadex.bt.impl.Event;
import jadex.bt.impl.ITimerCreator;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.ITriFunction;
import jadex.common.Tuple2;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.rules.eca.EventType;

public class RepeatDecorator<T> extends ConditionalDecorator<T> 
{
    protected int max;
    
    protected long delay;
    
    protected long timeout;
    
    public RepeatDecorator() 
    {
    	this(null, 0, 0);
    }
    
    public RepeatDecorator(int max, long delay) 
    {
    	this(null, max, delay);
    }
    
    public RepeatDecorator(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition) 
    {
    	this(condition, 0, 0);
    }
    
    public RepeatDecorator(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition, int max, long delay) 
    {
    	this.condition = condition==null? (event, state, context) -> new Future<Boolean>(true): condition;
    	this.max = max;
    	this.delay = delay;
    }
    
    public long getTimeout() 
    {
		return timeout;
	}

	public RepeatDecorator<T> setTimeout(long timeout) 
	{
		this.timeout = timeout;
		return this;
	}

	@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		return null;
	}
	
	@Override
    public IFuture<NodeState> afterExecute(Event event, NodeState state, ExecutionContext<T> execontext) 
    {
        Future<NodeState> ret = new Future<>();

        NodeContext<T> context = node.getNodeContext(execontext);
        ITimerCreator<T> tc = execontext.getTimerCreator();
        
        if(max == 0 || getAttempt(context) < max) 
        {
        	incAttempt(context);
        	condition.apply(event, state, execontext).then(rep -> 
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
                            dfut = tc.createTimer(execontext, delay);
                            context.setRepeatDelayTimer(dfut);
                        } 
                        else 
                        {
                            dfut = new TerminableFuture<>();
                            ((TerminableFuture<Void>)dfut).setResult(null);
                        }

                        dfut.then(Void -> 
                        {
                        	System.getLogger(getClass().getName()).log(Level.INFO, "repeat node: "+getNode());
                            //System.out.println("repeat node: "+getNode());
                            
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
                	if(getEvents()!=null)
                	{
	                	IFuture<Void> condfut = waitForCondition(e -> {
	                		Future<Tuple2<Boolean, Object>> cret = new Future<>();
	                		IFuture<Boolean> fut = condition.apply(new Event(e.getType().toString(), e.getContent()), node.getNodeContext(getExecutionContext()).getState(), getExecutionContext());
	        				fut.then(triggered ->
	        				{
	        					cret.setResult(new Tuple2<>(triggered, null));
	        				}).catchEx(ex -> 
	        				{
	        					cret.setResult(new Tuple2<>(false, null));
	        				});
	            			return cret;
	                	}, getEvents(), getTimeout(), execontext);
	                	
	                	condfut.then(Void -> 
                        {
                        	System.getLogger(getClass().getName()).log(Level.INFO, "conditional repeat node: "+getNode());
                            //System.out.println("repeat node: "+getNode());
                            
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
                	else
                	{
                		ret.setResult(state);
                	}
                }
            }).catchEx(ex2 -> ret.setResult(NodeState.FAILED));
        } 
        else 
        {
            ret.setResult(state); 
        }

        return ret;
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
    
    /*public RepeatDecorator<T> observeCondition(EventType[] events)
	{
		super.observeCondition(events, (event, rule, context, condresult) -> // action
		{
			System.getLogger(getClass().getName()).log(Level.INFO, "repeat condition triggered: "+event);
			//System.out.println("success condition triggered: "+event);
			
			getNode().succeed(getExecutionContext()); 
			
			return IFuture.DONE;
		});
		
		return this;
	}*/
}