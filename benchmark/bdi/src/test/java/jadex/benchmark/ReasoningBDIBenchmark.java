package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IExternalAccess;

public class ReasoningBDIBenchmark
{
	@Test
	void	benchmarkMemory()
	{
		double pct	= BenchmarkHelper.benchmarkMemory(() -> 
		{
			ReasoningBDIBenchmarkAgent	pojo	= new ReasoningBDIBenchmarkAgent();
			IExternalAccess	agent	= IBDIAgent.create(pojo);
			pojo.completed.get();
			return () -> agent.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
	
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			ReasoningBDIBenchmarkAgent	pojo	= new ReasoningBDIBenchmarkAgent();
			IExternalAccess	agent	= IBDIAgent.create(pojo);
			pojo.completed.get();
			agent.terminate().get();
		});
		assertTrue(pct<20, ">20%: "+pct);	// Fail when more than 20% worse
	}
}
