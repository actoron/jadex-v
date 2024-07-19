package jadex.bt;

import jadex.future.Future;
import jadex.future.IFuture;

/**
 * Execute nodes sequentially until all succeed or one fails.
 */
public class SequenceNode<T> extends CompositeNode<T>
{
	protected int idx = 0;
	
    @Override
    public Future<NodeState> internalExecute(Event event, T context) 
    {
      	Future<NodeState> ret = new Future<>();
    	
      	if(idx<getChildCount())
			executeNextChild(event, ret, idx);
		else
			ret.setResult(NodeState.SUCCEEDED);
      	
    	return ret;
    }
    
    protected void executeNextChild(Event event, Future<NodeState> ret, int i)
    {
    	if(i < getChildCount()) 
    	{
            IFuture<NodeState> child = getChild(i).execute(event);
            
            if(child.isDone())
            {
            	handleResult(event, child.get(), ret);
            }
            else
            {
            	child.then(res -> handleResult(event, res, ret)).catchEx(ex -> handleResult(event, NodeState.FAILED, ret));
            }
        }
    	else
    	{
    		ret.setResult(NodeState.FAILED);
    	}
    }
    
    protected void handleResult(Event event, NodeState state, Future<NodeState> ret) 
    {
    	if(state==NodeState.FAILED)
    	{
    		ret.setResult(NodeState.FAILED);
    	}
    	else if(state==NodeState.SUCCEEDED)
    	{
    		if(++idx<getChildCount())
    			executeNextChild(event, ret, idx);
    		else
    			ret.setResult(NodeState.SUCCEEDED);
    	}
    } 
    
    @Override
    protected void reset() 
    {
    	super.reset();
    	idx = 0;
    }
    
    public int getCurrentChildCount()
    {
    	return idx;
    }
}
