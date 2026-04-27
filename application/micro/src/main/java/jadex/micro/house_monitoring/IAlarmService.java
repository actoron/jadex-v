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
		ON,
		OFF,
	}
	
	//-------- tool methods, i.e. visible to the LLM --------
	
	/**
	 *  Trigger or disable the alarm.
	 */
	@Tool
	public IFuture<Void> setAlarmState(AlarmState state);
	
	/**
	 *  Get the current state of the alarm system.
	 *  @return The current state of the alarm system.
	 */
	@Tool
	public IFuture<AlarmState> getAlarmState();
	
	//-------- UI only methods --------
	
	/**
	 *  Subscribe to the state of the alarm system.
	 */
	public ISubscriptionIntermediateFuture<AlarmState> subSubcribeToAlarmState();
}
