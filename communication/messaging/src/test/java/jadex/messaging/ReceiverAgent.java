package jadex.messaging;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

/**
 *  Receiving agent for the messaging test.
 *  Run this for testing, the SenderAgent is started automatically.
 */
@Agent
public class ReceiverAgent 
{
	/**
	 *  Agent start method
	 *  @param agent The agent.
	 */
    @OnStart
    protected void start(IComponent agent)
    {
        System.out.println("Receiver agent started: "+agent.getId());
        
        IMessageFeature msgfeat = agent.getFeature(IMessageFeature.class);
        
        Future<Void> oneshot = new Future<>();
        Future<Void> exchange = new Future<>();
        
        msgfeat.addMessageHandler(new IMessageHandler() {
			
			/**
			 *  Test if remove after one message.
			 */
        	@Override
			public boolean isRemove()
        	{
				return false;
			}
			
        	/**
        	 * Test if message is handled.
        	 */
			@Override
			public boolean isHandling(ISecurityInfo secinfos, Object msg)
			{
				return msg instanceof String || msg instanceof SecureExchange;
			}
			
			/**
			 *  Handle message.
			 */
			@Override
			public void handleMessage(ISecurityInfo secinfos, Object msg)
			{
				if (msg instanceof String)
				{
					if ("Received Pong".equals(msg))
					{
						exchange.setResult(null);
					}
					else
					{
						System.out.println("Receiver: Agent has received oneshot message: " + msg);
						oneshot.setResult(null);
					}
				}
				else
				{
					SecureExchange secmsg = (SecureExchange) msg;
					System.out.println("Receiver: Agent has received exchange message: " + secmsg.message() + ", replying...");
					IMessageFeature msgfeat = agent.getFeature(IMessageFeature.class);
					msgfeat.sendReply(secmsg.sender(), secmsg.conversationid(), "Pong");
				}
			}
		});
        
        System.out.println("Receiver listening: " + agent.getId());
        
        Process subproc = SUtil.runJvmSubprocess(SenderAgent.class, null, Arrays.asList(new String[] { agent.getId().toString() }), true);
		
        oneshot.get();
        exchange.get();
        subproc.destroy();
        System.out.println("Messaging Test successful.");
        agent.terminate();
    }
    
    /**
     *  Run the messaging test.
     *  
     *  @throws Exception Thrown when broken.
     */
    @Test
	public void testAgentMessaging() throws Exception
	{
    	IComponentManager.get().create(this).get();
    	IComponentManager.get().waitForLastComponentTerminated();
	}

    /**
     *  Main method.
     *  @param args Command-line arguments.
     */
    public static void main(String[] args) 
    {
    	IComponentManager.get().create(new ReceiverAgent()).get();
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
