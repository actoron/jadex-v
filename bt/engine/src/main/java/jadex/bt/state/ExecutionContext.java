package jadex.bt.state;

import java.util.HashMap;
import java.util.Map;

import jadex.bt.impl.ITimerCreator;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.future.IFuture;

public class ExecutionContext<T> 
{	
	protected Map<Node<T>, NodeContext<T>> nodestates = new HashMap<Node<T>, NodeContext<T>>();

	protected T context;
	
	protected IFuture<NodeState> rootcall;
	
	protected ITimerCreator<T> timercreator;
	
	public NodeContext<T> getNodeContext(Node<T> node)
	{
		return nodestates.get(node);
	}
	
	public void setNodeContext(Node<T> node, NodeContext<T> state)
	{
		nodestates.put(node, state);
	}
	
	public T getUserContext() 
	{
		return context;
	}

	public ExecutionContext<T> setUserContext(T context) 
	{
		this.context = context;
		return this;
	}
	
	public IFuture<NodeState> getRootCall() 
	{
		return rootcall;
	}

	public void setRootCall(IFuture<NodeState> rootcall) 
	{
		this.rootcall = rootcall;
	}

	public ITimerCreator<T> getTimerCreator() 
	{
		return timercreator;
	}

	public ExecutionContext<T> setTimerCreator(ITimerCreator<T> timercreator) 
	{
		this.timercreator = timercreator;
		return this;
	}
}
