package jadex.quickstart.cleanerworld;

import jadex.core.IComponentManager;
import jadex.injection.annotation.OnStart;
import jadex.quickstart.cleanerworld.environment.SensorActuator;
import jadex.quickstart.cleanerworld.gui.EnvironmentGui;
import jadex.quickstart.cleanerworld.gui.SensorGui;

/**
 *  Simple cleaner with a main loop for moving randomly.
 */
public class SimpleCleanerAgent
{
	//-------- simple example behavior --------
	
	/**
	 *  The body is executed when the agent is started.
	 */
	@OnStart
	private void	exampleBehavior()
	{
		// Create the sensor/actuator interface object.
		SensorActuator	actsense	= new SensorActuator();
		
		// Open a window showing the agent's perceptions
		new SensorGui(actsense).setVisible(true);

		// Agent uses one main loop for its random move behavior
		while(true)
		{
			actsense.moveTo(Math.random(), Math.random());
		}
	}

	/**
	 *  Main method for starting the scenario.
	 *  @param args	ignored for now.
	 */
	public static void main(String[] args)
	{
		// Start an agent
		IComponentManager.get().create(new SimpleCleanerAgent());
		
		// Open the world view
		EnvironmentGui.create();
	}
}
