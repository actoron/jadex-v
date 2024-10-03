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
		//System.out.println("after rem deco: "+getNode());
		if(state == NodeState.SUCCEEDED || state == NodeState.FAILED) 
			((CompositeNode<T>)getNode().getParent()).removeChild(this.getNode());
		return null;
	}
}