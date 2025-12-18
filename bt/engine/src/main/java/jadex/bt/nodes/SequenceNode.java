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
	private static final String KEY_INDEX = "index";

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
		setIndex(-1, context);
    	return executeNextChild(event, context);
    }
    
    protected IFuture<NodeState> executeNextChild(Event event, ExecutionContext<T> context)
    {
    	Future<NodeState> ret = new Future<>();
    	
    	incIndex(context);
    	//System.out.println("sequence executing child: "+getNodeContext(context).getIndex());
  		getLogger().log(Level.INFO, "sequence executing child: "+this+" "+getIndex(context));

    	
    	if(getIndex(context) < getChildCount(context)) 
    	{
            getLogger().log(Level.INFO, "sequence child is: "+getChild(getIndex(context), context));
            IFuture<NodeState> child = getChild(getIndex(context), context).execute(event, context);
            
            if(child.isDone())
            {
            	handleResult(event, child.get(), ret, context);
            }
            else
            {
            	child.then(res -> handleResult(event, res, ret, context)).catchEx(ex -> handleResult(event, NodeState.FAILED, ret, context));
            }
        }
    	else if(getChildCount(context)==0 || getChildCount(context)==getIndex(context))
    	{
    		//System.out.println("sequence succeeded: "+this);
     		getLogger().log(Level.INFO, "sequence succeeded: "+this);
    		ret.setResult(NodeState.SUCCEEDED);
    	}
    	else
    	{
    		//System.out.println("sequence failed: "+this);
    		getLogger().log(Level.INFO, "sequence failed: "+this);
    		ret.setResult(NodeState.FAILED);
    	}
    	
    	return ret;
    }
    
    protected void handleResult(Event event, NodeState state, Future<NodeState> ret, ExecutionContext<T> context) 
    {
    	//System.out.println("seq node, child finished with: "+this+" "+state);
    	getLogger().log(Level.INFO, "seq node, child finished with: "+this+" "+state);

		if(ret.isDone())
		{
    		return;
		}
		else if(context.getNodeContext(this).getAbortState()!=null)
		{
			System.out.println("seq node, abort state detected: "+this+" "+context.getNodeContext(this).getAbortState()	);
			ret.setResultIfUndone(context.getNodeContext(this).getAbortState());
		}
		else
		{
			if(state==NodeState.FAILED)
			{
				//if(this.toString().indexOf("load")!=-1 || toString().indexOf("findst")!=-1)
					System.out.println("seq node child failed: "+state);

				ret.setResult(NodeState.FAILED);
			}
			else if(state==NodeState.SUCCEEDED)
			{
				System.out.println("seq node, child success, next: "+this+" "+state);
				getLogger().log(Level.INFO, "seq node, child success, next: "+this+" "+state);
				executeNextChild(event, context).delegateTo(ret);
			}
		}
    } 
    
    public int getCurrentChildCount(ExecutionContext<T> context)
    {
    	return getIndex(context);
    }
    
    /*@Override
    public NodeContext<T> copyNodeContext(NodeContext<T> src)
    {
    	SequenceNodeContext<T> ret = (SequenceNodeContext<T>)super.copyNodeContext(src);
    	ret.setIndex(((SequenceNodeContext<T>)src).getIndex());
		return ret;
    }*/
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	setIndex(-1, context);
    	//System.out.println("sequence node after reset = " + getNodeContext(context).getIndex()+" "+this);
    	System.getLogger(SequenceNode.class.getName()).log(Level.INFO, "sequence node after reset = " + getIndex(context)+" "+this);
    }

	public int getIndex(ExecutionContext<T> context) 
	{
	    Integer val = (Integer) context.getNodeContext(this).getValue("index");
	    return val != null ? val : -1;
	}

	public void setIndex(int idx, ExecutionContext<T> context) 
	{
		context.getNodeContext(this).setValue(KEY_INDEX, idx);	
	}
	
	public void incIndex(ExecutionContext<T> context)
	{
		Integer idx = getIndex(context);
		setIndex(idx+1, context);
	}
}
