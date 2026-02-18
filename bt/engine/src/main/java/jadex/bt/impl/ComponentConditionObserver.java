package jadex.bt.impl;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jadex.bt.IConditionObserver;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.ITriFunction;
import jadex.common.Tuple2;
import jadex.core.ChangeEvent;
import jadex.core.IChangeListener;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.injection.IInjectionFeature;

public class ComponentConditionObserver<T> implements IConditionObserver<T>
{
    @Override
    public List<Tuple2<String, IChangeListener>> observeCondition(
        List<ChangeEvent> events, 
        ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition, 
        //ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<NodeState>> function,
        Consumer<ChangeEvent> action, 
        Node<T> node, 
        ExecutionContext<T> execontext) 
    {
        List<Tuple2<String, IChangeListener>> listeners = new ArrayList<>();

        IComponent component = (IComponent)execontext.getUserContext();
        IInjectionFeature inj = component.getFeature(IInjectionFeature.class);
	    for(ChangeEvent event: events)
		{
            IChangeListener lis = e ->
			{
              if((event.type()==null || event.type()==e.type())
                    && (event.info()==null || event.info().equals(e.info())))
                {
                    if(condition!=null)
                    {
                        NodeContext<IComponent> context = node.getNodeContext((ExecutionContext)execontext);
                        IFuture<Boolean> fut = condition.apply(new Event(e.type().toString(), e.value()), context!=null? context.getState(): NodeState.IDLE, execontext);
                        fut.then(triggered ->
                        {
                            if(triggered)
                            {
                                action.accept(e);
                            }
                            //else
                            //{
                                //System.out.println("condition not triggered: "+deco+" "+e);
                                //IFuture<Boolean> fut2 = cond.apply(new Event(e.type().toString(), e.value()), context!=null? context.getState(): NodeState.IDLE, getExecutionContext());
                            //}
                        }).catchEx(ex -> 
                        {
                            System.getLogger(getClass().getName()).log(Level.WARNING, "Exception in condition: "+ex);
                        });
                    }
                    else
                    {
                        System.getLogger(getClass().getName()).log(Level.WARNING, "Rule without condition: "+this);
                    }
                }
            };
            
            listeners.add(new Tuple2<>(event.name(), lis));

		    inj.addListener(event.name(), lis);
            
        }
        return listeners;
    }

    @Override
    public void unobserveCondition(List<Tuple2<String, IChangeListener>> listeners, ExecutionContext<T> execontext) 
    {
        IComponent component = (IComponent)execontext.getUserContext();
        IInjectionFeature inj = component.getFeature(IInjectionFeature.class);
        for(Tuple2<String, IChangeListener> tuple: listeners)
        {
            inj.removeListener(tuple.getFirstEntity(), tuple.getSecondEntity());
        }
    }
}
