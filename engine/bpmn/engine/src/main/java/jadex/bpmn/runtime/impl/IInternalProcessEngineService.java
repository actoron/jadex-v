package jadex.bpmn.runtime.impl;

import java.util.Map;

import jadex.common.IResultCommand;
import jadex.common.UnparsedExpression;
import jadex.future.IFuture;

// todo!!!

/**
 *  Services to be called from BPMN process instances
 *  to some super-ordinated process engine, if any.
 */
//@Service(system=true)
public interface IInternalProcessEngineService
{
	/**
	 *  Register an event description to be notified, when the event happens.
	 *  @return An id to be used for deregistration.
	 */
	public IFuture<String>	addEventMatcher(String[] eventtypes, UnparsedExpression expression, String[] imports, 
		Map<String, Object> params, boolean remove, IResultCommand<IFuture<Void>, Object> command);
	
	/**
	 *  Register an event description to be notified, when the event happens.
	 */
	public IFuture<Void>	removeEventMatcher(String id);
}
