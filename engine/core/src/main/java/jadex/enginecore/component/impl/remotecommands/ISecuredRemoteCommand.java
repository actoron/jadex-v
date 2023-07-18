package jadex.enginecore.component.impl.remotecommands;

import jadex.enginecore.IInternalAccess;
import jadex.enginecore.service.annotation.Security;

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
	public Security getSecurityLevel(IInternalAccess access);
}
