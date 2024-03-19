package jadex.micro.producerconsumer;

import jadex.core.IComponent;

/**
 *  This example shows how a producer consumer pattern can be built with a future based 
 *  blocking queue.
 *  
 *  Both sides are balanced by the FutureBlockingQueue.
 *  producers >> consumers: queue defers enqueue
 *  consumers >> producers: queue defers dequeue
 *  
 *  The future based blocking queue is used as local shared data structure and provides
 *  protected against concurrent access. 
 */
public class Main 
{
	public static void main(String[] args) 
	{
		int psize = 3;
		int csize = 2;
		
		FutureBlockingQueue<String> queue = new FutureBlockingQueue<>(10);
		
		for(int i=0; i<psize; i++)
			IComponent.create(new ProducerAgent<String>(queue, 1000, () -> ""+Math.random()));
		
		for(int i=0; i<csize; i++)
			IComponent.create(new ConsumerAgent<String>(queue));
		
		IComponent.waitForLastComponentTerminated();
	}
}
