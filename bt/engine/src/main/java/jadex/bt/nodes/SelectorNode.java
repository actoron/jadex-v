package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.Map;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.AbortMode;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Execute in order. Succeeds when first child succeeds. Fails
 *  when all children fail.
 *  Also called fallback node.
 */
public class SelectorNode<T> extends CompositeNode<T> 
{
	public SelectorNode()
	{
	}
	
	public SelectorNode(String name)
	{
		super(name);
	}
	
    @Override
    public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> context) 
    {  	
    	Future<NodeState> ret = new Future<>();
    	
    	executeNextChild(event, ret, context);
    
    	return ret;
    }
    
    protected void executeNextChild(Event event, Future<NodeState> ret, ExecutionContext<T> context)
    {
   		incIndex(context);
    	 
    	if(getIndex(context) < getChildCount(context)) 
    	{
    		//System.out.println("Selector exeuting child: "+this+" "+getNodeContext(context).getIndex()+" "+getChild(getNodeContext(context).getIndex()));
      		getLogger().log(Level.INFO, "Selector exeuting child: "+this+" "+getIndex(context)+" "+getChild(getIndex(context), context));

       		//if(getChild(getNodeContext(context).getIndex()).toString().indexOf("collect")!=-1)
    		//	System.out.println("collectwaste");
      		
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
    	else
    	{
    		System.out.println("selector failed: "+this);
    		ret.setResult(NodeState.FAILED);
    	}
    }
    
    protected void handleResult(Event event, NodeState state, Future<NodeState> ret, ExecutionContext<T> context) 
    {
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
			if(state==NodeState.SUCCEEDED)
			{
				//System.out.println("selected succeeded: "+this);
				getLogger().log(Level.INFO, "selected succeeded: "+this);
				ret.setResult(NodeState.SUCCEEDED);
			}
			else if(state==NodeState.FAILED)
			{
				if(getNodeContext(context).getAborted()!=null)
				{
					//System.out.println("ignoring abort return of children: "+this+" "+state);
					getLogger().log(Level.INFO, "ignoring abort return of children: "+this+" "+state);
					ret.setResult(NodeState.FAILED);
				}
				else
				{
					executeNextChild(event, ret, context);
				}
			}
			else
			{
				getLogger().log(java.lang.System.Logger.Level.WARNING, "received non final state: "+state);
			}
		}
    }
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	setIndex(-1, context);
    	//System.out.println("selector node after reset = " + getNodeContext(context).getIndex()+" "+this);
      	System.getLogger(SelectorNode.class.getName()).log(Level.INFO, "selector node after reset = " + getIndex(context)+" "+this);
    }
    
    public int getCurrentChildCount(ExecutionContext<T> context)
    {
    	return getIndex(context);
    }
    
    /*@Override
    public NodeContext<T> copyNodeContext(NodeContext<T> src)
    {
    	SelectorNodeContext<T> ret = (SelectorNodeContext<T>)super.copyNodeContext(src);
    	ret.setIndex(((SelectorNodeContext<T>)src).getIndex());
		return ret;
    } */ 

	
}