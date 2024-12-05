package jadex.micro.tutorial.a3;

import java.text.SimpleDateFormat;
import java.util.Date;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Service;

/**
 *  Chat micro agent provides a basic chat service. 
 *  
 *  This example shows how a simple chat user interface can be provided via swing.
 *  The ui interacts with the agent via the external access (as all external threads).
 */
@Agent
@Service
public class ChatAgent implements IChatService
{
	/** The underlying micro agent. */
	@Agent
	protected IComponent agent;
	
	protected ChatGui gui;
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId());
		
		this.gui = new ChatGui(agent.getExternalAccess());
	}
	
	@OnEnd
	protected void end()
	{
		this.gui.dispose();
	}
	
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(final String sender, final String text)
	{
		String txt = agent.getId()+" received at "+new SimpleDateFormat("hh:mm:ss").format(new Date())+" from: "+sender+" message: "+text;
		gui.addMessage(txt);
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		MicroAgent.create(new ChatAgent());
		MicroAgent.create(new ChatAgent());
		MicroAgent.create(new ChatAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}