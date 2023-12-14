package jadex.micro.example.helloworld;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IThrowingFunction;
import jadex.execution.LambdaAgent;
import jadex.micro.annotation.Agent;

/**
 *  Agent creation benchmark. 
 */
@Agent
public class LambdaCreationAgent 
{
	/**
	 *  Main for testing.
	 */
	public static void main(String[] args)
	{
		createAgent(100000, 1, null, null);
		
		IComponent.waitForLastComponentTerminated();
	}	
	
	protected static void createAgent(int max, int num, Long smem, Long stime)
	{
		Long startmem = smem==null? Long.valueOf(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()): smem;
		Long starttime = stime==null? Long.valueOf(System.currentTimeMillis()): stime;

		LambdaAgent.create(agent ->
		{
			System.out.println("Created agent: "+agent.getId().getLocalName());
			long[] end = new long[1];
			
			if(num<max)
			{
				createAgent(max, num+1, startmem, starttime);
			}
			else
			{
				end[0] = System.currentTimeMillis();
				
				System.gc();
				try
				{
					Thread.sleep(500);
				}
				catch(InterruptedException e){}
				
			
				agent.terminate();
				
				System.out.println("body end: "+agent.getId());
			}	
			return (IThrowingFunction<IComponent, Object>)((a) -> 
			{
				System.out.println("Terminating: "+num);
				
				if(num==max)
				{
					System.out.println("killing started");
					
					long used = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
					long omem = (used-startmem.longValue())/1024;
					double upera = ((long)(1000*(used-startmem.longValue())/max/1024))/1000.0;
					System.out.println("Overall memory usage: "+omem+"kB. Per agent: "+upera+" kB.");
					System.out.println("Last peer created. "+max+" agents started.");
					double dur = ((double)end[0]-starttime.longValue())/1000.0;
					double pera = dur/max;
					System.out.println("Needed: "+dur+" secs. Per agent: "+pera+" sec. Corresponds to "+(1/pera)+" agents per sec.");
					long killstarttime = System.currentTimeMillis();
					
					deletePeers(max-1, killstarttime, dur, pera, omem, upera, max);
				}
				return null;
			});
			
		}, new ComponentIdentifier(""+num));
	}
	
	/**
	 *  Delete all peers from last-1 to first.
	 *  @param cnt The highest number of the agent to kill.
	 */
	protected static void deletePeers(int cnt, long killstarttime, double dur, double pera, long omem, double upera, int max)
	{
		while(cnt>0)
		{
			//System.out.println("Destroying peer: "+cnt);
			ComponentIdentifier aid = new ComponentIdentifier(""+cnt);
			
			IComponent.terminate(aid).get();
			
			if(cnt==1)
			{
				killLastPeer(max, killstarttime, dur, pera, omem, upera);
				break;
			}
			
			cnt--;
		}
	}
	
	/**
	 *  Kill the last peer and print out the results.
	 */
	protected static void killLastPeer(final int max, final long killstarttime, final double dur, final double pera, 
		final long omem, final double upera)
	{
		long killend = System.currentTimeMillis();
		System.out.println("Last peer destroyed. "+(max-1)+" agents killed.");
		double killdur = ((double)killend-killstarttime)/1000.0;
		final double killpera = killdur/(max-1);
		
		Runtime.getRuntime().gc();
		try
		{
			Thread.sleep(500);
		}
		catch(InterruptedException e){}
		long stillused = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024;
		
		System.out.println("\nCumulated results:");
		System.out.println("Creation needed: "+dur+" secs. Per agent: "+pera+" sec. Corresponds to "+(1/pera)+" agents per sec.");
		System.out.println("Killing needed:  "+killdur+" secs. Per agent: "+killpera+" sec. Corresponds to "+(1/killpera)+" agents per sec.");
		System.out.println("Overall memory usage: "+omem+"kB. Per agent: "+upera+" kB.");
		System.out.println("Still used memory: "+stillused+"kB.");
	}
}
