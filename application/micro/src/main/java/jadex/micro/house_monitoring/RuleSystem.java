package jadex.micro.house_monitoring;

import java.util.LinkedHashMap;
import java.util.Map;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.providedservice.IService;
import jadex.providedservice.impl.service.ServiceCall;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  The rule system component, which is responsible for storing and evaluating rules in the smart home.
 */
public class RuleSystem	implements IRuleSystemService
{
	/** The component. */
	@Inject
	protected IComponent	comp;
	
	/** The registered rules, mapped by event type and source. */
	protected Map<EventType, Map<String, String>>	rules = new LinkedHashMap<>();
	
	@Override
	public IFuture<Void> createRule(EventType type, String source, String prompt)
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
					rules.computeIfAbsent(type, t -> new LinkedHashMap<>()).put(source, prompt);
					return null;
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
	public IFuture<Void> notifyEvent(EventType type, String data)
	{
		String	source	= ServiceCall.getCurrentInvocation().getCaller().getLocalName();
		rules.getOrDefault(type, Map.of()).entrySet().stream()
			.filter(entry -> source.matches(entry.getKey()))
			.map(Map.Entry::getValue)
			.forEach(prompt ->
			{
				// In a real implementation, the prompt would be sent to an LLM for processing.
				System.out.println("Executing rule for event "+type+" from source "+source+": "+prompt);
			});
		return IFuture.DONE;
	}
}
