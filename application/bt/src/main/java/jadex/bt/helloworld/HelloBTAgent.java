package jadex.bt.helloworld;

import jadex.bt.IBTProvider;
import jadex.bt.actions.TerminableUserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.future.TerminableFuture;

public class HelloBTAgent implements IBTProvider 
{ 
    public Node<IComponent> createBehaviorTree() 	
    { 		
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
        IComponentManager.get().create(new HelloBTAgent()).get(); 	
        IComponentManager.get().waitForLastComponentTerminated();
    }
}