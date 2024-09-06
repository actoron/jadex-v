package jadex.messaging;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.messaging.IMessageFeature;
import jadex.messaging.IMessageHandler;
import jadex.messaging.ISecurityInfo;
import jadex.messaging.security.Security;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
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
    	Security.get();
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
    	IComponent.create(new SenderAgent(args[0]));
        IComponent.waitForLastComponentTerminated();
    }
}
