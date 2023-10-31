package jadex.micro.tutorial.a6;

import java.awt.Desktop;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.mj.publishservice.IPublishServiceFeature;
import jadex.mj.publishservice.publish.annotation.Publish;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Service;
import jadex.requiredservice.annotation.OnService;

/**
 *  Chat micro agent provides a basic chat service and publishes it as rest web service.
 *  Additionally, in this example the agent provides a minimal web user interface to 
 *  send chat messages and show all received messages.
 *  
 *  - The agent uses IChatService to send messages to other agents
 *  - The agent uses IChatGuiService to talk to the UI
 *    - The UI subscribes to the IChatGuiService to receive chat messages (and other notifications)
 *    - The UI uses sendMessageToAll of IChatGuiService to tell its agent to send the chat message to the other agents
 *	- The agent publishes the UI via its folder and the contained index.html page on the web server
 */
@Agent
@Service
@Publish(publishid="http://localhost:8081/${cid}/chatapi", publishtarget = IChatGuiService.class)
public class ChatAgent implements IChatService, IChatGuiService
{
	/** The underlying micro agent. */
	@Agent
	protected IComponent agent;
	
	@OnService
	protected Set<IChatService> chatservices = new HashSet<IChatService>();
	
	protected Set<SubscriptionIntermediateFuture<String>> subscribers = new HashSet<SubscriptionIntermediateFuture<String>>();
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId().getLocalName());
		
		IPublishServiceFeature ps = agent.getFeature(IPublishServiceFeature.class);
		ps.publishResources("http://localhost:8081/${cid}", "jadex/micro/tutorial/a6");
		
		openInBrowser("http://localhost:8081/"+agent.getId().getLocalName());
		//openInBrowser("http://localhost:8081/"+agent.getId()+"/index2.html");
	}
	
	/**
	 *  Receives a chat message.
	 *  @param sender The sender's name.
	 *  @param text The message text.
	 */
	public void message(final String sender, final String text)
	{
		String txt = new SimpleDateFormat("hh:mm:ss").format(new Date())+" from: "+sender+" message: "+text;
		//System.out.println(txt);
		
		// Forward the chat message to the web ui
		//System.out.println("forward message to ui: "+subscribers.size());
		for(SubscriptionIntermediateFuture<String> subscriber: subscribers)
			subscriber.addIntermediateResultIfUndone(txt);
	}
	
	/**
	 *  Send a message to all chat services;
	 *  @param text The text.
	 */
	public void sendMessageToAll(String text)
	{
		//System.out.println("found services: "+chatservices.size());
		for(Iterator<IChatService> it=chatservices.iterator(); it.hasNext(); )
		{
			IChatService cs = it.next();
			cs.message(agent.getId().getLocalName(), text);
		}
	}
	
	/**
	 *  Get the name of the chatter.
	 *  @return The name.
	 */
	public IFuture<String> getName()
	{
		return new Future<String>(agent.getId().getLocalName());
	}
	
	/**
	 *  Subscribe to chat.
	 *  @return Chat events.
	 */
	public ISubscriptionIntermediateFuture<String> subscribeToChat()
	{
		SubscriptionIntermediateFuture<String> ret = new SubscriptionIntermediateFuture<String>();
		ret.setTerminationCommand((ex) -> subscribers.remove(ret));
		subscribers.add(ret);
		return ret;
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
		IComponent.create(new ChatAgent());
		IComponent.create(new ChatAgent());
		IComponent.create(new ChatAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}