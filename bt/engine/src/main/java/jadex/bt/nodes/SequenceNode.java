package jadex.bt.nodes;

import java.lang.System.Logger.Level;

import jadex.bt.decorators.Decorator;
import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 * Execute nodes sequentially until all succeed or one fails.
 */
public class SequenceNode<T> extends CompositeNode<T>
{
	public SequenceNode()
	{
	}
	
	public SequenceNode(String name)
	{
		super(name);
	}
	
    @Override
    public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> context) 
    {
    	//Thread.dumpStack();
    	//System.out.println("seq node, internal exe: "+this+" "+state+" "+getNodeContext(context).getIndex());
    	return executeNextChild(event, context);
    }
    
    protected IFuture<NodeState> executeNextChild(Event event, ExecutionContext<T> context)
    {
    	Future<NodeState> ret = new Future<>();
    	
    	getNodeContext(context).incIndex();
    	//System.out.println("sequence executing child: "+getNodeContext(context).getIndex());
  		System.getLogger(this.getClass().getName()).log(Level.INFO, "sequence executing child: "+this+" "+getNodeContext(context).getIndex());

    	
    	if(getNodeContext(context).getIndex() < getChildCount()) 
    	{
            System.getLogger(this.getClass().getName()).log(Level.INFO, "sequence child is: "+getChild(getNodeContext(context).getIndex()));
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
    		System.out.println("sequence succeeded: "+this);
    		ret.setResult(NodeState.SUCCEEDED);
    	}
    	else
    	{
    		System.out.println("sequence failed: "+this);
    		ret.setResult(NodeState.FAILED);
    	}
    	
    	return ret;
    }
    
    protected void handleResult(Event event, NodeState state, Future<NodeState> ret, ExecutionContext<T> context) 
    {
    	System.out.println("seq node, child finished with: "+this+" "+state);
    	
    	if(state==NodeState.FAILED)
    	{
    		if(this.toString().indexOf("load")!=-1 || toString().indexOf("findst")!=-1)
    			System.out.println("seq node child failed: "+state);

    		ret.setResult(NodeState.FAILED);
    	}
    	else if(state==NodeState.SUCCEEDED)
    	{
    		System.out.println("seq node, child success, next: "+this+" "+state);
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
    public NodeContext<T> copyNodeContext(NodeContext<T> src)
    {
    	SequenceNodeContext<T> ret = (SequenceNodeContext<T>)super.copyNodeContext(src);
    	ret.setIndex(((SequenceNodeContext<T>)src).getIndex());
		return ret;
    }
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	getNodeContext(context).setIndex(-1);
    	System.out.println("sequence node after reset = " + getNodeContext(context).getIndex()+" "+this);
    }
    
    public static class SequenceNodeContext<T> extends NodeContext<T> implements IIndexContext
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
