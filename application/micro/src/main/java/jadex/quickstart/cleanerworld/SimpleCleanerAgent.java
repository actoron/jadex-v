package jadex.quickstart.cleanerworld;

import jadex.mj.core.annotation.OnStart;
import jadex.quickstart.cleanerworld.environment.SensorActuator;
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
}
