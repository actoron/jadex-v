package jadex.micro.house_monitoring;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.quartz.CronExpression;

import jadex.core.ChangeEvent;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.injection.annotation.Inject;
import jadex.micro.llmcall2.ChatFragment;
import jadex.micro.llmcall2.ILlmChatService;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.providedservice.IService;
import jadex.providedservice.impl.service.ServiceCall;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  The rule system component, which is responsible for storing and evaluating rules in the smart home.
 */
public class RuleSystem	implements IRuleSystemService
{
	//-------- attributes --------
	
	/** The rule counter for ID generation. */
	protected int rule_cnt = 0;
	
	/** The component. */
	@Inject
	protected IComponent	comp;
	
	/** The registered rules, mapped by event type and source. */
	protected Map<EventType, List<Rule>>	rules = new LinkedHashMap<>();
	
	/** The subscribers to the registered rules. */
	protected List<SubscriptionIntermediateFuture<ChangeEvent>>	subscribers = new ArrayList<>();
	
	/** The current LLM call, if any.*/
	protected IFuture<Void>	current_call = IFuture.DONE;
	
	//-------- tool methods --------
	
	@Override
	public IFuture<String> createRule(EventType type, String source, String prompt)
	{
		if(type==EventType.MOTION_DETECTED)
		{
			return comp.getFeature(IRequiredServiceFeature.class).searchServices(IMotionSensorService.class)
				.thenApply(services ->
			{
				boolean found = services.stream().anyMatch(service ->
					((IService)service).getServiceId().getProviderId().getLocalName().matches(source));
				if(found)
				{
					Rule rule = new Rule("rule_"+(++rule_cnt), null, type, source, prompt);
					rules.computeIfAbsent(type, v -> new ArrayList<>()).add(rule);
					for(SubscriptionIntermediateFuture<ChangeEvent> subscriber : subscribers)
					{
						try
						{
							subscriber.addIntermediateResult(new ChangeEvent(ChangeEvent.Type.ADDED, "rules", rule, null, null));
						}
						catch(Exception e)
						{
							System.err.println("Failed to notify subscriber: "+e);
						}
					}
					return rule.id();
				}
				else
				{
					throw new IllegalArgumentException("No motion sensor found with name: "+source+"\nAvailable motion sensors: "+services.stream()
						.map(service -> ((IService)service).getServiceId().getProviderId().getLocalName())
						.reduce((a,b) -> a+", "+b).orElse("none"));
				}
			});
		}
		else
		{
			// For simplicity, we only allow rules for motion sensors in this example.
			throw new UnsupportedOperationException("Only motion sensor rules are supported in this example.");
		}
	}
	
	@Override
	public IFuture<String> createRule(String cron_expression, String prompt)
	{
		try
		{
			// Create cron expression to validate it.
			CronExpression cron	= new CronExpression(cron_expression);
			
			// Add the rule to the list of rules. 
			Rule rule = new Rule("rule_"+(++rule_cnt), cron_expression, null, null, prompt);
			rules.computeIfAbsent(null, v -> new ArrayList<>()).add(rule);
			for(SubscriptionIntermediateFuture<ChangeEvent> subscriber : subscribers)
			{
				try
				{
					subscriber.addIntermediateResult(new ChangeEvent(ChangeEvent.Type.ADDED, "rules", rule, null, null));
				}
				catch(Exception e)
				{
					System.err.println("Failed to notify subscriber: "+e);
				}
			}
			
			// Schedule the rule execution according to the cron expression.
			@SuppressWarnings("unchecked")
			Consumer<Void>[]	execute	= new Consumer[1];
			execute[0]	= v ->
			{
				long	current_time	= comp.getFeature(IExecutionFeature.class).getTime();
				long	next_time	= cron.getNextValidTimeAfter(new Date(current_time)).getTime();
				System.out.println("Scheduling "+rule.id()+" to run in "+(next_time-current_time)+" ms");
				comp.getFeature(IExecutionFeature.class).waitForDelay(next_time - current_time)
					.then(v1 -> executePrompt(rule.prompt())
						.then(v2 -> execute[0].accept(null)));
			};
			execute[0].accept(null);
			
			return new Future<>(rule.id());
		}
		catch(ParseException e)
		{
			return new Future<>(e);
		}
	}
	
	@Override
	public IFuture<Void> deleteRule(String id)
	{
		for(List<Rule> lrules : rules.values())
		{
			for(Rule rule : lrules)
			{
				if(rule.id().equals(id))
				{
					for(SubscriptionIntermediateFuture<ChangeEvent> subscriber : subscribers)
					{
						try
						{
							subscriber.addIntermediateResult(new ChangeEvent(ChangeEvent.Type.REMOVED, "rules", rule, null, null));
						}
						catch(Exception e)
						{
							System.err.println("Failed to notify subscriber: "+e);
						}
					}
					return IFuture.DONE;
				}
			}
		}
		return new Future<>(new IllegalArgumentException("No rule found with ID: "+id));
	}
	
	@Override
	public IFuture<List<Rule>> listRules()
	{
		return new Future<>(rules.values().stream().flatMap(List::stream).toList());
	}
	
	//-------- non-tool methods, i.e. for inter-service calls --------
	
	@Override
	public IFuture<Void> notifyEvent(EventType type, String data)
	{
		String	source	= ServiceCall.getCurrentInvocation().getCaller().getLocalName();
		List<Rule>	matches	= rules.getOrDefault(type, Collections.emptyList()).stream()
			.filter(rule -> source.matches(rule.source()))
			.toList();
		
		if(!matches.isEmpty())
		{
			IFuture<Void>	ret	= IFuture.DONE;
			for(Rule rule : matches)
			{
				String	prompt = "Event "+type+" from source "+source+" occurred with data "+data+".\n"
					+ "Triggering "+rule.id()+":\n"
					+ rule.prompt();
				ret	= executePrompt(prompt);
			}
			return ret;
		}
		else
		{
			return IFuture.DONE; // No rules to trigger, just return.
		}
	}
	
	@Override
	public IFuture<Void> executePrompt(String prompt)
	{
		System.out.println("User: "+prompt);
		
		// If there is an ongoing call, we chain the new calls to it, otherwise we start a new chain.
		current_call = current_call!=null ? current_call : IFuture.DONE;
		current_call = current_call.thenCompose(v ->
			comp.getFeature(IRequiredServiceFeature.class).searchService(ILlmChatService.class)
				.thenCompose(llmChat -> 
			{
				ITerminableIntermediateFuture<ChatFragment>	fut	= llmChat.chat(prompt);
				LlmChatAgent.printResults(fut);
				return fut.thenApply(fragments -> null);
			}));
		return current_call;
	}
	
	//-------- UI only methods --------
	
	@Override
	public ISubscriptionIntermediateFuture<ChangeEvent> subscribeToRules()
	{
		SubscriptionIntermediateFuture<ChangeEvent> subscriber = new SubscriptionIntermediateFuture<>();
		subscriber.setTerminationCommand(ex -> subscribers.remove(subscriber));
		subscribers.add(subscriber);
		
		// Emit all existing rules as "new" rules to the new subscriber.
		rules.values().stream().flatMap(List::stream).forEach(rule ->
			subscriber.addIntermediateResult(new ChangeEvent(ChangeEvent.Type.INITIAL, "rules", rule, null, null)));
		
		return subscriber;
	}
}
