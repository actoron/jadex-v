package jadex.micro.producerconsumer;

import java.util.LinkedList;
import java.util.Queue;

import jadex.future.Future;

public class FutureBlockingQueue<T> 
{
	protected int capacity;

    protected final Queue<T> queue = new LinkedList<T>()
    {
    	public boolean add(T e) 
    	{
    		if(size()<capacity)
    			return super.add(e);
    		else
    			return false;
    	}
    };
    protected final Queue<Future<T>> consumers = new LinkedList<>();
    protected final Queue<Runnable> producers = new LinkedList<>();

	public FutureBlockingQueue(int capacity)
	{
		this.capacity = capacity;
	}
    
    public Future<Void> enqueue(T elem)
    {
    	Future<Void> ret = new Future<>();
    	boolean rset = false;
    	Future<T> cset1 = null;
    	
    	synchronized(this)
    	{
	    	if(consumers.isEmpty())
	    	{
	    		if(queue.offer(elem))
	    		{
	    			rset = true;
	    		}
	    		else
	    		{
	    			producers.add(() -> 
	    			{
	    				Future<T> cset2 = null;
	    				synchronized(FutureBlockingQueue.this)
	    				{
		    				if(consumers.isEmpty())
		    				{
		    					queue.offer(elem);
		    				}
		    				else
		    				{
			    				cset2 = consumers.poll();
			    				
		    			    }
	    				}
	    				if(cset2!=null)
	    					cset2.setResult(elem);
	    				ret.setResult(null);
	    			});
	    		}
	    	}
	    	else
	    	{
	    		cset1 = consumers.poll();
	    	}
    	}
    	
    	if(cset1!=null)
    		cset1.setResult(elem);
    	if(rset || cset1!=null)
			ret.setResult(null);
    	
    	return ret;
    }
    
    public Future<T> dequeue()
    {
    	Future<T> ret = new Future<>();
    	Runnable run = null;
    	T elem = null;
    	
    	synchronized(this)
    	{
	    	if(queue.isEmpty())
	    	{
	    		consumers.offer(ret);
	    	}
	    	else
	    	{
	    		elem = queue.poll();
	      		
	    		if(!producers.isEmpty())
	      			 run = producers.poll();
	    		
	    	}
    	}
    	
    	if(elem!=null)
    		ret.setResult(elem);
    	
    	if(run!=null)
    		run.run();
    	
    	return ret;
    }
    
    public synchronized int size()
    {
    	return queue.size();
    }
}