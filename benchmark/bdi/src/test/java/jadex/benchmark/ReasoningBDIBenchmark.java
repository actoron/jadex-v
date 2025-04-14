package jadex.benchmark;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;

public class ReasoningBDIBenchmark
{
	@Test
	void	benchmarkMemory()
	{
		 BenchmarkHelper.benchmarkMemory(() -> 
		{
			ReasoningBDIBenchmarkAgent	pojo	= new ReasoningBDIBenchmarkAgent();
			IComponentHandle	agent	= IComponentManager.get().create(pojo).get();
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
			IComponentHandle	agent	= IComponentManager.get().create(pojo).get();
			pojo.completed.get();
			agent.terminate().get();
		});
	}

	public static void main(String[] args)
	{
		for(;;)
		{
			ReasoningBDIBenchmarkAgent	pojo	= new ReasoningBDIBenchmarkAgent();
			IComponentHandle	agent	= IComponentManager.get().create(pojo).get();
			pojo.completed.get();
			agent.terminate().get();
		}
	}
}
