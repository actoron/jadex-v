package jadex.bt;

import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;

public interface IChildTraversalStrategy<T>
{
    public void init(CompositeNode<T> node, ExecutionContext<T> context);
    
    public Node<T> nextChild(CompositeNode<T> node, ExecutionContext<T> context);
    
    public void reset(CompositeNode<T> node, ExecutionContext<T> context);

    
    default void onChildAdded(CompositeNode<T> node, Node<T> child, ExecutionContext<T> context)
    {
    }

    default void onChildRemoved(CompositeNode<T> node, Node<T> child, ExecutionContext<T> context)
    {
    }
}
