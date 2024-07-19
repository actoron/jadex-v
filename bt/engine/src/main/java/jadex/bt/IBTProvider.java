package jadex.bt;

import jadex.core.IComponent;

public interface IBTProvider 
{
	public Node<IComponent> createBehaviorTree();
}
