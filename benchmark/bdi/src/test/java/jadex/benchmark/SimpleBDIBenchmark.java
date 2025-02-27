package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponentHandle;

public class SimpleBDIBenchmark
{
	@Test
	void	benchmarkMemory()
	{
		BenchmarkHelper.benchmarkMemory(() -> 
		{
			SimpleBDIBenchmarkAgent	pojo	= new SimpleBDIBenchmarkAgent();
			IComponentHandle	agent	= IBDIAgent.create(pojo);
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
			IComponentHandle	agent	= IBDIAgent.create(pojo);
			pojo.inited.get();
			agent.terminate().get();
		});
	}
}
