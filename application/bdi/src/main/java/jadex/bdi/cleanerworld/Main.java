package jadex.bdi.cleanerworld;

import jadex.bdi.cleanerworld.cleaner.CleanerAgent;
import jadex.bdi.cleanerworld.environment.Chargingstation;
import jadex.bdi.cleanerworld.environment.CleanerworldEnvironment;
import jadex.bdi.cleanerworld.environment.Waste;
import jadex.bdi.cleanerworld.environment.Wastebin;
import jadex.bdi.cleanerworld.ui.EnvironmentGui;
import jadex.bdi.runtime.IBDIAgent;
import jadex.core.IComponentManager;
import jadex.environment.Environment;
import jadex.math.Vector2Double;

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
		CleanerworldEnvironment env = IComponentManager.get().create(new CleanerworldEnvironment(5)).get().getPojoHandle(CleanerworldEnvironment.class);
		String envid = Environment.add(env);
		
		IBDIAgent.create(new CleanerAgent(envid));
		
		env.addSpaceObject(new Waste(new Vector2Double(0.1, 0.5)));
		env.addSpaceObject(new Waste(new Vector2Double(0.2, 0.5)));
		env.addSpaceObject(new Waste(new Vector2Double(0.3, 0.5)));
		env.addSpaceObject(new Waste(new Vector2Double(0.9, 0.9)));
		env.addSpaceObject(new Wastebin(new Vector2Double(0.2, 0.2), 20));
		env.addSpaceObject(new Wastebin(new Vector2Double(0.8, 0.1), 20));
		env.addSpaceObject(new Chargingstation(new Vector2Double(0.775, 0.775)));
		env.addSpaceObject(new Chargingstation(new Vector2Double(0.15, 0.4)));
		
		EnvironmentGui.create(envid);
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}