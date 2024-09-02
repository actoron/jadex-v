package jadex.bt.helloworld;

import jadex.bt.IBTProvider;
import jadex.bt.actions.UserAction;
import jadex.bt.nodes.ActionNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.micro.annotation.Agent;

@Agent(type="bt")
public class HelloWorldAgent implements IBTProvider
{
	public Node<IComponent> createBehaviorTree()
	{
		ActionNode<IComponent> an = new ActionNode<>("hello");
		an.setAction(new UserAction<IComponent>((e, agent) -> 
		{ 
			System.out.println("Hello from behavior trees: "+agent.getId());
			return new Future<>(NodeState.SUCCEEDED);
		}));
		return an;
	}
	
	public static void main(String[] args)
	{
		IComponent.create(new HelloWorldAgent());
		IComponent.waitForLastComponentTerminated();
	}
}
