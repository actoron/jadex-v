package jadex.bdi.cleanerworld;

import jadex.bdi.cleanerworld.cleaner.CleanerAgent;
import jadex.bdi.cleanerworld.environment.CleanerworldEnvironment;
import jadex.bdi.cleanerworld.ui.EnvGui;
import jadex.core.IComponentManager;
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
		
		CleanerworldEnvironment env = IComponentManager.get().create(new CleanerworldEnvironment(fps)).get().getPojoHandle(CleanerworldEnvironment.class);
		env.createWorld().get();
		String envid = Environment.add(env);
		
		IComponentManager.get().create(new CleanerAgent(envid));
		
		//EnvironmentGui.create(envid); // old Swing ui
		EnvGui.create(envid, env.getStepsPerSecond().get()); // new libgdx ui
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}