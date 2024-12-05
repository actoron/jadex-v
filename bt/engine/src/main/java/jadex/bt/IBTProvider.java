package jadex.bt;

import jadex.bt.nodes.Node;
import jadex.core.IComponent;

public interface IBTProvider 
{
	public Node<IComponent> createBehaviorTree();
}
