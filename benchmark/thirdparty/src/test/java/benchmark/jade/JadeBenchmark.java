package benchmark.jade;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jadex.benchmark.BenchmarkHelper;
import jadex.future.Future;

public class JadeBenchmark
{
	@SuppressWarnings("serial")
	public static class BenchmarkAgent	extends Agent
	{
		Future<String>	start;
		Future<String>	stop;
		
		public BenchmarkAgent(Future<String> start, Future<String> stop)
		{
			this.start	= start;
			this.stop	= stop;
		}
		
		@Override
		protected void setup()
		{
			start.setResult(getName());
		}
		
		@Override
		protected void takeDown()
		{
			stop.setResult(getName());
		}
	}
	
	@Test
	public void	benchmarkMemory()
	{
		jade.core.Runtime rt = jade.core.Runtime.instance();
		ContainerController cc = rt.createMainContainer(new ProfileImpl());
		
		BenchmarkHelper.benchmarkMemory(new Callable<Runnable>()
		{
			long cnt	= 0;
			@Override
			public Runnable call() throws Exception
			{
				try
				{
					Future<String>	start	= new Future<>();
					Future<String>	stop	= new Future<>();
					AgentController agent = cc.acceptNewAgent("Agent-"+(++cnt), new BenchmarkAgent(start, stop));
					
					agent.start();
					start.get();
					
					return () ->
					{
						try
						{
							agent.kill();
							stop.get();
						}
						catch(Exception e)
						{
							// Sometimes stale proxy exception!?
							e.printStackTrace();
						}
					};
				}
				catch(Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Test
	public void	benchmarkTime()
	{
		jade.core.Runtime rt = jade.core.Runtime.instance();
		ContainerController cc = rt.createMainContainer(new ProfileImpl());
		
		BenchmarkHelper.benchmarkTime(new Runnable()
		{
			long cnt	= 0;
			@Override
			public void run()
			{
				try
				{
					Future<String>	start	= new Future<>();
					Future<String>	stop	= new Future<>();
					AgentController agent = cc.acceptNewAgent("Agent-"+(++cnt), new BenchmarkAgent(start, stop));
					
					agent.start();
					start.get();
					
					agent.kill();
					stop.get();
				}
				catch(Exception e)
				{
					// Sometimes stale proxy exception!?
					e.printStackTrace();
				}
			}
		});
	}
}
