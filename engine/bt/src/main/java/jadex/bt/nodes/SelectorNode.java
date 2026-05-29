package jadex.bt.nodes;

import java.lang.System.Logger.Level;

import jadex.bt.IChildTraversalStrategy;
import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Execute children in traversal order.
 *  Succeeds when the first child succeeds.
 *  Fails when all children fail.
 *  (Fallback / Selector node)
 */
public class SelectorNode<T> extends CompositeNode<T>
{
    public SelectorNode()
    {
        this(null);
    }

    public SelectorNode(String name)
    {
		this(name, null);
    }

    public SelectorNode(String name, IChildTraversalStrategy<T> strategy)
    {
        super(name, strategy);
    }

    @Override
    public IFuture<NodeState> internalExecute(Event event, NodeState state, ExecutionContext<T> context)
    {
        // initialize traversal state (remaining children etc.)
        getStrategy().init(this, context);

        return executeNextChild(event, context);
    }

    protected IFuture<NodeState> executeNextChild(Event event, ExecutionContext<T> context)
    {
        Future<NodeState> ret = new Future<>();

        Node<T> child = getStrategy().nextChild(this, context);

        if(child == null)
        {
            getLogger().log(Level.INFO, "selector failed: " + this);
            ret.setResult(NodeState.FAILED);
            return ret;
        }

        getLogger().log(Level.INFO, "selector executing child: " + this + " -> " + child);

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
            ret.setResultIfUndone(context.getNodeContext(this).getAbortState());
            return;
        }

        if(childState == NodeState.SUCCEEDED)
        {
            getLogger().log(Level.INFO, "selector succeeded: " + this);
            ret.setResult(NodeState.SUCCEEDED);
        }
        else if(childState == NodeState.FAILED)
        {
            if(context.getNodeContext(this).getAborted() != null)
            {
                // abort result from child → selector fails
                getLogger().log(Level.INFO, "selector abort result treated as failure: " + this);

                ret.setResult(NodeState.FAILED);
            }
            else
            {
                executeNextChild(event, context).delegateTo(ret);
            }
        }
        else
        {
            getLogger().log(Level.WARNING, "selector received non-final state: " + childState);
        }
    }

    @Override
    public void reset(ExecutionContext<T> context, Boolean all)
    {
        super.reset(context, all);
        getStrategy().reset(this, context);
    }
}
