package jadex.micro.house_monitoring;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 *  This service represents the rule system in the smart home, which is responsible for storing and evaluating rules.
 */
@Service
public interface IRuleSystemService
{
	/**  The possible event types that can trigger rules. */
	public enum EventType
	{
		DOORBELL_PRESSED,
		MOTION_DETECTED,
		TIMER_EXPIRED
	}
	
	//-------- tool methods, i.e. visible to the LLM --------
	
	/**
	 *  Register an LLM prompt that is triggered by the given event type.
	 */
	@Tool("Register a rule for an LLM prompt to be executed later, when the given event occurs and the source matches.")
	public IFuture<Void>	createRule(
		@P("The event type") EventType type,
		@P("The event source, i.e. the name of the component.") String source,
		@P("The LLM prompt to be executed") String prompt);
	
	//-------- UI only methods --------
	
	/**
	 *  Notify the rule system of an event.
	 *  @param type	The event type.
	 *  @param data	Optional event data, e.g. for a doorbell event the name of the doorbell.
	 */
	public IFuture<Void>	notifyEvent(EventType type, String data);
}
