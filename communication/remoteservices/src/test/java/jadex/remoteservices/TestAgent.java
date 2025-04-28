package jadex.remoteservices;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.messaging.IMessageFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class TestAgent
{

	/**
	 *  Creates the test Agent.
	 *
	 */
	public TestAgent()
	{

	}
	
    @OnStart
    protected void start(IComponent agent)
    {
        System.out.println("Test agent started: "+agent.getId());
        
        IMessageFeature msgfeat = agent.getFeature(IMessageFeature.class);

		System.out.println("Got msg feat" + msgfeat);

        IRemoteExecutionFeature remotexec = agent.getFeature(IRemoteExecutionFeature.class);

		System.out.println("Got remoteexecfeat: " + remotexec);

        agent.terminate();
    }

    public static void main(String[] args) 
    {
    	IComponentManager.get().create(new TestAgent());
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
