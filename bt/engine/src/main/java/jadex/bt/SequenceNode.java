package jadex.bt;

import jadex.future.Future;
import jadex.future.IFuture;

/**
 * Execute nodes sequentially until all succeed or one fails.
 */
public class SequenceNode<T> extends CompositeNode<T>
{
    @Override
    public IFuture<NodeState> internalExecute(Event event, ExecutionContext<T> context) 
    {
    	return executeNextChild(event, context);
    }
    
    protected IFuture<NodeState> executeNextChild(Event event, ExecutionContext<T> context)
    {
    	Future<NodeState> ret = new Future<>();
    	
    	getNodeContext(context).incIndex();
    	System.out.println("sequence executing child: "+getNodeContext(context).getIndex());
    	
    	if(getNodeContext(context).getIndex() < getChildCount()) 
    	{
            IFuture<NodeState> child = getChild(getNodeContext(context).getIndex()).execute(event, context);
            
            if(child.isDone())
            {
            	handleResult(event, child.get(), ret, context);
            }
            else
            {
            	child.then(res -> handleResult(event, res, ret, context)).catchEx(ex -> handleResult(event, NodeState.FAILED, ret, context));
            }
        }
    	else if(getChildCount()==0 || getChildCount()==getNodeContext(context).getIndex())
    	{
    		ret.setResult(NodeState.SUCCEEDED);
    	}
    	else
    	{
    		ret.setResult(NodeState.FAILED);
    	}
    	
    	return ret;
    }
    
    protected void handleResult(Event event, NodeState state, Future<NodeState> ret, ExecutionContext<T> context) 
    {
    	//System.out.println("hadleRes: "+state);
    	if(state==NodeState.FAILED)
    	{
    		ret.setResult(NodeState.FAILED);
    	}
    	else if(state==NodeState.SUCCEEDED)
    	{
    		executeNextChild(event, context).delegateTo(ret);
    	}
    } 
    
    public int getCurrentChildCount(ExecutionContext<T> context)
    {
    	return getNodeContext(context).getIndex();
    }
    
    @Override
    public SequenceNodeContext<T> getNodeContext(ExecutionContext<T> execontext) 
    {
    	return (SequenceNodeContext<T>)super.getNodeContext(execontext);
    }
    
    @Override
    protected NodeContext<T> createNodeContext() 
    {
    	return new SequenceNodeContext<T>();
    }
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	getNodeContext(context).setIndex(-1);
    }
    
    public static class SequenceNodeContext<T> extends NodeContext<T>
    {
    	protected int idx = -1;

		public int getIndex() 
		{
			return idx;
		}

		public void setIndex(int idx) 
		{
			this.idx = idx;
		}
		
		public void incIndex()
		{
			idx++;
		}
    }
}
