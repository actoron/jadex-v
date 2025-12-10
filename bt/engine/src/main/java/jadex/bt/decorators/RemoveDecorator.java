package jadex.bt.decorators;

import jadex.bt.impl.Event;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.future.IFuture;

public class RemoveDecorator<T> extends Decorator<T> 
{
	/*@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		System.out.println("before rem deco: "+getNode());
		return super.beforeExecute(event, state, context);
	}*/
	
	@Override
	public IFuture<NodeState> afterExecute(Event event, NodeState state, ExecutionContext<T> context)
	{
		//System.out.println("after rem deco: "+getNode()+" "+hashCode()+" "+state+" "+event);
		if(state == NodeState.SUCCEEDED || state == NodeState.FAILED) 
		{
			CompositeNode<T> parent = (CompositeNode<T>)getNode().getParent();
			if(parent!=null)
			{
				parent.removeChild(this.getNode(), context);
			}
			else
			{
				// Multiple invocations possible due to reexecute of a node.
				//System.out.println("no parent to remove from: "+getNode()+" "+hashCode());
			}
		}
		return null;
	}
}