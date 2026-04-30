package jadex.micro.house_monitoring;

import java.util.List;

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
		MOTION_DETECTED
	}
	
	/** Record to hold rule information. */
	public record Rule(String rule_id, String cron_expression, EventType type, String source, String prompt) {}
	
	//-------- tool methods, i.e. visible to the LLM --------
	
	/**
	 *  Register an LLM prompt that is triggered by the given event type.
	 */
	@Tool("Register a rule for an LLM prompt to be executed later, when the given event occurs and the source matches.\n"
		+ "The tool returns the rule_id of the created rule, which can be used to delete the rule later.")
	public IFuture<String>	createEventRule(
		@P("The event type") EventType type,
		@P("The event source, i.e. the name of the component.") String source,
		@P("The LLM prompt to be executed") String prompt);
	
	/**
	 *  Register an LLM prompt that is scheduled using a cron expression.
	 */
	@Tool("Register a rule for an LLM prompt to be executed repeatedly according to the Quartz cron expression.\n"
		+ "The tool returns the rule_id of the created rule, which can be used to delete the rule later.")
	public IFuture<String>	createCronRule(
		@P("The Quartz cron expression (seconds minutes hours day_of_month month day_of_week [year])."
		// qwen3.5:4b performs better without the example expression (!?)
//			+ ", e.g. '0 0/5 * ? * * *' for every full 5 minutes."
		) String cron_expression,
		@P("The LLM prompt to be executed") String prompt);
	

	/**
	 *  Delete a rule by its ID.
	 */
	@Tool("Delete a rule by its rule_id.")
	public IFuture<Void>	deleteRule(String rule_id);
	
	/**
	 *  List all registered rules.
	 */
	@Tool("List all registered rules.")
	public IFuture<List<Rule>>	listRules();
	
	//-------- non-tool methods, i.e. for inter-service calls --------
	
	/**
	 *  Notify the rule system of an event.
	 *  @param type	The event type.
	 *  @param data	Optional event data, e.g. for a doorbell event the name of the doorbell.
	 */
	public IFuture<Void>	notifyEvent(EventType type, String data);
	
	/**
	 *  Execute a prompt.
	 *  If a prompt is currently being executed, the new execution will wait until the current execution is finished,
	 *  to avoid concurrent executions of prompts.
	 */
	public IFuture<Void>	executePrompt(String prompt);
	
	//-------- UI only methods --------
	
	/**
	 *  Subscribe to the registered rules, e.g. to display them in the UI.
	 *  Values emitted by the subscription are of type {@link Rule},
	 *  but wrapped in a {@link ChangeEvent} to indicate whether they are new rules or deleted rules.
	 */
	public ISubscriptionIntermediateFuture<ChangeEvent>	subscribeToRules();
}
