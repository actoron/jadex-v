package jadex.micro.house_monitoring;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jadex.core.ChangeEvent;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
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
	
	/** Record to hold rule information. */
	public record Rule(String id, EventType type, String source, String prompt) {}
	
	//-------- tool methods, i.e. visible to the LLM --------
	
	/**
	 *  Register an LLM prompt that is triggered by the given event type.
	 */
	@Tool("Register a rule for an LLM prompt to be executed later, when the given event occurs and the source matches.\n"
		+ "The tool returns the ID of the created rule, which can be used to delete the rule later.")
	public IFuture<String>	createRule(
		@P("The event type") EventType type,
		@P("The event source, i.e. the name of the component.") String source,
		@P("The LLM prompt to be executed") String prompt);
	
	/**
	 *  Delete a rule by its ID.
	 */
	@Tool("Delete a rule by its ID.")
	public IFuture<Void>	deleteRule(String id);
	
	//-------- non-tool methods, i.e. for inter-service calls --------
	
	/**
	 *  Notify the rule system of an event.
	 *  @param type	The event type.
	 *  @param data	Optional event data, e.g. for a doorbell event the name of the doorbell.
	 */
	public IFuture<Void>	notifyEvent(EventType type, String data);
	
	//-------- UI only methods --------
	
	/**
	 *  Subscribe to the registered rules, e.g. to display them in the UI.
	 *  Values emitted by the subscription are of type {@link Rule},
	 *  but wrapped in a {@link ChangeEvent} to indicate whether they are new rules or deleted rules.
	 */
	public ISubscriptionIntermediateFuture<ChangeEvent>	subscribeToRules();
}
