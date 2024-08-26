package jadex.bt;

import java.util.function.BiFunction;

import jadex.bt.Node.NodeState;
import jadex.future.IFuture;

public class UserAction<T> extends UserBaseAction<T, IFuture<NodeState>>
{ 
	public UserAction(BiFunction<Event, T, IFuture<NodeState>> action)
	{
		this(action, null);
	}
	
	public UserAction(BiFunction<Event, T, IFuture<NodeState>> action, String description)
	{
		super(action, description);
	}
}
