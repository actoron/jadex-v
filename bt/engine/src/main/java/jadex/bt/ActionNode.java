package jadex.bt;

import java.util.function.BiFunction;

import jadex.future.Future;
import jadex.future.FutureTerminatedException;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

public class ActionNode<T> extends Node<T> 
{
    protected BiFunction<Event, T, IFuture<NodeState>> action;
    protected IFuture<NodeState> icuraction; 
    
    public ActionNode()
    {
    }
    
    public ActionNode(BiFunction<Event, T, IFuture<NodeState>> action)
    {
    	this.action = action;
    }
    
    public ActionNode<T> setAction(BiFunction<Event, T, IFuture<NodeState>> action)
    {
    	this.action = action;
    	return this;
    }

    @Override
    public IFuture<NodeState> internalExecute(Event event, T context) 
    {
    	Future<NodeState> ret = new Future<NodeState>();
      	
    	try
    	{
			icuraction = action.apply(event, context);
			
			icuraction.then(res ->
			{
				ret.setResultIfUndone(res);
			}).catchEx(e ->
			{
		   		if(!(e instanceof FutureTerminatedException))
		   			System.out.println("exception in action: "+e);
		   		ret.setResult(NodeState.FAILED);
			});
    	}
    	catch(Exception e)
    	{
     		System.out.println("exception in action: "+e);
    		ret.setResult(NodeState.FAILED);
    	}
		
		return ret;
    }
    
    @Override
    protected void reset() 
    {
    	super.reset();
    	icuraction = null;
    }
    
    @Override
    public void abort() 
    {
    	super.abort();
    	if(icuraction instanceof ITerminableFuture)
    		((ITerminableFuture<NodeState>)icuraction).terminate();
    }
}
