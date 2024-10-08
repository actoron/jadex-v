package jadex.bt.nodes;

import java.lang.System.Logger.Level;

import jadex.bt.actions.UserBaseAction;
import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.future.Future;
import jadex.future.FutureTerminatedException;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

public class ActionNode<T> extends Node<T> 
{
    //protected BiFunction<Event, T, IFuture<NodeState>> action;
    protected UserBaseAction<T, ? extends IFuture<NodeState>> action;
    
    public ActionNode()
    {
    }
    
    public ActionNode(String name)
    {
    	super(name);
    }
    
    //public ActionNode(BiFunction<Event, T, IFuture<NodeState>> action)
    public ActionNode(UserBaseAction<T, ? extends IFuture<NodeState>> action)
    {
    	this.action = action;
    }
    
    public UserBaseAction<T, ? extends IFuture<NodeState>> getAction() 
    {
		return action;
	}

	public ActionNode<T> setAction(UserBaseAction<T, ? extends IFuture<NodeState>> action)
    {
		//System.out.println("action is: "+this+" "+action);
    	this.action = action;
    	return this;
    }

    @Override
    public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> context) 
    {
		System.getLogger(this.getClass().getName()).log(Level.INFO, "exeuting action node: "+this);
  		//System.out.println("exeuting action node: "+this);
    	
    	Future<NodeState> ret = new Future<>();
      	
    	try
    	{
			IFuture<NodeState> fut = action.getAction().apply(event, context.getUserContext());
			getNodeContext(context).setUsercall(fut);
			
			fut.then(res ->
			{
				ret.setResultIfUndone(res);
			}).catchEx(e ->
			{
		   		if(!(e instanceof FutureTerminatedException))
		   		{
		   			//System.out.println("exception in action: "+e);
		   			System.getLogger(this.getClass().getName()).log(Level.ERROR, "exception in action: "+e);
		   		}
		   		ret.setResultIfUndone(NodeState.FAILED);
			});
    	}
    	catch(Exception e)
    	{
     		//System.out.println("exception in action: "+e);
     		System.getLogger(this.getClass().getName()).log(Level.ERROR, "exception in action: "+e);
    		ret.setResultIfUndone(NodeState.FAILED);
    	}
		
		return ret;
    }
    
    @Override
    public void abort(AbortMode abortmode, NodeState state, ExecutionContext<T> execontext) 
    {
    	ActionNodeContext<T> context = getNodeContext(execontext);
    	
     	if(context.getAborted()!=null || NodeState.RUNNING!=context.getState())
    		return;
 
     	IFuture<NodeState> usercall = context.getUsercall();
     	
      	super.abort(abortmode, state, execontext);

    	if(abortmode==AbortMode.SELF)
    	{
    		if(usercall==null)
    		{
    			//System.out.println("abort: no user action: "+this);
    			System.getLogger(this.getClass().getName()).log(Level.INFO, "abort: no user action: "+this);
    		}
    		else
    		{
    			if(usercall.isDone())
        		{
        			//System.out.println("abort: user action finished: "+this);
        			System.getLogger(this.getClass().getName()).log(Level.INFO, "abort: user action finished: "+this);
        		}
    			else if(usercall instanceof ITerminableFuture)
    			{
    				((ITerminableFuture<NodeState>)usercall).terminate();
    				//System.out.println("abort: aborting user action: "+this);
    				System.getLogger(this.getClass().getName()).log(Level.INFO, "abort: aborting user action: "+this);
    			}
    			else
    			{
    				//System.out.println("abort: cannot abort non-terminable action "+this);
    				System.getLogger(this.getClass().getName()).log(Level.INFO, "abort: cannot abort non-terminable action "+this);
    			}
    		}
    	}
    	else
    	{
    		//System.out.println("ignoring abort: "+abortmode+" "+this);
			System.getLogger(this.getClass().getName()).log(Level.INFO, "ignoring abort: "+abortmode+" "+this);
    	}
    }
    
    @Override
    public void reset(ExecutionContext<T> context, Boolean all) 
    {
    	super.reset(context, all);
    	//System.out.println("removed call: "+getNodeContext(context).getUsercall());
    	getNodeContext(context).setUsercall(null);
    }
    
    @Override
    public ActionNodeContext<T> getNodeContext(ExecutionContext<T> execontext) 
    {
    	return (ActionNodeContext<T>)super.getNodeContext(execontext);
    }
    
    protected NodeContext<T> createNodeContext()
    {
    	return new ActionNodeContext<T>();
    }
    
    public static class ActionNodeContext<T> extends NodeContext<T>
    {
    	protected IFuture<NodeState> usercall;

		public IFuture<NodeState> getUsercall() 
		{
			return usercall;
		}

		public void setUsercall(IFuture<NodeState> usercall) 
		{
			//System.out.println("usercall: "+usercall+" "+this);
			this.usercall = usercall;
		}
    }
    
	/*@Override
	public String toString() 
	{
		String ret = "ActionNode [id=" + id + ", action="+action+"]";
		//System.out.println("a: "+ret);
		return ret;
	}*/
}
