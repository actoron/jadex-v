package jadex.messaging;

import jadex.core.IRuntimeFeature;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.security.authentication.AbstractAuthenticationSecret;

/**
 *  Security is responsible for validating (remote) requests.
 */
public interface ISecurityFeature extends IRuntimeFeature
{
	/** Special "trusted" role indicating that flagged entity may invoke any service remotely. */
	public static final String TRUSTED = "trusted";
	
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
}
