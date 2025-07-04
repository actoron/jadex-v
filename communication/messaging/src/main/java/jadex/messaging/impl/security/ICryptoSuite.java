package jadex.messaging.impl.security;

import jadex.core.ComponentIdentifier;
import jadex.messaging.ISecurityInfo;
import jadex.messaging.impl.security.handshake.BasicSecurityMessage;

public interface ICryptoSuite
{
	/**
	 *  Encrypts and signs the message for a receiver.
	 *  
	 *  @param content The content
	 *  @return Encrypted/signed message.
	 */
	public byte[] encryptAndSign(ComponentIdentifier receiver, byte[] content);
	
	/**
	 *  Decrypt and authenticates the message from a sender.
	 *  
	 *  @param content The content.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	public byte[] decryptAndAuth(byte[] content);
	
	/**
	 *  Decrypt and authenticates a locally encrypted message.
	 *  
	 *  @param content The content.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	//public byte[] decryptAndAuthLocal(byte[] content);
	
	/**
	 *  Gets the security infos related to the authentication state.
	 *  
	 *  @return The security infos for decrypted messages.
	 */
	public ISecurityInfo getSecurityInfos();
	
	/**
	 *  Returns if the suite is expiring and should be replaced.
	 *  
	 *  @return True, if the suite is expiring and should be replaced.
	 */
	public boolean isExpiring();
	
	/**
	 *  Returns the creation time of the crypto suite.
	 *  
	 *  @return The creation time.
	 */
	public long getCreationTime();
	
	/**
	 *  Handles handshake messages.
	 *  
	 *  @param security The security object.
	 *  @param incomingmessage A message received from the other side of the handshake,
	 *  					   set to null for initial message.
	 *  @return True, if handshake continues, false when finished.
	 *  @throws SecurityException if handshake failed.
	 */
	public boolean handleHandshake(SecurityFeature security, BasicSecurityMessage incomingmessage);
	
	/**
	 *  Gets the ID used to identify the handshake of the suite.
	 *  
	 *  @return Handshake ID.
	 */
	public String getHandshakeId();
	
	/**
	 *  Sets the ID used to identify the handshake of the suite.
	 *  
	 *  @param id Handshake ID.
	 */
	public void setHandshakeId(String id);
	
	/**
	 *  Sets if the suite represents the protocol initializer.
	 * @param initializer True, if initializer.
	 */
	public void setInitializer(boolean initializer);
}
