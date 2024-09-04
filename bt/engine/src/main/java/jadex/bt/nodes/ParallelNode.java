package jadex.bt.nodes;

import java.util.ArrayList;
import java.util.List;

import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
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
	
    public IFuture<NodeState> internalExecute(Event event, NodeState mystate, ExecutionContext<T> context) 
    {
        List<IFuture<NodeState>> results = new ArrayList<>();
    	Future<NodeState> ret = new Future<>();
    	//curaction = ret;
    	//this.state = NodeState.RUNNING;
    	
        for(int i=0; i<getChildCount(); i++) 
        {
            IFuture<NodeState> child = getChild(i).execute(event, context);
            results.add(child);
            
            if(child.isDone())
            {
            	NodeState state = child.get();
            	if(checkFastExit(state))
            	{
            		handleFinish(state, ret, results, context);
            		break;
            	}
            	
            	state = checkFinished(results);
            	if(state!=null)
            	{
            		handleFinish(state, ret, results, context);
            		break;
            	}
            }
            
            child.then(state -> handleResult(state, results, ret, context))
            	.catchEx(ex -> handleResult(NodeState.FAILED, results, ret, context));
        }
        
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
}
