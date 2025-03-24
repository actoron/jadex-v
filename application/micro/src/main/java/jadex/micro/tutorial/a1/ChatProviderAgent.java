package jadex.micro.tutorial.a1;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;

/**
 *  Chat micro agent provides a basic chat service as separate class. 
 */
public class ChatProviderAgent
{
	/** The underlying micro agent. */
	@Inject
	protected IComponent agent;
	
	/** The chat service. */
	protected IChatService	chat	= new ChatService();
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId());
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		IComponentManager.get().create(new ChatProviderAgent());
	}
}