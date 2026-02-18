package jadex.bt.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jadex.bt.IChildTraversalStrategy;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;

public class RandomChildTraversalStrategy<T> implements IChildTraversalStrategy<T>
{
    private static final String KEY = "remainingchildren";

    private final Random rnd;

    public RandomChildTraversalStrategy(long seed)
    {
        this.rnd = new Random(seed);
    }

    @Override
    public void init(CompositeNode<T> node, ExecutionContext<T> context)
    {
        NodeContext<T> nc = node.getNodeContext(context);

        List<Node<T>> shuffled = new ArrayList<>(node.getChildren(context));
        Collections.shuffle(shuffled, rnd);

        nc.setValue(KEY, shuffled);
    }

    @Override
    public Node<T> nextChild(CompositeNode<T> node, ExecutionContext<T> context)
    {
        NodeContext<T> nc = node.getNodeContext(context);
        List<Node<T>> children = (List<Node<T>>) nc.getValue(KEY);

        return (children != null && !children.isEmpty())
            ? children.remove(0)
            : null;
    }

    @Override
    public void reset(CompositeNode<T> node, ExecutionContext<T> context)
    {
        node.getNodeContext(context).removeValue(KEY);
    }
}
