package jadex.micro.philosophers.thread;

import java.util.Arrays;

public class Table 
{
	/** The chop sticks */
    protected String[] sticks;
    
    /** The current owners of the sticks (who has stick 1, 2, ...). */
    protected Philosopher[] owners;
    
    /** The philosopher that sit at the table. */
    protected Philosopher[] philosophers;
    
    /** Wait for times or click events. */
    protected boolean waitforclicks;
    
    public Table(int seats, boolean waitforclicks) 
    {
    	this.waitforclicks = waitforclicks;
        sticks = new String[seats];
        owners = new Philosopher[seats];
        philosophers = new Philosopher[seats];
        
        for (int i = 0; i < seats; i++) {
            sticks[i] = "Stick" + (i + 1);
        }
        
        this.waitforclicks = false;
    }
    
	public void addPhilosopher(int no, Philosopher p)
	{
		philosophers[no] = p;
	}
	
	public Philosopher getPhilosopher(int no)
	{
		return philosophers[no];
	}
	
	public void getLeftStick(int no)
	{
		getStick(no);
	}
	
	public void getRightStick(int no)
	{
		getStick(no==0? sticks.length-1: no-1);
	}
	
	/**
	 *  Implement me
	 */
	public void getStick(int no)
	{
		// grab the stick (or wait for it if not available)
		// save the current owner in the owners array (use getPhilosopherByThread to fetch the philosopher of the invocation)
		// note: think about synchronization. Which object you should use to ensure functioning if multiple philosophers call at the same time 
		
		synchronized(sticks[no])
		{
			Philosopher caller = getCurrentPhilosopher();
			
			while(getStickOwner(no)!=null)
			{
				try
				{
					sticks[no].wait();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			
			owners[no] = caller;
		}
	}
	
	public void releaseLeftStick(int no)
	{
		releaseStick(no);
	}
	
	public void releaseRightStick(int no)
	{
		releaseStick(no==0? sticks.length-1: no-1);
	}
	
	/**
	 *  Implement me
	 */
	public void releaseStick(int no)
	{
		// release the stick if the calling thread (philosoph) is the owner of the stick
		// notify other philosoph possibly already waiting for the stick
		// note: think about synchronization. You should use the same synchronization object as in getStick()
		
		synchronized(sticks[no])
		{
			owners[no] = null;
			
			sticks[no].notify();
		}
	}
	
	public Philosopher getCurrentPhilosopher()
	{
		Philosopher ret = null;
		
		Thread t = Thread.currentThread();
		
		for(Philosopher p: philosophers)
		{
			if(p.getMyThread().equals(t))
			{
				ret = p;
				break;
			}
		}
		if(ret==null)
			throw new RuntimeException("Philosopher not found: "+t);
		
		return ret;
	}
	
	public Philosopher getStickOwner(int no)
	{
		return owners[no];
	}
	
	public boolean isWaitForClicks()
	{
		return waitforclicks;
	}
	
	public void invertWaitForClicks()
	{
		this.waitforclicks = !waitforclicks;
	}
	
	public void notifyAllPhilosophers()
	{
		for(Philosopher p: philosophers)
		{
			p.notifyPhilosopher((long)(Math.random()*2000));
			//p.notifyPhilosopher(100);
		}
	}

	public String toString()
	{
		return Arrays.toString(owners);
	}
}
