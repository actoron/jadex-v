package jadex.bt.helloworld;

import jadex.bt.BTCache;
import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.TerminableFuture;
import jadex.micro.annotation.Agent;

@Agent(type="bt")
public class HelloSharedBTAgent implements IBTProvider 
{ 
    public Node<IComponent> createBehaviorTree() 	
    { 		
       	return BTCache.createOrGet(HelloSharedBTAgent.class, HelloSharedBTAgent::buildBehaviorTree);
       	//return BTCache.createOrGet(HelloSharedBTAgent.class, this::createBehaviorTree2);
    }
    
    /*public Node<IComponent> createBehaviorTree2() 	
    { 		
        ActionNode<IComponent> an = new ActionNode<>("hello"); 		
        an.setAction(new UserAction<IComponent>((e, agent) -> 
        {  			
            Future<NodeState> ret = new Future<>(); 			
            System.out.println("Hello from behavior trees: "+agent.getId()); 			
            agent.getFeature(IExecutionFeature.class).waitForDelay(2000).then(Void -> ret.setResult(NodeState.SUCCEEDED)).catchEx(ret); 			
            return ret; 		
        })); 		
        return an; 	
    } */
    
    public static Node<IComponent> buildBehaviorTree()
    {
    	System.out.println("build");
    	ActionNode<IComponent> an = new ActionNode<>("hello"); 		
        an.setAction(new TerminableUserAction<IComponent>((e, agent) -> 
        {  			
            TerminableFuture<NodeState> ret = new TerminableFuture<>(); 			
            System.out.println("Hello from behavior trees: "+agent.getId()); 			
            agent.getFeature(IExecutionFeature.class).waitForDelay(2000).then(Void -> ret.setResult(NodeState.SUCCEEDED)).catchEx(ret); 			
            return ret; 		
        })); 		
        return an; 	
    }
    
	public static void main(String[] args) 
	{ 
		int cnt = 5;
		
		for(int i=0; i<cnt; i++)
			IComponentManager.get().create(new HelloSharedBTAgent()).get(); 	
        
		IComponentManager.get().waitForLastComponentTerminated();
    }
}