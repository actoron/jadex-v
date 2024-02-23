package jadex.bdi.puzzle;

import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponent;

/**
 *  Main for starting the example programmatically.
 */
public class MainBenchmark
{
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		IBDIAgent.create("jadex.bdi.puzzle.BenchmarkAgent");
		IComponent.waitForLastComponentTerminated();
	}
}
