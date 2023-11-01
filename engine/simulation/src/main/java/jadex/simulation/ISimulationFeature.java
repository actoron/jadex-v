package jadex.simulation;

import jadex.execution.IExecutionFeature;
import jadex.future.IFuture;

/**
 *  The simulation feature is an extended execution feature
 *  that provides star/stop/step operations and event-based
 *  simulation.
 */
public interface ISimulationFeature	extends IExecutionFeature
{
	/**
	 *  Set the current time.
	 *  Time entries older than the given time will be executed
	 *  when the simulation is running, but receive the new current time in {@link #getTime()}
	 *  @param millis	The time in milliseconds.
	 */
	public void	setTime(long millis);
	
	/**
	 *  Start the simulation.
	 *  Used to resume a simulation after it was stopped.
	 *  
	 *  @throws IllegalStateException	When the simulation is already running.
	 */
	public void	start();
	
	/**
	 *  Stop the simulation.
	 *  Stops scheduling of due time entries,
	 *  i.e., the time will no longer advance.
	 *  Note, that components will continue to execute as long as
	 *  they have activities for the current time.
	 *  
	 *  @return A future to indicate that simulation has stopped,
	 *  		i.e., all components have finished their activities. 
	 */
	public IFuture<Void> stop();
}
