package jadex.micro.producerconsumer;

import java.util.Queue;
import java.util.function.Supplier;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

@Agent
public class ProducerAgent<R> 
{
	protected FutureBlockingQueue<R> queue;
	protected Supplier<R> create;
	protected long maxdelay;
	
	public ProducerAgent(FutureBlockingQueue<R> queue, Supplier<R> create)
	{
		this(queue, 2000, create);
	}
	
	public ProducerAgent(FutureBlockingQueue<R> queue, long maxdelay, Supplier<R> create)
	{
		this.queue = queue;
		this.maxdelay = maxdelay;
		this.create = create;
	}
	
	@OnStart
	protected void onStart(IComponent agent)
	{
		while(true)
		{
			long delay = (long)(Math.random()*maxdelay);
			System.out.println(agent.getId()+" waiting: "+delay);
			agent.getFeature(IExecutionFeature.class).waitForDelay(delay).get();
			
			R item = create.get();
			System.out.println(agent.getId()+" created: "+item);
			queue.enqueue(item).get();
			System.out.println(agent.getId()+" added: "+item+" "+queue.size());
		}
	}
}
