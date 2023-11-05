package jadex.benchmark;

public class ReasoningBDIBenchmark	extends AbstractBDIBenchmark
{
	// Corresponding BDI agent is in testFixtures so class is not loaded by JUnit to check for tests.
	@Override
	protected String getClassname()
	{
		return "jadex.benchmark.ReasoningBDI";
	}
	
	@Override
	protected String getComponentTypeName()
	{
		return "Reasoning BDI";
	}
}
