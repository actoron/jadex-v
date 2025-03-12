package jadex.bt.decorators;

import java.lang.System.Logger.Level;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.ITriFunction;
import jadex.common.Tuple2;
import jadex.execution.ITimerCreator;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

public class RepeatDecorator<T> extends ConditionalDecorator<T> 
{
    protected int max;
    
    protected long delay;
    
    protected long timeout;
    
    public RepeatDecorator() 
    {
    	this(null, 0, 0);
    }
    
    public RepeatDecorator(long delay) 
    {
    	this(null, 0, delay);
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
    	this.condition = condition;//condition==null? (event, state, context) -> new Future<Boolean>(true): condition;
    	this.max = max;
    	this.delay = delay;
    }
    
    public boolean isRepeatAllowed(Event event, NodeState state, ExecutionContext<T> context)
    {
    	return true;
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
		//System.out.println("afterExecute: "+this+" "+state);
		
        Future<NodeState> ret = new Future<>();

        NodeContext<T> context = node.getNodeContext(execontext);
        ITimerCreator tc = execontext.getTimerCreator();
        
        if(context.isFinishedInBefore())
	    {
	        System.out.println("RepeatDecorator: Execution aborted in beforeExecute. Stopping repeat.");
	        ret.setResult(state); 
	        return ret; 
	    }
        
        Runnable repeat = new Runnable()
        {
        	@Override
        	public void run() 
        	{
        		System.getLogger(getClass().getName()).log(Level.INFO, "repeat node: "+getNode());
                //System.out.println("repeat node: "+getNode());
                
    			//reset(execontext); must not reset here, is done in execute on reexecution call
    			IFuture<NodeState> fut = getNode().execute(event, execontext);
    			if(fut!=ret && !ret.isDone()) // leads to duplicate result when both already done :-( HACK! todo: fix delegateTo
    				fut.delegateTo(ret);	
        	}
        };
        
        if(max == 0 || getAttempt(context) < max) 
        {
        	incAttempt(context);
        	//condition.apply(event, state, execontext).then(rep -> 
        	if(getCondition()==null)
        	{
        		if(isRepeatAllowed(event, state, execontext))
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
	                    	repeat.run();
	                    }).catchEx(e -> 
	                    {
	                        //e.printStackTrace();
	                        getNode().reset(execontext, true);
	                        ret.setResultIfUndone(NodeState.FAILED);
	                    });
	                }
        		}
        		else
        		{
        			ret.setResult(state);
        		}
        	} 
        	else // wait for condition
            {
        		if(getEvents()==null)
        			System.out.println("condition without events: "+condition);
        		System.out.println("repeat decorator is waiting for condition");
        		
        		IFuture<Boolean> cfut = getCondition().apply(event, node.getNodeContext(getExecutionContext()).getState(), getExecutionContext());
				cfut.then(triggered ->
				{
					if(triggered)
					{
						repeat.run();
					}
					else
					{
						IFuture<Void> condfut = waitForCondition(e -> 
		            	{
		            		Future<Tuple2<Boolean, Object>> cret = new Future<>();
		            		IFuture<Boolean> fut = getCondition().apply(new Event(e.getType().toString(), e.getContent()), node.getNodeContext(getExecutionContext()).getState(), getExecutionContext());
		    				fut.then(triggered2 ->
		    				{
		    					cret.setResult(new Tuple2<>(triggered2, null));
		    				}).catchEx(ex -> 
		    				{
		    					cret.setResult(new Tuple2<>(false, null));
		    				});
		        			return cret;
		            	}, getEvents(), getTimeout(), execontext);
		            	
		            	condfut.then(Void -> 
		                {
		                	repeat.run();
		                }).catchEx(e -> 
		                {
		                    e.printStackTrace();
		                    getNode().reset(execontext, true);
		                    ret.setResultIfUndone(NodeState.FAILED);
		                });
					}
				}).catchEx(ex -> 
				{
					 ret.setResult(state); 
				});
            }
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