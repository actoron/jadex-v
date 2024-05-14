package jadex.quickstart.cleanerworld;

import jadex.bdi.runtime.IBDIAgent;
import jadex.quickstart.cleanerworld.gui.EnvironmentGui;
import jadex.quickstart.cleanerworld.single.CleanerBDIAgentB3;

/**
 *  Main class for starting a cleaner-world scenario
 */
public class Main
{
	/**
	 *  Main method for starting the scenario.
	 *  @param args	ignored for now.
	 */
	public static void main(String[] args)
	{
		// Start an agent
//		IBDIAgent.create(new CleanerBDIAgentA4());
		IBDIAgent.create(new CleanerBDIAgentB3());
//		IBDIAgent.create("jadex.quickstart.cleanerworld.single.CleanerBDIAgentE1");
		
		// Open the world view
		EnvironmentGui.create();
	}
}
