package jadex.communication;

import java.util.Set;

import jadex.core.ComponentIdentifier.GlobalProcessIdentifier;

/**
 *  Security meta-information of a message;
 *
 */
public interface ISecurityInfo
{
	/**
	 *  Returns the authenticated global process ID as String.
	 *
	 *  @return The authenticated global process ID, null if not authenticated.
	 */
	public GlobalProcessIdentifier getAuthenticatedGlobalProcessId();
	
	/**
	 *  Gets the authenticated groups of the sender.
	 *
	 *  @return The authenticated groups of the sender (sorted).
	 */
	public Set<String> getGroups();
	
	/**
	 *  Gets the authenticated groups of the sender.
	 *
	 *  @return The authenticated groups of the sender (sorted).
	 */
	public Set<String> getSharedGroups();
	
	/**
	 *  Gets the roles associated with the sender.
	 *
	 *  @return The roles.
	 */
	public Set<String> getRoles();
}
