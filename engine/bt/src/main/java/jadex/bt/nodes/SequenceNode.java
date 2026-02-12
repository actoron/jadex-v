package jadex.bt.nodes;

import java.lang.System.Logger.Level;

import jadex.bt.IChildTraversalStrategy;
import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Execute nodes sequentially until all succeed or one fails.
 *  Traversal order is fully delegated to IChildTraversalStrategy.
 */
public class SequenceNode<T> extends CompositeNode<T>
{
    public SequenceNode()
    {
        this(null);
    }

    public SequenceNode(String name)
    {
        this(name, null);
    }

    public SequenceNode(String name, IChildTraversalStrategy<T> strategy)
    {
        super(name, strategy);
    }

    @Override
    public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> context)
    {
        // Initialize traversal state (remaining children etc.)
        getStrategy().init(this, context);

        return executeNextChild(event, context);
    }

    protected IFuture<NodeState> executeNextChild(Event event, ExecutionContext<T> context)
    {
        Future<NodeState> ret = new Future<>();

        Node<T> child = getStrategy().nextChild(this, context);

        if(child == null)
        {
            getLogger().log(Level.INFO, "sequence succeeded: " + this);
            ret.setResult(NodeState.SUCCEEDED);
            return ret;
        }

        getLogger().log(Level.INFO, "sequence executing child: " + this + " -> " + child);

        IFuture<NodeState> fut = child.execute(event, context);

        if(fut.isDone())
        {
            handleResult(event, fut.get(), ret, context);
        }
        else
        {
            fut.then(res -> handleResult(event, res, ret, context))
               .catchEx(ex -> handleResult(event, NodeState.FAILED, ret, context));
        }

        return ret;
    }

    protected void handleResult(Event event, NodeState childState, 
		Future<NodeState> ret, ExecutionContext<T> context)
    {
        if(ret.isDone())
            return;

        if(context.getNodeContext(this).getAbortState() != null)
        {
            ret.setResultIfUndone(
                context.getNodeContext(this).getAbortState());
            return;
        }

        if(childState == NodeState.FAILED)
        {
            getLogger().log(Level.INFO, "sequence failed: " + this);
            ret.setResult(NodeState.FAILED);
        }
        else if(childState == NodeState.SUCCEEDED)
        {
            executeNextChild(event, context).delegateTo(ret);
        }
    }

    @Override
    public void reset(ExecutionContext<T> context, Boolean all)
    {
        super.reset(context, all);
        getStrategy().reset(this, context);
    }
}
