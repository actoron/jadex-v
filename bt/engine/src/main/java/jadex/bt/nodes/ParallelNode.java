package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 * Execute nodes sequentially until all succeed or one fails.
 */
public class ParallelNode<T> extends CompositeNode<T>
{
	public enum ResultMode
	{
		ON_ONE,
		ON_ALL
	}
	
	protected ResultMode failmode = ResultMode.ON_ONE;
	protected ResultMode successmode = ResultMode.ON_ALL;
	protected boolean keeprunning = false;
	
	public ParallelNode()
	{
	}
	
	public ParallelNode(String name)
	{
		super(name);
	}
	
	public ParallelNode<T> setFailureMode(ResultMode failmode)
	{
		this.failmode = failmode;
		return this;
	}
	
	public ParallelNode<T> setSuccessMode(ResultMode successmode)
	{
		this.successmode = successmode;
		return this;
	}

	public boolean isKeepRunning() 
	{
		return keeprunning;
	}

	public ParallelNode<T> setKeepRunning(boolean keeprunning) 
	{
		this.keeprunning = keeprunning;
		return this;
	}

	@Override
	public CompositeNode<T> addChild(Node<T> child, Event event, ExecutionContext<T> execontext)
	{
		super.addChild(child, event, execontext);
		
		// Child can be directly be activated in parallel node
		//System.out.println("newChildAdded: "+getChildCount()+" "+getChildren());
		NodeContext<T> context = getNodeContext(execontext);
			
		// If execution is ongoing, abort children, reset this node, and reexecute
        Future<NodeState> call = context.getCallFuture();
        if(call != null && !call.isDone()) 
        {
        	//System.out.println("execute again called: "+this+" "+ret+" "+context.isRepeat());
   			getLogger().log(Level.INFO, "execute new child: "+this+" "+context.getState());
        	
   			executeChild(child, event, getRuntimeInfo(execontext).getResults(), execontext, getRuntimeInfo(execontext).getInternalCall());
        }
        else
        {
        	getLogger().log(Level.INFO, "child added but node not running: "+this+" "+context.getState());
        }
        
        return this;
	}
	
    public IFuture<NodeState> internalExecute(Event event, NodeState mystate, ExecutionContext<T> execontext) 
    {
        Map<Node<T>, IFuture<NodeState>> results = new LinkedHashMap<>();
    	Future<NodeState> ret = new Future<>();
    	NodeContext<T> context = getNodeContext(execontext);
		getRuntimeInfo(execontext).setInternalCall(ret);
		getRuntimeInfo(execontext).setResults(results);
    	//context.setInternalCall(ret);
    	//context.setResults(results);
    	
        for(int i=0; i<getChildCount(execontext); i++) 
        {
        	if(executeChild(getChild(i, execontext), event, results, execontext, ret))
        		break;
        }
		executeAbortWait(context).then(abortstate -> handleFinish(abortstate, ret, results, execontext));
        
        return ret;
    }

	protected IFuture<NodeState> executeAbortWait(NodeContext<T> context)
	{
		Future<NodeState> ret = new Future<>();
		//context.setAbortWait(ret);
		getRuntimeInfo(context).setAbortWait(ret);
		return ret;
	} 

    protected boolean executeChild(Node<T> child, Event event, Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> context, Future<NodeState> fut)
    {
    	boolean ret = false;
    	
    	IFuture<NodeState> childfut = child.execute(event, context);
    	results.put(child, childfut);
         
        if(childfut.isDone())
        {
        	NodeState state = childfut.get();
        	if(checkFastExit(state))
         	{
         		handleFinish(state, fut, results, context);
         		ret = true;
         	}
         	
         	state = checkFinished(results, context);
         	if(state!=null)
         	{
         		handleFinish(state, fut, results, context);
         	}
         }
         
         childfut.then(state -> handleResult(state, results, fut, context))
         	.catchEx(ex -> handleResult(NodeState.FAILED, results, fut, context));
         
         return ret;
    }
    
    protected boolean checkFastExit(NodeState state)
    {
    	return !keeprunning && (failmode==ResultMode.ON_ONE && NodeState.FAILED==state || successmode==ResultMode.ON_ONE && NodeState.SUCCEEDED==state);
    }
    
    protected NodeState checkFinished(Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> execontext)
    {
    	if(isKeepRunning())
    		return null;
    	
    	if(checkSuccess(results, execontext))
    		return NodeState.SUCCEEDED;
    	else if(checkFailure(results, execontext))
    		return NodeState.FAILED;
    	return null;
    }
    
    protected boolean checkSuccess(Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> execontext)
    {
    	boolean ret = false;
    	if(ResultMode.ON_ALL==successmode && results.size()==getChildCount(execontext))
    		ret = results.values().stream().allMatch(result -> result.isDone() && result.get()==NodeState.SUCCEEDED);
    	return ret;
    }
    
    protected boolean checkFailure(Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> execontext)
    {
    	boolean ret = false;
    	if(ResultMode.ON_ALL==failmode && results.size()==getChildCount(execontext))
    		ret = results.values().stream().allMatch(result -> result.isDone() && result.get()==NodeState.FAILED);
    	return ret;
    }
    
    protected void handleFinish(NodeState state, Future<NodeState> ret, Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> context)
    {
		System.out.println("handleFinish: "+this+" "+state+" "+context.getNodeContext(this).getAbortState());
    	if(ret.isDone())
		{
    		return;
		}
		else if(context.getNodeContext(this).getAbortState()!=null)
		{
			ret.setResultIfUndone(context.getNodeContext(this).getAbortState());
		}
		else
		{
			for(Entry<Node<T>, IFuture<NodeState>> entry: results.entrySet()) 
			{
				if(!entry.getValue().isDone())
				{
					entry.getKey().abort(AbortMode.SELF, NodeState.FAILED, context);
				}
			}
    	
			//getNodeContext(context).setState(state); automatically set in node
			ret.setResultIfUndone(state);
		}
    }
    
    protected void handleResult(NodeState state, Map<Node<T>, IFuture<NodeState>> results, Future<NodeState> ret, ExecutionContext<T> context) 
    {
        if(checkFastExit(state)) 
        {
            handleFinish(state, ret, results, context);
        } 
        else 
        {
            NodeState finstate = checkFinished(results, context);
            if(finstate != null)
                handleFinish(finstate, ret, results, context);
        }
    }
    
    public int getCurrentChildCount(ExecutionContext<T> context)
    {
    	// hmmm?!
    	return getChildCount(context);
    }
    
    /*@Override
    public NodeContext<T> copyNodeContext(NodeContext<T> src)
    {
    	ParallelNodeContext<T> ret = (ParallelNodeContext<T>)super.copyNodeContext(src);
    	ret.setInternalCall(((ParallelNodeContext<T>)src).getInternalCall());
    	ret.setResults(((ParallelNodeContext<T>)src).getResults());
		return ret;
    }*/

	public boolean hasIndex()
    {
    	return false;
    }

	public RuntimeInfo<T> getRuntimeInfo(ExecutionContext<T> execontext)
	{
		return getRuntimeInfo(getNodeContext(execontext));
	}

	public RuntimeInfo<T> getRuntimeInfo(NodeContext<T> nc)
	{
		RuntimeInfo<T> ret = (RuntimeInfo<T>)nc.getValue("runtimeinfo");
		if(ret==null)
		{
			ret = new RuntimeInfo<T>();
			nc.setValue("runtimeinfo", ret);
		}
		return ret;
	}


    public static class RuntimeInfo<T>
    {
    	protected Future<NodeState> internalcall;
    	protected Map<Node<T>, IFuture<NodeState>> results;
		protected Future<NodeState> abortwait;

		public Future<NodeState> getInternalCall() 
		{
			return internalcall;
		}

		public void setInternalCall(Future<NodeState> internalcall) 
		{
			//System.out.println("usercall: "+usercall+" "+this);
			this.internalcall = internalcall;
		}

		public Map<Node<T>, IFuture<NodeState>> getResults() 
		{
			return results;
		}

		public void setResults(Map<Node<T>, IFuture<NodeState>> results) 
		{
			this.results = results;
		}

		public Future<NodeState> getAbortWait()
		{
			return abortwait;
		}

		public void setAbortWait(Future<NodeState> abortwait)
		{
			this.abortwait = abortwait;	
		}
    }

	public void setAbortFuture(IFuture<Void> abortfuture, NodeContext<T> context) 
	{
		super.setAbortFuture(abortfuture, context);
		Future<NodeState> aw = getRuntimeInfo(context).getAbortWait();
		if(aw!=null)
			aw.setResultIfUndone(context.getAbortState());
	}
}

