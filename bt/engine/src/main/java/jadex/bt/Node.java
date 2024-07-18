package jadex.bt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

import jadex.future.Future;
import jadex.future.IFuture;

public abstract class Node
{
	public enum NodeState 
	{
	    SUCCEEDED,
	    FAILED,
	    RUNNING
	}

	public enum AbortMode 
	{
	    NONE,
	    SELF,
	    SUBTREE,
	    LOW_PRIORITY
	}
	
	protected Node parent;
	protected AbortMode abortmode = AbortMode.SUBTREE;
	//protected NodeState state = NodeState.IDLE;
	protected Future<NodeState> curaction;
	protected Blackboard blackboard;
	protected List<Decorator> beforedecos = new ArrayList<>();
	protected List<Decorator> afterdecos = new ArrayList<>();
	protected boolean aborted = false;
	
	public abstract IFuture<NodeState> internalExecute(Event event);
	
	public Node setParent(Node parent)
	{
		this.parent = parent;
		return this;
	}
	
	public Node setBlackboard(Blackboard blackboard)
	{
		this.blackboard = blackboard;
		return this;
	}
	
	public Node setAbortMode(AbortMode abortmode)
	{
		this.abortmode = abortmode;
		return this;
	}
	
	public Node getParent()
	{
		return parent;
	}
	
    /*public NodeState getState() 
    {
        return state;
    }*/
    
    public Blackboard getBlackboard() 
    {
        if(blackboard != null) 
        {
            return blackboard;
        } 
        else if (parent != null) 
        {
            return parent.getBlackboard();
        } 
        else 
        {
            return null;
        }
    }
    
    public AbortMode getAbortMode()
    {
    	return abortmode;
    }
    
	public void addBeforeDecorator(Decorator deco)
	{
		if(!beforedecos.contains(deco))
			beforedecos.add(deco);
		else
			System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "decorator already contained: "+deco);
	}
	
	public void addAfterDecorator(Decorator deco)
	{
		if(!afterdecos.contains(deco))
			afterdecos.add(deco);
		else
			System.getLogger(this.getClass().getName()).log(java.lang.System.Logger.Level.WARNING, "decorator already contained: "+deco);
	}
	
	protected IFuture<NodeState> execute(Event event) 
	{
	   	if(curaction!=null)
    		return curaction;
		
		Future<NodeState> ret = new Future<>();
        curaction = ret;
		
		// Decorators are always executed independent of the node state
        executeDecorators(beforedecos, 0, event, NodeState.RUNNING, s -> s!=NodeState.RUNNING).then(bs ->
        {
        	if(bs != NodeState.RUNNING) 
      	    {
        		reset();
              	ret.setResult(bs);
      	    }
        	else
        	{
        		IFuture<NodeState> fut = internalExecute(event);
                 
    	        fut.then(state -> 
    	        {
    	        	executeDecorators(afterdecos, 0, event, state, null).then(as ->
    	        	{
    	        		reset();
        	        	ret.setResultIfUndone(as);
    	        	})
    	        	.catchEx(e -> e.printStackTrace());
    	        	
    	        }).catchEx(ex -> 
    	        {
    	        	executeDecorators(afterdecos, 0, event, NodeState.FAILED, null).then(as ->
    	        	{
    	        		reset();
        	        	ret.setResultIfUndone(as);
    	        	})
    	        	.catchEx(e -> e.printStackTrace());
    	        });
        	}
        }).catchEx(e ->
        {
        	// Should not happen exceptions in decorators are handled in method
        	reset();
        	e.printStackTrace();
        });
        
        return ret;
	}
	
	protected IFuture<NodeState> executeDecorators(List<Decorator> decos, int i, Event event, NodeState state, Predicate<NodeState> abort)
	{
		Future<NodeState> ret = new Future<>();
		
		if(abort!=null && abort.test(state))
		{
			ret.setResult(state);
		}
		else
		{
			Decorator deco = i<beforedecos.size()? beforedecos.get(i): null;
			
			if(deco!=null)
			{
				try
				{
					deco.execute(this, event, state).then(s ->
					{
						executeDecorators(decos, i+1, event, s, abort).delegateTo(ret);
					}).catchEx(e ->
					{
						e.printStackTrace();
						executeDecorators(decos, i+1, event, state, abort).delegateTo(ret); // ignore decorator exception?! 
					});
				}
				catch(Exception e)
				{
					e.printStackTrace();
					executeDecorators(decos, i+1, event, state, abort).delegateTo(ret);
				}
			}
			else
			{
				ret.setResult(state);
			}
		}
		
		return ret;
	}

    public void abort() 
    {
    	if(aborted)
    		return;
    	
    	aborted = true;
    	if(curaction!=null)
    		curaction.setResult(NodeState.FAILED);
		//if(curaction!=null)
    	//	((ITerminableFuture<NodeState>)curaction).terminate();
    }
    
    protected void reset()
    {
    	this.curaction = null;
    	//this.state = NodeState.IDLE;
    	this.aborted = false;
    }
}
