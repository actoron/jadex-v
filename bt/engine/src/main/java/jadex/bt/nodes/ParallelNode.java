package jadex.bt.nodes;

import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jadex.bt.IChildTraversalStrategy;
import jadex.bt.impl.Event;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.future.Future;
import jadex.future.IFuture;

/**
 * Parallel execution of children.
 */
public class ParallelNode<T> extends CompositeNode<T>
{
    private static final String KEY_RUNTIMEINFO = "runtimeinfo";

    public enum ResultMode
    {
        ON_ONE,
        ON_ALL
    }

    protected ResultMode failmode = ResultMode.ON_ONE;
    protected ResultMode successmode = ResultMode.ON_ALL;
    protected boolean keeprunning = false;

    public ParallelNode()
    {
		this(null);
    }

    public ParallelNode(String name)
    {
        this(name, null);
    }

    public ParallelNode(String name, IChildTraversalStrategy<T> strategy)
    {
        super(name, strategy);
    }

    public ParallelNode<T> setFailureMode(ResultMode failmode)
    {
        this.failmode = failmode;
        return this;
    }

    public ParallelNode<T> setSuccessMode(ResultMode successmode)
    {
        this.successmode = successmode;
        return this;
    }

    public boolean isKeepRunning()
    {
        return keeprunning;
    }

    public ParallelNode<T> setKeepRunning(boolean keeprunning)
    {
        this.keeprunning = keeprunning;
        return this;
    }

	@Override
	public IFuture<NodeState> internalExecute(Event event, NodeState mystate, ExecutionContext<T> context)
	{
        Map<Node<T>, IFuture<NodeState>> results = new LinkedHashMap<>();
    	Future<NodeState> ret = new Future<>();

    	NodeContext<T> nc = getNodeContext(context);
        RuntimeInfo<T> ri = getRuntimeInfo(context, ret, results);

		List<Node<T>> children = getChildren(context); 

		for(Node<T> child : children)
		{
			executeChild(child, event, results, context, ret);
		}

		executeAbortWait(context)
			.then(abortstate -> handleFinish(abortstate, ret, results, context));

		return ret;
	}

    // ------------------------------------------------------------------------

    @Override
    public CompositeNode<T> addChild(Node<T> child, Event event, ExecutionContext<T> context)
    {
        super.addChild(child, event, context);

        NodeContext<T> nc = getNodeContext(context);
        RuntimeInfo<T> ri = getRuntimeInfo(context, null, null);

        Future<NodeState> call = ri.getInternalCall();
        if(call != null && !call.isDone())
        {
            getLogger().log(Level.INFO, "parallel executing new child: " + this + " -> " + child);

            executeChild(child, event, ri.getResults(), context, call);
        }

        return this;
    }

    protected boolean executeChild(
        Node<T> child,
        Event event,
        Map<Node<T>, IFuture<NodeState>> results,
        ExecutionContext<T> context,
        Future<NodeState> fut)
    {
        IFuture<NodeState> childfut = child.execute(event, context);
        results.put(child, childfut);

        if(childfut.isDone())
        {
            NodeState state = childfut.get();
            if(checkFastExit(state))
            {
                handleFinish(state, fut, results, context);
                return true;
            }

            NodeState fin = checkFinished(results, context);
            if(fin != null)
                handleFinish(fin, fut, results, context);
        }

        childfut.then(state -> handleResult(state, results, fut, context))
        	.catchEx(ex -> handleResult(NodeState.FAILED, results, fut, context));

        return false;
    }

    protected boolean checkFastExit(NodeState state)
    {
        return !keeprunning &&
            ((failmode == ResultMode.ON_ONE && state == NodeState.FAILED) ||
             (successmode == ResultMode.ON_ONE && state == NodeState.SUCCEEDED));
    }

    protected NodeState checkFinished(Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> context)
    {
        if(keeprunning)
            return null;

        if(checkSuccess(results, context))
            return NodeState.SUCCEEDED;
        if(checkFailure(results, context))
            return NodeState.FAILED;

        return null;
    }

   	protected boolean checkSuccess(Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> context)
	{
		if(successmode != ResultMode.ON_ALL)
			return false;

		List<Node<T>> children = getChildren(context);

		if(results.size() != children.size())
			return false;

		return results.values().stream()
			.allMatch(f -> f.isDone() && f.get() == NodeState.SUCCEEDED);
	}

	protected boolean checkFailure(Map<Node<T>, IFuture<NodeState>> results, ExecutionContext<T> context)
	{
		if(failmode != ResultMode.ON_ALL)
			return false;

		List<Node<T>> children = getChildren(context);

		if(results.size() != children.size())
			return false;

		return results.values().stream()
			.allMatch(f -> f.isDone() && f.get() == NodeState.FAILED);
	}


    protected void handleFinish(
        NodeState state,
        Future<NodeState> ret,
        Map<Node<T>, IFuture<NodeState>> results,
        ExecutionContext<T> context)
    {
        if(ret.isDone())
            return;

        if(context.getNodeContext(this).getAbortState() != null)
        {
            ret.setResultIfUndone(context.getNodeContext(this).getAbortState());
            return;
        }

        for(Entry<Node<T>, IFuture<NodeState>> entry : results.entrySet())
        {
            if(!entry.getValue().isDone())
            {
                entry.getKey().abort(AbortMode.SELF, NodeState.FAILED, context);
            }
        }

        ret.setResultIfUndone(state);
    }

    protected void handleResult(NodeState state, Map<Node<T>, IFuture<NodeState>> results,
        Future<NodeState> ret, ExecutionContext<T> context)
    {
        if(checkFastExit(state))
        {
            handleFinish(state, ret, results, context);
        }
        else
        {
            NodeState fin = checkFinished(results, context);
            if(fin != null)
                handleFinish(fin, ret, results, context);
        }
    }

    protected IFuture<NodeState> executeAbortWait(ExecutionContext<T> context)
    {
        Future<NodeState> ret = new Future<>();
        getRuntimeInfo(context, null, null).setAbortWait(ret);
        return ret;
    }

    /*@Override
    public boolean hasIndex()
    {
        return false;
    }*/

    public RuntimeInfo<T> getRuntimeInfo()
    {
        return getRuntimeInfo();
    }

    public RuntimeInfo<T> getRuntimeInfo(ExecutionContext<T> context, Future<NodeState> call, Map<Node<T>, IFuture<NodeState>> results)
    {
        NodeContext<T> nc = getNodeContext(context);
        RuntimeInfo<T> ret = (RuntimeInfo<T>) nc.getValue(KEY_RUNTIMEINFO);
        if(ret==null)
        {
            if(call==null || results==null)
                System.out.println("Creating parallel node context without results and call: "+this);
            ret = new RuntimeInfo<>(call, results);
            nc.setValue(KEY_RUNTIMEINFO, ret);
        }
        return ret;
    }

    public static class RuntimeInfo<T>
    {
        protected Future<NodeState> internalcall;
        protected Map<Node<T>, IFuture<NodeState>> results;
        protected Future<NodeState> abortwait;

        public RuntimeInfo(Future<NodeState> call, Map<Node<T>, IFuture<NodeState>> results)
        {
            this.internalcall = call;
            this.results = results;
        }

        public Future<NodeState> getInternalCall()
        {
            return internalcall;
        }

        public void setInternalCall(Future<NodeState> internalcall)
        {
            this.internalcall = internalcall;
        }

        public Map<Node<T>, IFuture<NodeState>> getResults()
        {
            return results;
        }

        public void setResults(Map<Node<T>, IFuture<NodeState>> results)
        {
            this.results = results;
        }

        public Future<NodeState> getAbortWait()
        {
            return abortwait;
        }

        public void setAbortWait(Future<NodeState> abortwait)
        {
            this.abortwait = abortwait;
        }
    }

    @Override
    public void setAbortFuture(IFuture<Void> abortfuture, ExecutionContext<T> context)
    {
        super.setAbortFuture(abortfuture, context);

        Future<NodeState> aw = getRuntimeInfo(context, null, null).getAbortWait();
        if(aw != null)
            aw.setResultIfUndone(getNodeContext(context).getAbortState());
    }
}
