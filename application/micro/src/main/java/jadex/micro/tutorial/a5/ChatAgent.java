package jadex.micro.tutorial.a5;

import java.awt.Desktop;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.publishservice.publish.annotation.Publish;
import jadex.requiredservice.annotation.InjectService;
import jadex.requiredservice.annotation.InjectService.Mode;

/**
 *  Chat micro agent provides a basic chat service and publishes it as rest web service.
 *  
 *  It can be invoked via the standard service info page at the publish url: http://localhost:8081/chat.
 *  The agent receives the message and display at the console.
 */
@Publish(publishid="http://localhost:8081/chat")
public class ChatAgent implements IChatService
{
	/** The underlying micro agent. */
	@Inject
	protected IComponent agent;
	
	@InjectService(mode=Mode.QUERY)
	protected Set<IChatService> chatservices = new HashSet<IChatService>();
	
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(final String sender, final String text)
	{
		String txt = agent.getId()+" received at "+new SimpleDateFormat("hh:mm:ss").format(new Date())+" from: "+sender+" message: "+text;
		System.out.println(txt);
		//gui.addMessage(txt);
	}
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId());
		
		openInBrowser("http://localhost:8081/chat");
	}
	
	@OnEnd
	protected void end()
	{
		System.out.println("on end called: "+agent.getId());
		//this.gui.dispose();
	}
	
	/**
	 *  Get the chat services.
	 *  @return The chat services.
	 */
	public Set<IChatService> getChatServices()
	{
		return chatservices;
	}
	
	/**
	 *  Open the url in the browser.
	 *  @param url The url.
	 */
	protected void openInBrowser(String url)
	{
		try 
		{
			URI uri = new URI(url);
			Desktop.getDesktop().browse(uri);
		}	
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		IComponentManager.get().create(new ChatAgent());
		//IComponentManager.get().create(new ChatAgent());
		//IComponentManager.get().create(new ChatAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}