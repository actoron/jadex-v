package jadex.micro.philosophers.thread;

import java.util.Random;

import jadex.micro.philosophers.PhilosopherState;


public class Philosopher implements Runnable
{
	protected PhilosopherState state;
	protected Random r = new Random();
	protected int no;
	protected Table t;
	protected int eatcnt;
	protected Thread mythread;
	
	public Philosopher(int no, Table t) 
	{
		this.no = no;
		this.t = t;
		this.state = PhilosopherState.THINKING;
		t.addPhilosopher(no, this);
	}
	
	/**
	 *  Implement me
	 */
	public void run()
	{
		this.mythread = Thread.currentThread();
		while(true)
		{
			// implement the lifecycle of the philosoph here
			// set the philosophers state and wait when thinking and eating
			// additionally you can wait before trying to fetch the second stick
			
			doSleepRandom(3000);
			
			setState(PhilosopherState.WAITING_FOR_LEFT);
			t.getLeftStick(no);
			
			setState(PhilosopherState.WAITING_FOR_RIGHT);
			doSleepRandom();
			t.getRightStick(no);

			setState(PhilosopherState.EATING);
			doSleepRandom();

			t.releaseLeftStick(no);
			t.releaseRightStick(no);
			setState(PhilosopherState.THINKING);
			
			eatcnt++;
		}
	}

	public PhilosopherState getState()
	{
		return state;
	}

	public void setState(PhilosopherState state)
	{
		this.state = state;
	}
	
	public int getEatCnt()
	{
		return eatcnt;
	}
	
	public Thread getMyThread()
	{
		return mythread;
	}
	
	public int getNo()
	{
		return no;
	}

	protected void doSleepRandom()
	{
		doSleepRandom(2000);
	}
	
	protected void doSleepRandom(long time)
	{
		doSleep((long)(Math.random()*time));
	}
	
	/**
	 *  Implement me
	 */
	protected void doSleep(long time)
	{
		if(!t.isWaitForClicks())
		{
			// implement time wait here (thread sleep)
			try
			{
				//System.out.println("sleep: "+getNo()+" "+time);
				Thread.sleep(time);
			}
			catch(Exception e)
			{
			}
		}
		else
		{
			try
			{
				synchronized(this)
				{
					this.wait();
				}
			}
			catch(InterruptedException e)
			{
			}
		}
	}
	
	public void notifyPhilosopher(long time)
	{
		if(time>0)
			doSleep(time);
		
		try
		{
			synchronized(this)
			{
				this.notify();
			}
		}
		catch(Exception e)
		{
		}
	}
}