package jadex.bt;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class RetryDecorator extends Decorator 
{
    protected int max;

    public RetryDecorator(int max) 
    {
    	this.max = max;
    }

    @Override
    public IFuture<NodeState> execute(Node node, Event event, NodeState state) 
    {
        Future<NodeState> ret = new Future<>();
        executeWithRetry(node, event, state, 0, ret);
        return ret;
    }
    
    protected void executeWithRetry(Node node, Event event, NodeState state, int attempt, Future<NodeState> ret) 
    {
        if(attempt < max) 
        {
            node.internalExecute(event).then(s -> 
            {
                if(s == NodeState.FAILED && attempt + 1 < max) 
                    executeWithRetry(node, event, state, attempt + 1, ret);
                else 
                    ret.setResult(s);
            }).catchEx(ex -> 
            {
                if(attempt + 1 < max) 
                    executeWithRetry(node, event, state, attempt + 1, ret);
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