package jadex.messaging;

import java.util.Set;

/**
 *  Security meta-information of a message;
 *
 */
public interface ISecurityInfo
{
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
