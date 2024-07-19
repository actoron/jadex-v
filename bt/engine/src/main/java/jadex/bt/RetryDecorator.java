package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class RetryDecorator<T> extends Decorator<T> 
{
    protected int max;

    public RetryDecorator(int max) 
    {
    	this.max = max;
    }

    @Override
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, T context) 
    {
        Future<NodeState> ret = new Future<>();
        executeWithRetry(node, event, state, 0, ret, context);
        return ret;
    }
    
    protected void executeWithRetry(Node<T> node, Event event, NodeState state, int attempt, Future<NodeState> ret, T context) 
    {
        if(attempt < max) 
        {
            node.internalExecute(event, context).then(s -> 
            {
                if(s == NodeState.FAILED && attempt + 1 < max) 
                    executeWithRetry(node, event, state, attempt + 1, ret, context);
                else 
                    ret.setResult(s);
            }).catchEx(ex -> 
            {
                if(attempt + 1 < max) 
                    executeWithRetry(node, event, state, attempt + 1, ret, context);
                else
                    ret.setResult(NodeState.FAILED);
            });
        } 
        else 
        {
            ret.setResult(NodeState.FAILED);
        }
    }
}