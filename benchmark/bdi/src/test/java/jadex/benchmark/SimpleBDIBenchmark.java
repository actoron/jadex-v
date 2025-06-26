package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;

public class SimpleBDIBenchmark
{
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			SimpleBDIBenchmarkAgent	pojo	= new SimpleBDIBenchmarkAgent();
			IComponentHandle	agent	= IComponentManager.get().create(pojo).get();
			pojo.inited.get();
			return () -> agent.terminate().get();
		});
	}
	
	@Test
	void	benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			SimpleBDIBenchmarkAgent	pojo	= new SimpleBDIBenchmarkAgent();
			IComponentHandle	agent	= IComponentManager.get().create(pojo).get();
			pojo.inited.get();
			agent.terminate().get();
		});
	}

	public static void	main(String[] args)
	{
		for(int i=0; i<10000; i++)
		{
			SimpleBDIBenchmarkAgent	pojo	= new SimpleBDIBenchmarkAgent();
			IComponentManager.get().create(pojo).get();
			pojo.inited.get();
		}
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
