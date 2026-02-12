package jadex.bt.decorators;

import java.lang.System.Logger.Level;
import java.util.function.Function;

import jadex.bt.impl.Event;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.common.ITriFunction;
import jadex.core.ChangeEvent;
import jadex.future.IFuture;

public class ChildCreationDecorator<T> extends ConditionalDecorator<T>
{
	protected Function<Event, Node<T>> creator;
	
	public ChildCreationDecorator()
	{
		this.action	= event ->
		{
			System.getLogger(getClass().getName()).log(Level.INFO, "child creation condition triggered: "+event);
			//System.out.println("child creation condition triggered: "+event);
			
			Event e = new Event(event.type().name()+"."+event.name(), event.value());
			Node<T> child = creator.apply(e);
			
			CompositeNode<T> node = (CompositeNode<T>)getNode();
			// todo? should all dynamically added nodes be removed after final execution?
			child.addDecorator(new RemoveDecorator<T>());
			
			node.addChild(child, e, getExecutionContext());
		};
	}
	
	@Override
	public ChildCreationDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		return (ChildCreationDecorator<T>)super.setAsyncCondition(condition);
	}
	
	@Override
	public ChildCreationDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		return (ChildCreationDecorator<T>)super.setCondition(condition);
	}
	
	@Override
	public ChildCreationDecorator<T> setEvents(ChangeEvent... events)
	{
		return (ChildCreationDecorator<T>) super.setEvents(events);
	}

	@Override
	public Decorator<T> setNode(Node<T> node)
	{
		if(!(node instanceof CompositeNode<T>))
			throw new IllegalArgumentException("Must be added on composite node");
		this.node = node;
		return this;
	}
	
	@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		return null;
	}
	
	public ChildCreationDecorator<T> setChildCreator(Function<Event, Node<T>> creator)
	{
		this.creator = creator;
		return this;
	}
}
