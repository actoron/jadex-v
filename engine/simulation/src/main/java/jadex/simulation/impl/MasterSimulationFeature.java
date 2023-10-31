package jadex.simulation.impl;

import java.util.PriorityQueue;
import java.util.Queue;

import jadex.feature.execution.impl.ExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.simulation.ISimulationFeature;

/**
 *  The master simulation feature does the actual work
 *  and holds the step queue and timer entries for all
 *  components of a simulation.
 */
public class MasterSimulationFeature	extends ExecutionFeature	implements ISimulationFeature
{
	/** Flag indicating that the simulation is active. */
	protected boolean	simulating	= true;
	
	/** Inform stop() callers when simulation stops (if any). */
	protected Future<Void>	stopping	= null;
	
	public void scheduleStep(ExecutionFeature exe, Runnable r)
	{
		super.scheduleStep(new StepInfo(exe, r));
	}
	
	protected Queue<TimerEntry>	timer_entries	= new PriorityQueue<>();
	protected long	current_time	= System.currentTimeMillis();
	
	protected abstract static class TimerEntry	implements Runnable, Comparable<TimerEntry>
	{
		protected long	time;
		
		public TimerEntry(long time)
		{
			this.time	= time;
		}
		
		@Override
		public int compareTo(TimerEntry other)
		{
			return (int)(this.time - other.time);
		}
	}
	
	@Override
	public IFuture<Void> waitForDelay(long millis)
	{
		Future<Void>	ret	= new Future<>();
		
		synchronized(this)
		{
			timer_entries.offer(new TimerEntry(current_time+millis)
			{
				@Override
				public void run()
				{
					ret.setResult(null);
				}
			});
			
			// When executor is not running (i.e. no active components)
			// it needs to be restarted by scheduling the next timepoint. 
			if(!executing)
			{
				idle();
			}
		}
		
		return  ret;
	}
	
	@Override
	public long getTime()
	{
		return current_time;
	}
	
	@Override
	public void setTime(long millis)
	{
		synchronized(this)
		{
			current_time	= millis;
		}
	}
	
	@Override
	public void start()
	{
		synchronized(this)
		{
			if(simulating)
			{
				throw new IllegalStateException("Simulation already running.");
			}
			else
			{
				simulating	= true;
				// Restart execution by scheduling next time point (if any).
				idle();
			}
		}
	}
	
	@Override
	public IFuture<Void>	stop()
	{
		IFuture<Void>	ret;
		synchronized(this)
		{
			if(!simulating)
			{
				throw new IllegalStateException("Simulation not running.");
			}
			// stop() was already called and did not finish yet
			else if(stopping!=null)
			{
				ret	= stopping;
			}
			// simulation is idle -> stop immediately
			else if(!executing)
			{
				simulating	= false;
				ret	= IFuture.DONE;
			}
			// components are still executing -> stop in next idle
			else
			{
				stopping	= new Future<>();
				ret	= stopping;
			}
		}
		return ret;
	}
	
	@Override
	// Called when all components have finished executing
	protected void idle()
	{
		Future<Void>	stop	= null;
		TimerEntry	next	= null;
		synchronized(this)
		{
			if(simulating && busy==0)
			{
				if(stopping!=null)
				{
					stop	= stopping;
					simulating	= false;
					stopping	= null;
				}
				else
				{
					next	= timer_entries.poll();
					if(next!=null && next.time>current_time)
					{
						// Outdated entries might have older time (e.g. when setTime() was used to fast-forward)
						current_time	= next.time;
					}
				}
			}
		}
		
		if(stop!=null)
		{
			stop.setResult(null);
		}
		else if(next!=null)
		{
			scheduleStep(next);
		}
	}

	/** Count busy components when in parallel mode. */
	protected volatile int	busy	= 0;
	
	protected void componentBusy()
	{
		synchronized(this)
		{
			busy++;
		}
	}

	protected void componentIdle()
	{
		synchronized(this)
		{
			if(--busy==0)
			{
				idle();
			}
		}
	}
	
	// Only used in single-thread simulation
	static class StepInfo	implements Runnable
	{
		Runnable	step;
		ExecutionFeature	exe;
		
		public StepInfo(ExecutionFeature exe, Runnable step)
		{
			this.exe	= exe;
			this.step	= step;
		}

		@Override
		public void run()
		{
			LOCAL.set(exe);
			exe.doRun(step);
		}
	}
}
