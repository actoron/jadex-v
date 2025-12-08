package jadex.bt;

import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;
import jadex.core.IComponent;
import jadex.execution.impl.ComponentTimerCreator;

public interface IBTProvider 
{
	public Node<IComponent> createBehaviorTree();

	public default ExecutionContext<IComponent> createExecutionContext(IComponent component)
	{
		return new ExecutionContext<IComponent>(component, new ComponentTimerCreator());
	}
}
