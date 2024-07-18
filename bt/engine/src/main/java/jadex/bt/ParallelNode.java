package jadex.bt;

import java.util.ArrayList;
import java.util.List;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;
import jadex.future.TerminableFuture;

/**
 * Execute nodes sequentially until all succeed or one fails.
 */
public class ParallelNode extends CompositeNode
{
	public enum ResultMode
	{
		ON_ONE,
		ON_ALL
	}
	
	protected ResultMode failmode = ResultMode.ON_ONE;
	protected ResultMode successmode = ResultMode.ON_ALL;
	
	/*public SequenceNode(Node parent, Blackboard blackboard, AbortMode abortmode)
	{
		super(parent, blackboard, abortmode);
	}*/
	
	public ParallelNode setFailureMode(ResultMode failmode)
	{
		this.failmode = failmode;
		return this;
	}
	
	public ParallelNode setSuccessMode(ResultMode successmode)
	{
		this.successmode = successmode;
		return this;
	}
	
    public IFuture<NodeState> internalExecute(Event event) 
    {
        List<IFuture<NodeState>> results = new ArrayList<>();
    	Future<NodeState> ret = new Future<>();
    	//curaction = ret;
    	//this.state = NodeState.RUNNING;
    	
        for(int i=0; i<getChildCount(); i++) 
        {
            IFuture<NodeState> child = getChild(i).execute(event);
            results.add(child);
            
            if(child.isDone())
            {
            	NodeState state = child.get();
            	if(checkFastExit(state))
            	{
            		handleFinish(state, ret, results);
            		break;
            	}
            	
            	state = checkFinished(results);
            	if(state!=null)
            	{
            		handleFinish(state, ret, results);
            		break;
            	}
            }
            
            child.then(state -> handleResult(state, results, ret))
            	.catchEx(ex -> handleResult(NodeState.FAILED, results, ret));
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
    
    protected void handleFinish(NodeState state, Future<NodeState> ret, List<IFuture<NodeState>> results)
    {
    	if(ret.isDone())
    		return;
    	
    	for(int i=0; i<results.size(); i++) 
    	{
    		if(!results.get(i).isDone())
    		{
    			getChild(i).abort();
    		}
    	}
    	
		//this.state = state;
   		ret.setResultIfUndone(state);
    }
    
    protected void handleResult(NodeState state, List<IFuture<NodeState>> results, Future<NodeState> ret) 
    {
        if(checkFastExit(state)) 
        {
            handleFinish(state, ret, results);
        } 
        else 
        {
            NodeState finstate = checkFinished(results);
            if(finstate != null)
                handleFinish(finstate, ret, results);
        }
    }
    
    public int getCurrentChildCount()
    {
    	return getChildCount();
    }
}

