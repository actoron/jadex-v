package jadex.micro.messagequeue;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.requiredservice.annotation.OnService;

/**
 *  Example queue user that registers at the queue with a topic and
 *  publishes a number of topics before terminating.
 */
@Agent
public class UserAgent
{
	//-------- attributes --------
	
	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/** The topic. */
	protected String topic;
	
	//-------- methods --------

	public UserAgent()
	{
		this(null);
	}
	
	public UserAgent(String topic)
	{
		this.topic = topic==null? "default_topic": topic;
	}
	
	@OnService
	public void onService(IMessageQueueService mq)
	{
		final ISubscriptionIntermediateFuture<Event> fut = mq.subscribe(topic);
		
		fut.next(event ->
		{
			System.out.println("Received: "+agent.getId()+" "+event);
		}).catchEx(ex ->
		{
			System.out.println("Ex: "+ex);
		});

		for(int i=0; i<10; i++)
		{
			mq.publish(topic, new Event("some type", i, agent.getId()));
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
		}
		fut.terminate();
	}
}
