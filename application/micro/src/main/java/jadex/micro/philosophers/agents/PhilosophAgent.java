package jadex.micro.philosophers.agents;

import java.util.Random;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.micro.philosophers.PhilosopherState;

public class PhilosophAgent implements IPhilosopherService
{
	@Inject
	protected IComponent agent;
	
	protected PhilosopherState state;
	protected Random r = new Random();
	protected int no;
	// TODO: no provide
	@Inject
	protected ITableService t;
	protected int eatcnt;
	protected Future<Void> wait;
	
	public PhilosophAgent(int no) 
	{
		this.no = no;
		this.state = PhilosopherState.THINKING;
	}
	
	@Inject
	protected void foundTable(ITableService table)
	{
		this.t = table;
		t.addPhilosopher(no);
		
		run();
	}
	
	public void run()
	{
		while(true)
		{
			// implement the lifecycle of the philosoph here
			// set the philosophers state and wait when thinking and eating
			// additionally you can wait before trying to fetch the second stick
			
			doSleepRandom();
			
			setState(PhilosopherState.WAITING_FOR_LEFT);
			t.getLeftStick(no).get();
			
			setState(PhilosopherState.WAITING_FOR_RIGHT);
			doSleepRandom();
			t.getRightStick(no).get();

			setState(PhilosopherState.EATING);
			doSleepRandom();

			t.releaseLeftStick(no);
			t.releaseRightStick(no);
			setState(PhilosopherState.THINKING);
			
			eatcnt++;
		}
	}

	public IFuture<PhilosopherState> getState()
	{
		return new Future<PhilosopherState>(state);
	}

	public void setState(PhilosopherState state)
	{
		this.state = state;
	}
	
	public IFuture<Integer> getEatCnt()
	{
		return new Future<Integer>(eatcnt);
	}
	
	public IFuture<Integer> getNo()
	{
		return new Future<Integer>(no);
	}

	protected void doSleepRandom()
	{
		doSleep((int)(Math.random()*2000));
	}
	
	protected void doSleep(long time)
	{
		if(!t.isWaitForClicks().get())
		{
			// implement time wait here 
			agent.getFeature(IExecutionFeature.class).waitForDelay((long)(Math.random()*2000)).get();
		}
		else
		{
			this.wait = new Future<Void>();
			wait.get();
		}
	}
	
	public void notifyPhilosopher(long time)
	{
		if(time>0)
			doSleep(time);
		
		if(wait!=null)
		{
			Future<Void> tmp = wait;
			wait = null;
			tmp.setResult(null);
		}
	}
}