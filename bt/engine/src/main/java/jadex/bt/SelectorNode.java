package jadex.bt;

import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Execute in order. Succeeds when first child succeeds. Fails
 *  when all children fail.
 *  Also called fallback node.
 */
public class SelectorNode<T> extends CompositeNode<T> 
{
    @Override
    public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> context) 
    {  	
    	Future<NodeState> ret = new Future<>();
    	
    	executeNextChild(event, ret, context);
    
    	return ret;
    }
    
    protected void executeNextChild(Event event, Future<NodeState> ret, ExecutionContext<T> context)
    {
   		getNodeContext(context).incIndex();
    	 
    	if(getNodeContext(context).getIndex() < getChildCount()) 
    	{
    		System.out.println("Selector exeuting child: "+this+" "+getNodeContext(context).getIndex()+" "+getChild(getNodeContext(context).getIndex()));
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
    	else
    	{
    		ret.setResult(NodeState.FAILED);
    	}
    }
    
    protected void handleResult(Event event, NodeState state, Future<NodeState> ret, ExecutionContext<T> context) 
    {
    	if(state==NodeState.SUCCEEDED)
    	{
    		ret.setResult(NodeState.SUCCEEDED);
    	}
    	else if(state==NodeState.FAILED)
    	{
    		executeNextChild(event, ret, context);
    	}
    	else
    	{
    		System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "received non final state: "+state);
    	}
    }
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	getNodeContext(context).setIndex(-1);
    }
    
    public int getCurrentChildCount(ExecutionContext<T> context)
    {
    	return getNodeContext(context).getIndex();
    }
    
    @Override
    public SelectorNodeContext<T> getNodeContext(ExecutionContext<T> execontext) 
    {
    	return (SelectorNodeContext<T>)super.getNodeContext(execontext);
    }
    
    protected NodeContext<T> createNodeContext() 
    {
    	return new SelectorNodeContext<T>();
    }
    
    public static class SelectorNodeContext<T> extends NodeContext<T>
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