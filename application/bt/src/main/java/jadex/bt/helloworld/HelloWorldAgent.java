package jadex.bt.helloworld;

import jadex.bt.ActionNode;
import jadex.bt.IBTProvider;
import jadex.bt.Node;
import jadex.bt.Node.NodeState;
import jadex.bt.UserAction;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.micro.annotation.Agent;

@Agent(type="bt")
public class HelloWorldAgent implements IBTProvider
{
	public Node<IComponent> createBehaviorTree()
	{
		ActionNode<IComponent> an = new ActionNode<>();
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
