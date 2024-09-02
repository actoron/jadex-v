package jadex.bt.decorators;

import jadex.bt.impl.BTAgentFeature;
import jadex.bt.impl.Event;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.ITriFunction;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.rules.eca.EventType;

// depends on agent usage
public class TriggerDecorator<T> extends ConditionalDecorator<T>
{
	protected ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition;
	
	public TriggerDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		return (TriggerDecorator<T>)super.setAsyncCondition(condition);
	}
	
	public TriggerDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		return (TriggerDecorator<T>)super.setCondition(condition);
	}
	
	@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		return null; // trigger is never checked on node execution (then it is already triggered)
	}
	
	public TriggerDecorator<T> observeCondition(EventType[] events)
	{
		super.observeCondition(events, (event, rule, context, condresult) -> // action
		{
			System.out.println("trigger condition triggered: "+event);
			
			// Execution in next step is too late, as parent then executes next child before
			/*getSelf().getFeature(IExecutionFeature.class).scheduleStep(agent ->
			{
				BTAgentFeature.get().executeBehaviorTree(node, new Event(event.getType().toString(), event));
			});*/
			
			NodeContext<T> ncontext = getNode().getNodeContext(getExecutionContext());
			
			if(NodeState.RUNNING==ncontext.getState())
			{
				System.out.println("node already active, ignoring: "+getNode());
				return IFuture.DONE;
			}
			
			// todo: remove hack
			boolean resetted = BTAgentFeature.get().resetOngoingExecution((Node<IComponent>)getNode(), (ExecutionContext<IComponent>)getExecutionContext());
			
			if(!resetted)
			{
				IExecutionFeature.get().scheduleStep(agent ->
				{
					System.out.println("triggered complete tree reexecution");
					// todo: remove hack
					BTAgentFeature.get().executeBehaviorTree((Node<IComponent>)getNode(), new Event(event.getType().toString(), event));
				});
			}
			
			return IFuture.DONE;
		});
		
		return this;
	}
}
