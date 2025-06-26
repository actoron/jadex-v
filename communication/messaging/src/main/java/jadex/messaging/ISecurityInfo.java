package jadex.messaging;

import jadex.core.impl.GlobalProcessIdentifier;

import java.util.Set;

/**
 *  Security meta-information of a message;
 *
 */
public interface ISecurityInfo
{
	/**
	 *  Gets the sender of the message.
	 *  @return Sender of the message.
	 */
	public GlobalProcessIdentifier getSender();

	/**
	 *  Returns the authenticated host as String.
	 *
	 *  @return The authenticated host, null if not authenticated.
	 */
	public String getAuthenticatedHostName();
	
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
