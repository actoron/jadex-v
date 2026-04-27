package jadex.micro.house_monitoring;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IRuleSystemService
{
	public enum EventType
	{
		DOORBELL_PRESSED,
		MOTION_DETECTED,
		TIMER_EXPIRED
	}
	
	/**
	 *  Notify the rule system of an event.
	 *  @param type	The event type.
	 *  @param data	Optional event data, e.g. for a doorbell event the name of the doorbell.
	 */
	public default IFuture<Void>	notifyEvent(EventType type, String data)
	{
		System.out.println("notify Event: "+type+" ("+data+")");
		return IFuture.DONE;
	}
	
	/**
	 *  Register an LLM prompt that is triggered by the given event type.
	 */
	@Tool("Register a rule for an LLM prompt to be executed later, when the given event occurs.")
	public default IFuture<Void>	createRule(@P("The event type") EventType type, @P("The LLM prompt to be executed") String prompt)
	{
		System.out.println("create Rule: "+type+" -> "+prompt);
		return IFuture.DONE;
	}
}
