package jadex.benchmark;

public class SimpleBDIBenchmark	extends AbstractBDIBenchmark
{
	// Corresponding BDI agent is in testFixtures so class is not loaded by JUnit to check for tests.
	@Override
	protected String getClassname()
	{
		return "jadex.benchmark.bdi.SimpleBDI";
	}
	
	@Override
	protected String getComponentTypeName()
	{
		return "Simple BDI";
	}
}
