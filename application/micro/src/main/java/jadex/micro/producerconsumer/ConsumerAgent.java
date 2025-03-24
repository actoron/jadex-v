package jadex.micro.producerconsumer;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.FutureBlockingQueue;
import jadex.injection.annotation.OnStart;

public class ConsumerAgent<R> 
{
	protected FutureBlockingQueue<R> queue;
	protected long maxdelay;
	
	public ConsumerAgent(FutureBlockingQueue<R> queue)
	{
		this(queue, 2000);
	}
	
	public ConsumerAgent(FutureBlockingQueue<R> queue, long maxdelay)
	{
		this.queue = queue;
		this.maxdelay = maxdelay;
	}
	
	@OnStart
	protected void start(IComponent agent)
	{
		while(true)
		{
			System.out.println(agent.getId()+" fetch item");
			
			R elem = queue.dequeue().get();
			
			System.out.println(agent.getId()+" received item: "+elem);
			
			long delay = (long)(Math.random()*maxdelay);
			System.out.println(agent.getId()+" waiting: "+delay);
			
			agent.getFeature(IExecutionFeature.class).waitForDelay(delay).get();
		}
	}
}
