package jadex.bt;

import java.util.function.Predicate;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class ConditionalDecorator<T> extends Decorator<T> 
{
    private Predicate<Blackboard> condition;

    public ConditionalDecorator(Predicate<Blackboard> condition) 
    {
        this.condition = condition;
    }

    @Override
    public IFuture<NodeState> execute(Node<T> node, Event event, NodeState state, T context) 
    {
        if(condition.test(node.getBlackboard())) 
        {
            return node.internalExecute(event, context);
        } 
        else 
        {
            return new Future<>(NodeState.FAILED);
        }
    }
}
