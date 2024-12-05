package jadex.bt;

import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;

public class NodeListener<T> implements INodeListener<T>
{
	//public void onStateChanged(Node<T> node, NodeState oldstate, NodeState newstate, ExecutionContext<T> context);
	
	public void onSucceeded(Node<T> node, ExecutionContext<T> context)
	{
	}
	
	public void onFailed(Node<T> node, ExecutionContext<T> context)
	{
	}
	
	public void onChildAdded(Node<T> parent, Node<T> child, ExecutionContext<T> context)
	{
	}
	
	public void onChildRemoved(Node<T> parent, Node<T> child, ExecutionContext<T> context)
	{
	}
}
