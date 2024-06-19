package jadex.benchmark;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IExternalAccess;

public class SimpleBDIBenchmark
{
	@Test
	void	benchmarkTime()
	{
		double pct	= BenchmarkHelper.benchmarkTime(() -> 
		{
			SimpleBDIBenchmarkAgent	pojo	= new SimpleBDIBenchmarkAgent();
			IExternalAccess	agent	= IBDIAgent.create(pojo);
			pojo.inited.get();
			agent.terminate().get();
		});
		assertTrue(pct<20);	// Fail when more than 20% worse
	}
}
