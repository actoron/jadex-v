package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponentHandle;

public class ReasoningBDIBenchmark
{
	@Test
	void	benchmarkMemory()
	{
		 BenchmarkHelper.benchmarkMemory(() -> 
		{
			ReasoningBDIBenchmarkAgent	pojo	= new ReasoningBDIBenchmarkAgent();
			IComponentHandle	agent	= IBDIAgent.create(pojo);
			pojo.completed.get();
			return () -> agent.terminate().get();
		});
	}
	
	@Test
	void	benchmarkTime()
	{
		BenchmarkHelper.benchmarkTime(() -> 
		{
			ReasoningBDIBenchmarkAgent	pojo	= new ReasoningBDIBenchmarkAgent();
			IComponentHandle	agent	= IBDIAgent.create(pojo);
			pojo.completed.get();
			agent.terminate().get();
		});
	}
}
