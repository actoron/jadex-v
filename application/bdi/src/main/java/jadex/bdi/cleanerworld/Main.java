package jadex.bdi.cleanerworld;

import jadex.bdi.cleanerworld.cleaner.CleanerAgent;
import jadex.bdi.cleanerworld.environment.CleanerworldEnvironment;
import jadex.bdi.cleanerworld.ui.EnvGui;
import jadex.core.Application;
import jadex.environment.Environment;

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
		int fps = 30; // steps / frames per second
		Application	app = new Application("Cleanerworld");
		
		CleanerworldEnvironment env = app.create(new CleanerworldEnvironment(fps)).get().getPojoHandle(CleanerworldEnvironment.class);
		env.createWorld().get();
		String envid = Environment.add(env);
		
		app.create(new CleanerAgent(envid), "BDI Cleaner");
		
		//EnvironmentGui.create(envid); // old Swing ui
		EnvGui.create(envid); // new libgdx ui
		
		app.waitForLastComponentTerminated();
	}
}