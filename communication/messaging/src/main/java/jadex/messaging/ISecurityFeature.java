package jadex.messaging;

import jadex.core.IRuntimeFeature;
import jadex.messaging.impl.security.authentication.AbstractAuthenticationSecret;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  Security is responsible for validating (remote) requests.
 */
public interface ISecurityFeature extends IRuntimeFeature 
{
	/** The unrestricted group and role (access is granted to all), e.g. used for chat. */
	public static final String UNRESTRICTED = "unrestricted";

	/** The default role that is assigned to services without security annotation and granted in all authenticated networks. */
	public static final String TRUSTED = "trusted";

	/** The admin role that is required by all jadex system services, e.g. CMS. */
	public static final String ADMIN = "admin";

	/** The local group and role for processes running on the same host. */
	public static final String LOCAL_GROUP = "localgroup";
	
	//-------- message-level encryption/authentication -------
	
	/**
	 *  Get the security instance.
	 */
	/*public static ISecurity get()
	{
		return Security.get();
	}*/
	
	/**
	 *  Encrypts and signs the message for a receiver.
	 *  
	 *  @param receiver The receiver.
	 *  @param content The content
	 *  @return Encrypted/signed message.
	 */
	//public IFuture<byte[]> encryptAndSign(GlobalProcessIdentifier receiver, byte[] content);
	
	public record DecodedMessage(ISecurityInfo secinfo, byte[] message) {};
	
	/**
	 *  Decrypt and authenticates the message from a sender.
	 *  
	 *  @param sender The sender.
	 *  @param content The content.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	//public DecodedMessage decryptAndAuth(GlobalProcessIdentifier sender, byte[] content);
	
	/**
	 *  Sets a new group.
	 * 
	 *  @param groupname The group name.
	 *  @param secret The secret encoded as String, null to remove.
	 */
	public void addGroup(String groupname, String secret);

	/**
	 *  Adds a new group.
	 *
	 *  @param groupname The group name.
	 *  @param asecret The secret.
	 */
	public void addGroup(String groupname, AbstractAuthenticationSecret asecret);

	/**
	 *  Get access to the stored groups.
	 *
	 *  @return The stored groups.
	 */
	public Map<String, List<AbstractAuthenticationSecret>> getGroups();

	/**
	 *  Remove a group.
	 * 
	 *  @param groupname The network name.
	 *  @param secret The secret, null to remove the network completely.
	 */
	public void removeGroup(String networkname, String secret);
	
	/** 
	 *  Adds an authority for authenticating platform names.
	 *  @param secret The secret, only X.509 secrets allowed.
	 */
	public void addNameAuthority(String cert);
	
	/** 
	 *  Removes an authority for authenticating platform names.
	 *  @param secret The secret, only X.509 secrets allowed.
	 */
	public void removeNameAuthority(String cert);
	
	/** 
	 *  Adds a name of an authenticated host to allow access.
	 *  @param name The host name, name must be authenticated with certificate.
	 */
	public void addTrustedHost(String name);
	
	/** 
	 *  Adds a name of an authenticated host to allow access.
	 *  @param name The host name.
	 */
	public void removeTrustedHost(String name);
	
	/**
	 *  Adds a role for an entity (platform or network name).
	 *  @param entity The entity name.
	 *  @param role The role name.
	 */
	public void addRole(String entity, String role);
	
	/**
	 *  Adds a role of an entity (platform or network name).
	 *  @param entity The entity name.
	 *  @param role The role name.
	 */
	public void removeRole(String entity, String role);

	/**
	 *  Marks a group as not part of the default authorization.
	 *
	 *  @param groupname The group name.
	 */
	public void addNoDefaultAuthorizationGroup(String groupname);

	/**
	 *  Unmarks a group as not part of the default authorization.
	 *
	 *  @param groupname The group name.
	 */
	public void removeNoDefaultAuthorizationGroup(String groupname);

	/**
	 *  Disable loading the local group. Must be invoked before messaging is used.
	 */
	public void disableLocalGroup();
	
	/**
	 *  Returns the allowed access groups from a given set of roles of a Security annotation.
	 *  @param annotationroles Roles specied in the Security annotation.
	 *  @return Groups representing those roles.
	 */
	public Set<String> getPermittedGroups(Set<String> annotationroles);
}
