package jadex.remoteservice.impl.remotecommands;

import jadex.core.impl.Component;
import jadex.providedservice.annotation.Security;

/**
 *  Remote command that can provide custom security settings
 *  for being checked before execution.
 */
public interface ISecuredRemoteCommand
{
	/**
	 *  Method to provide the required security level.
	 *  @return The security settings or null to inhibit execution.
	 */
	public Security getSecurityLevel(Component component);
}
