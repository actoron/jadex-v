package jadex.micro.house_monitoring;

import dev.langchain4j.agent.tool.Tool;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.providedservice.annotation.Service;

/**
 *  This service represents the alarm system in the smart home. 
 */
@Service
public interface IAlarmService
{
	/**  The possible states of the alarm system. */
	public enum AlarmState
	{
		TRIGGERED,
		SILENT,
	}
	
	//-------- tool methods, i.e. visible to the LLM --------
	
	/**
	 *  Trigger or stop the alarm.
	 */
	@Tool("# Purpose\n"
		+ "Trigger or stop the alarm.\n"
		+ "\n"
		+ "## Usage\n"
		+ "1. Trigger the alarm only if you notice a clear and imminent danger to the house.\n"
		+ "2. Do not set the alarm to TRIGGERED unless you are sure that there is an immediate threat,"
		+ " e.g., you clearly see intruders or suspicious activity around the house.\n"
		+ "3. Setting the state to SILENT will stop an ongoing alarm, if any.\n"
		+ "4. Do not stop an ongoing alarm unless explicitly instructed to do so by the house owner.")
	public IFuture<Void> setAlarmState(AlarmState state);
	
	/**
	 *  Get the current state of the alarm system.
	 *  @return The current state of the alarm system.
	 */
	@Tool("Get the current state of the alarm system.")
	public IFuture<AlarmState> getAlarmState();
	
	//-------- UI only methods --------
	
	/**
	 *  Subscribe to the state of the alarm system.
	 */
	public ISubscriptionIntermediateFuture<AlarmState> subcribeToAlarmState();
}
