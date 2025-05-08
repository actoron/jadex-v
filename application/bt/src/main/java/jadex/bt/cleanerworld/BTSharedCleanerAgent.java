package jadex.bt.cleanerworld;

import jadex.bt.BTCache;
import jadex.bt.nodes.Node;
import jadex.core.IComponent;

public class BTSharedCleanerAgent extends BTCleanerAgent
{
	public Node<IComponent> createBehaviorTree() 	
    { 		
       	return BTCache.createOrGet(BTSharedCleanerAgent.class, BTCleanerAgent::buildBehaviorTree);
    }
}
