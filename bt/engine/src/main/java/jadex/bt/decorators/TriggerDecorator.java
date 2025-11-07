package jadex.bt.decorators;

import java.lang.System.Logger.Level;

import jadex.bt.impl.BTAgentFeature;
import jadex.bt.impl.Event;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;
import jadex.common.ITriFunction;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;
import jadex.rules.eca.EventType;

public class TriggerDecorator<T> extends ConditionalDecorator<T>
{
	public TriggerDecorator()
	{
		this.action	= (event, rule, context, condresult) -> // action
		{
			System.getLogger(getClass().getName()).log(Level.INFO, "trigger condition triggered: "+event+" "+this);
			//System.out.println("trigger condition triggered: "+event);
			
			// Execution in next step is too late, as parent then executes next child before
			/*getSelf().getFeature(IExecutionFeature.class).scheduleStep(agent ->
			{
				BTAgentFeature.get().executeBehaviorTree(node, new Event(event.getType().toString(), event));
			});*/
			
			NodeContext<T> ncontext = getNode().getNodeContext(getExecutionContext());
			
			if(ncontext!=null && NodeState.RUNNING==ncontext.getState())
			{
				//System.out.println("node already active, ignoring: "+getNode());
				System.getLogger(""+this.getClass()).log(Level.INFO, "node already active, ignoring: "+getNode());
				return IFuture.DONE;
			}
			
			// todo: remove hack
			boolean resetted = BTAgentFeature.get().resetOngoingExecution((Node<IComponent>)getNode(), (ExecutionContext<IComponent>)getExecutionContext());
			
			if(!resetted)
			{
				IExecutionFeature.get().scheduleStep(agent ->
				{
					//System.out.println("triggered complete tree reexecution");
					System.getLogger(""+this.getClass()).log(Level.INFO, "triggered complete tree reexecution: "+getNode());
					// todo: remove hack
					BTAgentFeature.get().executeBehaviorTree((Node<IComponent>)getNode(), new Event(event.getType().toString(), event));
				});
			}
			
			return IFuture.DONE;
		};
	}
	
	public TriggerDecorator<T> setAsyncCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, IFuture<Boolean>> condition)
	{
		return (TriggerDecorator<T>)super.setAsyncCondition(condition);
	}
	
	public TriggerDecorator<T> setCondition(ITriFunction<Event, NodeState, ExecutionContext<T>, Boolean> condition) 
	{
		return (TriggerDecorator<T>)super.setCondition(condition);
	}
	
	@Override
	public TriggerDecorator<T> setEvents(EventType... events)
	{
		return (TriggerDecorator<T>) super.setEvents(events);
	}
	
	
	@Override
	public IFuture<NodeState> beforeExecute(Event event, NodeState state, ExecutionContext<T> context) 
	{
		return null; // trigger is never checked on node execution (then it is already triggered)
		// otherwise e.g. in cleaner it would stop loading shortly over trigger battery level
	}
	
	@Override
	public NodeState mapToNodeState(Boolean state, NodeState nstate)
	{
		NodeState ret = state!=null && !state? NodeState.FAILED: nstate; 
		System.out.println("trigger deco map result: "+state+":"+ret+" "+this);
		return ret;
	}
}
