package jadex.bt.actions;

import java.util.function.BiFunction;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node.NodeState;
import jadex.future.IFuture;

@Deprecated
public class UserAction<T> extends UserBaseAction<T, IFuture<NodeState>>
{ 
	public UserAction(BiFunction<Event, T, IFuture<NodeState>> action)
	{
		super(action);
	}
	
	/*public UserAction(BiFunction<Event, T, IFuture<NodeState>> action, String description)
	{
		super(action, description);
	}*/
}
