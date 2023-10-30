package jadex.micro.example.helloworld;

import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.micro.annotation.Agent;
import jadex.mj.model.annotation.OnEnd;
import jadex.mj.model.annotation.OnStart;

/**
 *  Agent creation benchmark. 
 */
@Agent
public class AgentCreationAgent 
{
	//-------- attributes --------
	
	/** The agent. */
	@Agent
	protected IComponent agent;
	
	/** The number of agents. */
	protected int max;
	
	/** The agent number. */
	protected int num;
	
	protected Long startmem;
	
	protected Long starttime;
	
	//-------- methods --------
		
	public AgentCreationAgent()
	{
		this(100000);
	}
	
	public AgentCreationAgent(int max)
	{
		this(max, 1, null, null);
	}
	
	private AgentCreationAgent(int max, int num, Long startmem, Long starttime)
	{
		System.out.println("Started: "+num);
		this.max = max;
		this.num = num;
		this.startmem = startmem==null? startmem = Long.valueOf(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()): startmem;
		this.starttime = starttime==null? starttime = Long.valueOf(System.currentTimeMillis()): starttime;
	}
	
	/**
	 *  Execute an agent step.
	 */
	@OnStart
	public void onStart()
	{
		if(num<max)
		{
			IComponent.create(new AgentCreationAgent(max, num+1, startmem, starttime), new ComponentIdentifier(num+1+""));
		}
		else
		{
			final long end = System.currentTimeMillis();
			
			System.gc();
			try
			{
				Thread.sleep(500);
			}
			catch(InterruptedException e){}
			final long used = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
			final long omem = (used-startmem.longValue())/1024;
			final double upera = ((long)(1000*(used-startmem.longValue())/max/1024))/1000.0;
			System.out.println("Overall memory usage: "+omem+"kB. Per agent: "+upera+" kB.");
			System.out.println("Last peer created. "+max+" agents started.");
			final double dur = ((double)end-starttime.longValue())/1000.0;
			final double pera = dur/max;
			System.out.println("Needed: "+dur+" secs. Per agent: "+pera+" sec. Corresponds to "+(1/pera)+" agents per sec.");
			
			long killstarttime = System.currentTimeMillis();
			deletePeers(max-1, killstarttime, dur, pera, omem, upera, max);
		}	
	}
	
	@OnEnd
	public void onEnd()
	{
		System.out.println("Terminating: "+num);
	}

	/**
	 *  Delete all peers from last-1 to first.
	 *  @param cnt The highest number of the agent to kill.
	 */
	protected void deletePeers(int cnt, long killstarttime, double dur, double pera, long omem, double upera, int max)
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
	protected void killLastPeer(final int max, final long killstarttime, final double dur, final double pera, 
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
		
		//agent.getFeature(IArgumentsResultsFeature.class).getResults().put("microcreationtime", new Tuple(""+pera, "s"));
		//agent.getFeature(IArgumentsResultsFeature.class).getResults().put("microkillingtime", new Tuple(""+killpera, "s"));
		//agent.getFeature(IArgumentsResultsFeature.class).getResults().put("micromem", new Tuple(""+upera, "kb"));
		agent.terminate();
	}
	
	/**
	 *  Main for testing.
	 */
	public static void main(String[] args)
	{
		IComponent.create(new AgentCreationAgent(), new ComponentIdentifier("1"));
		
		IComponent.waitForLastComponentTerminated();
	}	
}
