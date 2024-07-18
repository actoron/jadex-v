package jadex.bt;

import java.util.function.Predicate;

import jadex.bt.Node.NodeState;
import jadex.future.Future;
import jadex.future.IFuture;

public class ConditionalDecorator extends Decorator 
{
    private Predicate<Blackboard> condition;

    public ConditionalDecorator(Predicate<Blackboard> condition) 
    {
        this.condition = condition;
    }

    @Override
    public IFuture<NodeState> execute(Node node, Event event, NodeState state) 
    {
        if(condition.test(node.getBlackboard())) 
        {
            return node.internalExecute(event);
        } 
        else 
        {
            return new Future<>(NodeState.FAILED);
        }
    }
}
