package jadex.enginecore.service.types.ecarules;

import jadex.enginecore.service.annotation.Service;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.rules.eca.IRule;

/**
 * 
 */
@Service
public interface IRulebaseService
{
	/**
	 *  Add a new rule.
	 *  @param rule The rule.
	 */
	public IFuture<Void> addRule(IRule<?> rule);
	
	/**
	 *  Remove a rule.
	 *  @param rule The rule.
	 */
	public IFuture<Void> removeRule(String rulename);
	
	/**
	 *  Subscribe to rule base changes.
	 */
	public ISubscriptionIntermediateFuture<IRulebaseEvent> subscribeToRulebase();
}
