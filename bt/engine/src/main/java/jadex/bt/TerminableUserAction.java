package jadex.bt;

import java.util.function.BiFunction;

import jadex.bt.Node.NodeState;
import jadex.future.IFuture;
import jadex.future.ITerminableFuture;

public class TerminableUserAction<T> extends UserBaseAction<T, ITerminableFuture<NodeState>> 
{
	public TerminableUserAction(BiFunction<Event, T, ITerminableFuture<NodeState>> action)
	{
		this(action, null);
	}
	
	public TerminableUserAction(BiFunction<Event, T, ITerminableFuture<NodeState>> action, String description)
	{
		super(action, description);
	}
	
	public Class<? extends IFuture<NodeState>> getFutureReturnType() 
	{
		return (Class<? extends IFuture<NodeState>>)(Class<?>)ITerminableFuture.class;
	}
}
