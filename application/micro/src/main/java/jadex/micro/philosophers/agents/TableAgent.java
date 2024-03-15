package jadex.micro.philosophers.agents;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.providedservice.annotation.Service;
import jadex.providedservice.impl.service.ServiceCall;

@Agent
@Service
public class TableAgent implements ITableService
{
	@Agent
	protected IComponent agent;
	
	/** The chop sticks */
	protected Queue<Future<Void>>[] sticks;
	
	/** The current owners of the sticks (who has stick 1, 2, ...). */
	protected IExternalAccess[] owners;
	
	/** The philosopher that sit at the table. */
	protected IExternalAccess[] philosophers;
	
	/** Wait for times or click events. */
	protected boolean waitforclicks;
	
	public TableAgent(int seats, boolean waitforclicks) 
	{
		this.waitforclicks = waitforclicks;
	    sticks = new Queue[seats];
	    owners = new IExternalAccess[seats];
	    philosophers = new IExternalAccess[seats];
	    
	    for (int i = 0; i < seats; i++) 
	    {
	        sticks[i] = new LinkedList<>();
	    }
	    
	    this.waitforclicks = false;
	}
	
	public IComponent getAgent() 
	{
		return agent;
	}

	public void addPhilosopher(int no)
	{
		ComponentIdentifier cid = ServiceCall.getCurrentInvocation().getCaller();
		philosophers[no] = agent.getExternalAccess(cid);
	}
	
	public IFuture<IExternalAccess> getPhilosopher(int no)
	{
		return new Future<IExternalAccess>(philosophers[no]);
	}
	
	public IFuture<Void> getLeftStick(int no)
	{
		return getStick(no);
	}
	
	public IFuture<Void> getRightStick(int no)
	{
		return getStick(no==0? sticks.length-1: no-1);
	}
	
	/**
	 *  Implement me
	 */
	public IFuture<Void> getStick(int no)
	{
		// grab the stick (or wait for it if not available)
		// save the current owner in the owners array (use getPhilosopherByThread to fetch the philosopher of the invocation)
		// note: think about synchronization. Which object you should use to ensure functioning if multiple philosophers call at the same time 
		
		Future<Void> ret = new Future<Void>();
		
		IExternalAccess caller = getCurrentPhilosopher();
		
		if(getStickOwner(no).get()!=null)
		{
			sticks[no].add(ret);
		}
		else
		{
			owners[no] = caller;
			ret.setResult(null);
		}
		
		return ret;
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
		owners[no] = null;
		if(!sticks[no].isEmpty())
		{
			Future<Void> fut = sticks[no].remove();
			fut.setResult(null);
		}
	}
	
	public IExternalAccess getCurrentPhilosopher()
	{
		ComponentIdentifier cid = ServiceCall.getCurrentInvocation().getCaller();
		return agent.getExternalAccess(cid);
	}
	
	public IFuture<IExternalAccess> getStickOwner(int no)
	{
		return new Future<IExternalAccess>(owners[no]);
	}
	
	public IFuture<Boolean> isWaitForClicks()
	{
		return new Future<Boolean>(waitforclicks);
	}
	
	public void invertWaitForClicks()
	{
		this.waitforclicks = !waitforclicks;
	}
	
	public void notifyAllPhilosophers()
	{
		for(IExternalAccess p: philosophers)
		{
			p.scheduleStep(agent ->
			{
				PhilosophAgent pa = (PhilosophAgent)agent.getPojo();
				//pa.notifyPhilosopher(100);
				pa.notifyPhilosopher((long)(Math.random()*2000));
			});
		}
	}

	public String toString()
	{
		return Arrays.toString(owners);
	}
}
