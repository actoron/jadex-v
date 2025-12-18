package jadex.bt.nodes;

import java.util.ArrayList;
import java.util.List;

import jadex.bt.IChildTraversalStrategy;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;

public class DefaultChildTraversalStrategy<T> implements IChildTraversalStrategy<T>
{
    private static final String KEY_REMAININGCHILDREN = "remainingchildren";

    @Override
    public void init(CompositeNode<T> node, ExecutionContext<T> context)
    {
        NodeContext<T> nc = node.getNodeContext(context);

        List<Node<T>> remaining = new ArrayList<>(node.getChildren(context));

        nc.setValue(KEY_REMAININGCHILDREN, remaining);
    }

    @Override
    public Node<T> nextChild(CompositeNode<T> node, ExecutionContext<T> context)
    {
        NodeContext<T> nc = node.getNodeContext(context);

        List<Node<T>> remaining = (List<Node<T>>) nc.getValue(KEY_REMAININGCHILDREN);

        if(remaining == null || remaining.isEmpty())
            return null;

        return remaining.remove(0);
    }

    @Override
    public void reset(CompositeNode<T> node, ExecutionContext<T> context)
    {
        node.getNodeContext(context).removeValue(KEY_REMAININGCHILDREN);
    }
}
