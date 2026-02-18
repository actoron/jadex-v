package jadex.bt.actions;

import java.util.function.BiFunction;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.future.IFuture;

public class UserBaseAction<T, F extends IFuture<NodeState>>
{ 
	protected BiFunction<Event, T, F> action; 
	//protected String description;
	
	public UserBaseAction(BiFunction<Event, T, F> action)
	{
		this.action = action;
	}
	
	public UserBaseAction(BiFunction<Event, T, F> action, String description)
	{
		this.action = action;
		//this.description = description;
	}
	
	public BiFunction<Event, T, F> getAction() 
	{
		return action;
	}

	public void setAction(BiFunction<Event, T, F> action) 
	{
		this.action = action;
	}

	public Class<? extends IFuture<NodeState>> getFutureReturnType() 
	{
		return (Class<? extends IFuture<NodeState>>)(Class<?>)IFuture.class;
	}
	
	/*public String getDescription()
	{
		return description;
	}*/
}
