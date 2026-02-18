package jadex.bt;

import java.util.List;
import java.util.function.Consumer;

import jadex.bt.impl.Event;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ITriFunction;
import jadex.common.Tuple2;
import jadex.core.ChangeEvent;
import jadex.core.IChangeListener;
import jadex.future.IFuture;

public interface IConditionObserver<T>
{
    public List<Tuple2<String, IChangeListener>> observeCondition(
        List<ChangeEvent> events, 
        ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition, 
        Consumer<ChangeEvent> action, 
        Node<T> node, 
        ExecutionContext<T> execcontext);

    public void unobserveCondition(List<Tuple2<String, IChangeListener>> listeners, ExecutionContext<T> execontext);
}
