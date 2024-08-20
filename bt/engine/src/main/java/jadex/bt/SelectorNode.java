package jadex.bt;

import java.util.logging.Level;

import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Execute in order. Succeeds when first child succeeds. Fails
 *  when all children fail.
 *  Also called fallback node.
 */
public class SelectorNode<T> extends CompositeNode<T> 
{
	protected int idx = 0;
	
	/*public SelectorNode(Node parent, Blackboard blackboard, AbortMode abortmode)
	{
		super(parent, blackboard, abortmode);
	}*/
	
    @Override
    public IFuture<NodeState> internalExecute(Event event, T context) 
    {  	
    	Future<NodeState> ret = new Future<>();
    	//this.state = NodeState.RUNNING;
    	
    	executeNextChild(event, ret, idx);
    
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
    	if(state==NodeState.SUCCEEDED)
    	{
    		ret.setResult(NodeState.SUCCEEDED);
    	}
    	else if(state==NodeState.FAILED)
    	{
    		executeNextChild(event, ret, ++idx);
    	}
    	else
    	{
    		System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "received non final state: "+state);
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