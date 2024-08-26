package jadex.bt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;
import jadex.rules.eca.EventType;

public abstract class Node<T>
{
	public static AtomicInteger idgen = new AtomicInteger();
	
	public enum NodeState 
	{
	    SUCCEEDED,
	    FAILED,
	    RUNNING
	}

	public enum AbortMode 
	{
	    NONE,
	    SELF,
	    SUBTREE,
	}
	
	protected int id = idgen.incrementAndGet();
	protected Node<T> parent;
	protected List<Decorator<T>> beforedecos = new ArrayList<>();
	protected List<Decorator<T>> afterdecos = new ArrayList<>();
	
	protected BiFunction<Node<T>, Supplier<ExecutionContext<T>>, Boolean> triggercondition;
	protected EventType[] triggerevents;
	
	protected BiFunction<Node<T>, Supplier<ExecutionContext<T>>, Boolean> successcondition;
	protected EventType[] successevents;

	public abstract IFuture<NodeState> internalExecute(Event event, ExecutionContext<T> context);
	
	public Node<T> setParent(Node<T> parent)
	{
		this.parent = parent;
		return this;
	}
	
	public Node<T> getParent()
	{
		return parent;
	}
	
	public BiFunction<Node<T>, Supplier<ExecutionContext<T>>, Boolean> getTriggerCondition() 
	{
		return triggercondition;
	}
	
	public EventType[] getTriggerEvents() 
	{
		return triggerevents;
	}

	public Node<T> setTriggerCondition(BiFunction<Node<T>, Supplier<ExecutionContext<T>>, Boolean> condition, EventType[] events) 
	{
		this.triggercondition = condition;
		this.triggerevents = events;
		return this;
	}
	
	public BiFunction<Node<T>, Supplier<ExecutionContext<T>>, Boolean> getSuccessCondition() 
	{
		return successcondition;
	}
	
	public EventType[] getSuccessEvents() 
	{
		return successevents;
	}
	
	public Node<T> setSuccessCondition(BiFunction<Node<T>, Supplier<ExecutionContext<T>>, Boolean> condition, EventType[] events) 
	{
		this.successcondition = condition;
		this.successevents = events;
		return this;
	}
	
	public void collectNodes(Collection<Node<T>> nodes)
	{
		nodes.add(this);
	}
	
	public Node<T> getRoot()
	{
		Node<T> ret = this;
		while(ret.getParent()!=null)
			ret = ret.getParent();
		return ret;
	}
	
	public void addBeforeDecorator(Decorator<T> deco)
	{
		beforedecos.add(deco);
	}
	
	public void addAfterDecorator(Decorator<T> deco)
	{
		afterdecos.add(deco);
	}
	
	public IFuture<NodeState> execute(Event event, ExecutionContext<T> execontext) 
	{
		Future<NodeState> ret;
		if(execontext==null)
		{
			ret = new Future<NodeState>();
			ret.setException(new RuntimeException("execution context must not null: "+this));
			return ret;
		}
		
		NodeContext<T> context = getNodeContext(execontext);
		
	    // If execution is ongoing, abort children, reset this node, and reexecute
        Future<NodeState> call = context.getCall();
        if(call != null && !call.isDone()) 
        {
            ret = call;
        	System.out.println("execute again called: "+this+" "+ret+" "+context.isRepeat());
            boolean rep = context.isRepeat();
            reset(execontext, !rep);  // First, reset the state of this node
            context.setState(NodeState.RUNNING);  // Set state to running again (reset sets to null)
            
            // In case of repeat, nothing to abort and call must not return
            if(!rep)
            {
            	abort(AbortMode.SUBTREE, NodeState.FAILED, execontext);  // Then abort children to execute this node further
            	return ret;  // Return as execution is continued by the current context
            }
        }
		else
		{
			System.out.println("execute newly called: "+this);
			ret = new Future<>();
			context.setCall(ret);
				context.setState(NodeState.RUNNING);
			
			if(getParent()==null)
				execontext.setRootCall(ret);
			ret.then(state -> 
			{
				System.out.println("node fini: "+state+" "+this+" "+ret);
				context.setState(state);
			}).catchEx(ex -> 
			{
				System.out.println("node fini failed: "+ex+" "+this);
				context.setState(NodeState.FAILED);
			});
		}
		
        Consumer<Exception> handleerr = new Consumer<>() 
        {
			@Override
			public void accept(Exception e)
			{
				if(e!=null)
					e.printStackTrace();
	           	reset(execontext, true);
	        	ret.setResultIfUndone(NodeState.FAILED);
			}
		};
        
		// Decorators are always executed independent of the node state
        executeDecorators(beforedecos, 0, event, NodeState.RUNNING, s -> s!=NodeState.RUNNING, execontext).then(bs ->
        {
        	if(bs != NodeState.RUNNING) 
      	    {
        		System.out.println("beforedecos give: "+bs+" "+this);
        		// no abort necessary because nothing is running
                reset(execontext, true);
              	ret.setResult(bs);
             }
        	else
        	{
        		ITimerCreator<T> tc = execontext.getTimerCreator();
        		
        		ITerminableFuture<Void> timeout = context.getTimeout()>0? tc.createTimer(this, execontext, context.getTimeout()): null;
        		if(timeout!=null)
        		{
        			//System.out.println("timeout timer created: "+context.getTimeout());
        			context.setTimeoutTimer(timeout);
	        		timeout.then(Void ->
	        		{
	        			//System.out.println("timeout occurred: "+this);
	        			abort(AbortMode.SELF, NodeState.FAILED, execontext);
	        		}).catchEx(ex ->
	        		{
	        			System.out.println("timer aborted: "+this);
	        		});
        		}
        		
        		System.out.println("internalExecute: "+this);
    			IFuture<NodeState> fut = internalExecute(event, execontext);
    			
        		Consumer<NodeState> doafter = new Consumer<>()
	        	{
	        		public void accept(NodeState state)
	        		{
	        			try
	        			{
	    	        		if(state!=NodeState.SUCCEEDED && state!=NodeState.FAILED)
	        	        		System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "Should only receive final state: "+state);
	
	    	        		if(timeout!=null)
	    	        			timeout.terminate();
	    	        		
	    	        		if(context.isRepeat() && context.getAborted()!=AbortMode.SELF)
	    	   	        	{
	    	              		ITerminableFuture<Void> delay = null;
	    	              		if(context.getRepeatDelay()>0)
	    	              		{
	    	              			delay = tc.createTimer(Node.this, execontext, context.getRepeatDelay());
	    	              			context.setRepeatDelayTimer(delay);
	    	              		}
	    	              		else
	    	              		{
	    	              			delay = new TerminableFuture<Void>();
	    	              			((TerminableFuture<Void>)delay).setResult(null);
	    	              		}
	    	              			
	    	              		delay.then(Void ->
	    	            		{
	    	            			System.out.println("repeat node: "+Node.this);
	    	            			//reset(execontext); must not reset here, is done in execute on reexecution call
	    	            			IFuture<NodeState> fut = execute(event, execontext);
	    	            			if(fut!=ret && !ret.isDone()) // leads to duplicate result when both already done :-( HACK! todo: fix delegateTo
	    	            				fut.delegateTo(ret);
		    	   	        	}).catchEx(e ->
		    		            {
		    		            	handleerr.accept(e);
		    		            });
	    	   	        	}
	    	   	        	else
	    	   	        	{
	    	   	        		reset(execontext, true);
	    	   	        		ret.setResultIfUndone(state);
	    	   	        	}
	        			}
	        			catch(Exception e)
	        			{
	        				handleerr.accept(e);
	        			}
	        		}
	        	};
        		
    	        fut.then(state -> 
    	        {
    	        	if(state!=NodeState.SUCCEEDED && state!=NodeState.FAILED)
    	        		System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "Should only receive final state: "+state);
    	        	//System.out.println("state is: "+state);
    	        	
    	        	executeDecorators(afterdecos, 0, event, state, null, execontext).then(as ->
    	        	{
    	   	        	doafter.accept(as);
    	        	})
    	        	.catchEx(e ->
    	            {
    	            	handleerr.accept(e);
    	            });
    	        	
    	        }).catchEx(ex -> 
    	        {
    	        	executeDecorators(afterdecos, 0, event, NodeState.FAILED, null, execontext).then(as ->
    	        	{
    	        		doafter.accept(as);
    	        	})
    	        	.catchEx(e ->
    	            {
    	            	handleerr.accept(e);
    	            });
    	        });
        		
        	}
        }).catchEx(e ->
        {
        	// Should not happen exceptions in decorators are handled in method
        	handleerr.accept(e);
        });
        
        return ret;
	}
	
	protected IFuture<NodeState> executeDecorators(List<Decorator<T>> decos, int i, Event event, NodeState state, Predicate<NodeState> abort, ExecutionContext<T> context)
	{
		Future<NodeState> ret = new Future<>();
		
		if(abort!=null && abort.test(state)) // when to abort decorator execution
		{
			ret.setResult(state);
		}
		else
		{
			Decorator<T> deco = i<decos.size()? decos.get(i): null;
			
			if(deco!=null)
			{
				try
				{
					deco.execute(this, event, state, context).then(s ->
					{
						executeDecorators(decos, i+1, event, s, abort, context).delegateTo(ret);
					}).catchEx(e ->
					{
						e.printStackTrace();
						executeDecorators(decos, i+1, event, state, abort, context).delegateTo(ret); // ignore decorator exception?! 
					});
				}
				catch(Exception e)
				{
					e.printStackTrace();
					executeDecorators(decos, i+1, event, state, abort, context).delegateTo(ret);
				}
			}
			else
			{
				ret.setResult(state);
			}
		}
		
		return ret;
	}

    public NodeContext<T> getNodeContext(ExecutionContext<T> execontext)
    {
    	NodeContext<T> ret = execontext.getNodeContext(this);
    	if(ret==null)
    	{
    		ret = createNodeContext();
    		execontext.setNodeContext(this, ret);
    	}
    	return ret;
    }
    
    protected NodeContext<T> createNodeContext()
    {
    	return new NodeContext<T>();
    }
    
    /*public void abort(AbortMode abortmode, ExecutionContext<T> execontext)
    {
    	abort(abortmode, NodeState.FAILED, execontext);
    }*/
    
    public void abort(AbortMode abortmode, NodeState state, ExecutionContext<T> execontext)
    {
    	NodeContext<T> context = getNodeContext(execontext);
    	
       	if(context.getAborted()!=null || NodeState.RUNNING!=context.getState())
    		return;

    	context.setAborted(abortmode);
    	
    	if(AbortMode.SELF==abortmode)
    	{
           	System.out.println("abort: "+this+" "+context.getState()+" "+state);
           	
           	if(this.toString().indexOf("patrol")!=-1)
           		System.out.println("hererer");
           	
	    	if(context.getRepeatDelayTimer()!=null)
	    		context.getRepeatDelayTimer().terminate();
	    	
	    	if(context.getTimeoutTimer()!=null)
	    		context.getTimeoutTimer().terminate();
	    	
	    	if(context.getCall()!=null)
	    		context.getCall().setResultIfUndone(state);
    	}
    }
    
    public void reset(ExecutionContext<T> execontext, Boolean all)
    {
  		NodeContext<T> context = getNodeContext(execontext);
  		if(all==null)
      		all = !context.isRepeat();
      	
      	//System.out.println("Clear context: "+this+" all="+!context.isRepeat());
      	context.reset(all);
    }
    
    public void succeed(ExecutionContext<T> execontext)
    {
    	System.out.println("node succeeded: "+this);
    	abort(AbortMode.SELF, NodeState.SUCCEEDED, execontext);
    }

	@Override
	public int hashCode() 
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) 
	{
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Node<T> other = (Node<T>)obj;
		return id == other.id;
	}

	@Override
	public String toString() 
	{
		return "Node [id=" + id + ", class="+SReflect.getUnqualifiedClassName(getClass())+"]";
	}
}
