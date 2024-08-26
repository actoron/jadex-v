package jadex.bt;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import jadex.bt.Node.NodeState;
import jadex.common.SReflect;
import jadex.future.Future;
import jadex.future.IFuture;

public class Decorator<T> 
{
	public static AtomicInteger idgen = new AtomicInteger();
	
	protected int id = idgen.incrementAndGet();
	
	protected BiFunction<Event, NodeState, IFuture<NodeState>> execute;
	
	public Decorator()
	{
	}

	public Decorator<T> setFuntion(BiFunction<Event, NodeState, IFuture<NodeState>> execute)
	{
		this.execute = execute;
		return this;
	}
	
	public Decorator<T> setFunction(BiFunction<Event, NodeState, NodeState> execute) 
	{
		this.execute = (event, state) -> new Future<>(execute.apply(event, state));
	    return this;
	}
	
	public IFuture<NodeState> execute(Node<T> node, Event event, NodeState result, ExecutionContext<T> Context)
	{
		return execute.apply(event, result);
	}
	
	@Override
	public String toString() 
	{
		return "AfterDecorator [id=" + id + ", class="+SReflect.getUnqualifiedClassName(getClass())+"]";
	}
}
