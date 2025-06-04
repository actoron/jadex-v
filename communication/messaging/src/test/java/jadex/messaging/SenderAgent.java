package jadex.messaging;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.injection.annotation.OnStart;

public class SenderAgent 
{
	protected String receiveragent;
	
	/**
	 *  Creates the sender Agent.
	 *  
	 *  @param receiveragent Name of the receiver agent.
	 */
	public SenderAgent(String receiveragent)
	{
		this.receiveragent = receiveragent;
	}
	
    @OnStart
    protected void start(IComponent agent)
    {
        System.out.println("Sender agent started: "+agent.getId());
        
        IMessageFeature msgfeat = agent.getFeature(IMessageFeature.class);
        
        ComponentIdentifier rec = ComponentIdentifier.fromString(receiveragent);
        
    	System.out.println("Sender: Sending Hello...");
    	msgfeat.sendMessage("Hello", rec);
        
    	System.out.println("Sender: Sending Ping, waiting for reply...");
    	String reply = msgfeat.sendMessageAndWait(rec, "Ping").get().message().toString();
    	System.out.println("Sender: Received reply: " + reply);
    	msgfeat.sendMessage("Received Pong", rec);
    }

    public static void main(String[] args) 
    {
    	IComponentManager.get().create(new SenderAgent(args[0]));
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
