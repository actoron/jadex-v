package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

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
	
	@Override
	protected void newChildAdded(Node<T> child, Event event, ExecutionContext<T> execontext)
	{
		// Child can be directly be activated in parallel node
		System.out.println("activating child if node is running");
		ParallelNodeContext<T> context = getNodeContext(execontext);
			
		// If execution is ongoing, abort children, reset this node, and reexecute
        Future<NodeState> call = context.getCall();
        if(call != null && !call.isDone()) 
        {
        	//System.out.println("execute again called: "+this+" "+ret+" "+context.isRepeat());
   			System.getLogger(this.getClass().getName()).log(Level.INFO, "execute new child: "+this+" "+context.getState());
        	
   			executeChild(child, event, context.getResults(), execontext, context.getInternalCall());
        }
        else
        {
        	System.getLogger(this.getClass().getName()).log(Level.INFO, "child added but node not running: "+this+" "+context.getState());
        }
	}
	
    public IFuture<NodeState> internalExecute(Event event, NodeState mystate, ExecutionContext<T> execontext) 
    {
        List<IFuture<NodeState>> results = new ArrayList<>();
    	Future<NodeState> ret = new Future<>();
    	ParallelNodeContext<T> context = getNodeContext(execontext);
    	context.setInternalCall(ret);
    	context.setResults(results);
    	
        for(int i=0; i<getChildCount(); i++) 
        {
        	if(executeChild(getChild(i), event, results, execontext, ret))
        		break;
        }
        
        return ret;
    }
    
    protected boolean executeChild(Node<T> child, Event event, List<IFuture<NodeState>> results, ExecutionContext<T> context, Future<NodeState> fut)
    {
    	boolean ret = false;
    	
    	IFuture<NodeState> childfut = child.execute(event, context);
    	results.add(childfut);
         
        if(childfut.isDone())
        {
        	NodeState state = childfut.get();
        	if(checkFastExit(state))
         	{
         		handleFinish(state, fut, results, context);
         		ret = true;
         	}
         	
         	state = checkFinished(results);
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
    	return failmode==ResultMode.ON_ONE && NodeState.FAILED==state || successmode==ResultMode.ON_ONE && NodeState.SUCCEEDED==state;
    }
    
    protected NodeState checkFinished(List<IFuture<NodeState>> results)
    {
    	if(checkSuccess(results))
    		return NodeState.SUCCEEDED;
    	else if(checkFailure(results))
    		return NodeState.FAILED;
    	return null;
    }
    
    protected boolean checkSuccess(List<IFuture<NodeState>> results)
    {
    	boolean ret = false;
    	if(ResultMode.ON_ALL==successmode && results.size()==getChildCount())
    		ret = results.stream().allMatch(result -> result.isDone() && result.get()==NodeState.SUCCEEDED);
    	return ret;
    }
    
    protected boolean checkFailure(List<IFuture<NodeState>> results)
    {
    	boolean ret = false;
    	if(ResultMode.ON_ALL==failmode && results.size()==getChildCount())
    		ret = results.stream().allMatch(result -> result.isDone() && result.get()==NodeState.FAILED);
    	return ret;
    }
    
    protected void handleFinish(NodeState state, Future<NodeState> ret, List<IFuture<NodeState>> results, ExecutionContext<T> context)
    {
    	if(ret.isDone())
    		return;
    	
    	for(int i=0; i<results.size(); i++) 
    	{
    		if(!results.get(i).isDone())
    		{
    			getChild(i).abort(AbortMode.SELF, NodeState.FAILED, context);
    		}
    	}
    	
    	//getNodeContext(context).setState(state); automatically set in node
   		ret.setResultIfUndone(state);
    }
    
    protected void handleResult(NodeState state, List<IFuture<NodeState>> results, Future<NodeState> ret, ExecutionContext<T> context) 
    {
        if(checkFastExit(state)) 
        {
            handleFinish(state, ret, results, context);
        } 
        else 
        {
            NodeState finstate = checkFinished(results);
            if(finstate != null)
                handleFinish(finstate, ret, results, context);
        }
    }
    
    public int getCurrentChildCount(ExecutionContext<T> context)
    {
    	// hmmm?!
    	return getChildCount();
    }
    
    @Override
    public ParallelNodeContext<T> getNodeContext(ExecutionContext<T> execontext) 
    {
    	return (ParallelNodeContext<T>)super.getNodeContext(execontext);
    }
    
    protected NodeContext<T> createNodeContext()
    {
    	return new ParallelNodeContext<T>();
    }
    
    public static class ParallelNodeContext<T> extends NodeContext<T>
    {
    	protected Future<NodeState> internalcall;
    	protected List<IFuture<NodeState>> results;

		public Future<NodeState> getInternalCall() 
		{
			return internalcall;
		}

		public void setInternalCall(Future<NodeState> internalcall) 
		{
			//System.out.println("usercall: "+usercall+" "+this);
			this.internalcall = internalcall;
		}

		public List<IFuture<NodeState>> getResults() 
		{
			return results;
		}

		public void setResults(List<IFuture<NodeState>> results) 
		{
			this.results = results;
		}
    }
}

