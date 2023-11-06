package jadex.quickstart.cleanerworld;

import jadex.execution.LambdaAgent;
import jadex.quickstart.cleanerworld.environment.SensorActuator;
import jadex.quickstart.cleanerworld.gui.SensorGui;

/**
 *  Agent without POJO class, just lambda body.
 */
public class SimpleCleanerAgentLambda
{
	/**
	 *  Start the agent
	 */
	public static void main(String[] args)
	{
		LambdaAgent.create(() ->
		//SComponentFactory.create(() ->
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
		});
	}
}
