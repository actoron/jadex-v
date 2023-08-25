package jadex.mj.feature.simulation.impl;

import java.util.PriorityQueue;
import java.util.Queue;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.feature.execution.impl.MjExecutionFeature;

/**
 *  The master simulation feature does the actual work
 *  and holds the step queue and timer entries for all
 *  components of a simulation.
 */
public class MjMasterSimulationFeature	extends MjExecutionFeature
{
	public void scheduleStep(MjExecutionFeature exe, Runnable r)
	{
		super.scheduleStep(() ->
		{
			try
			{
				LOCAL.set(exe);
				r.run();
			}
			finally
			{
				LOCAL.remove();
			}
		});
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
		
		boolean	maybeidle	= false;
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
			
			if(!running)
			{
				maybeidle	= true;
			}
		}
		
		if(maybeidle)
		{
			mayBeIdle();
		}
		
		return  ret;
	}
	
	@Override
	public long getTime()
	{
		return current_time;
	}
	
	@Override
	protected void mayBeIdle()
	{
		TimerEntry	next	= null;
		synchronized(this)
		{
			next	= timer_entries.poll();
		}

		if(next!=null)
		{
			current_time	= next.time;
			scheduleStep(next);
		}
	}
}
