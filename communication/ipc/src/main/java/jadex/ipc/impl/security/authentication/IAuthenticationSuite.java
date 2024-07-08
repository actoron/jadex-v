package jadex.ipc.impl.security.authentication;

import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.ipc.impl.security.Security;

/**
 *  Suite for authenticating messages.
 * @author jander
 *
 */
public interface IAuthenticationSuite
{
	/**
	 *  Gets the authentication suite ID.
	 *  
	 *  @return The authentication suite ID.
	 */
	public int getId();
	
	/**
	 *  Creates an authentication token for a message based on an abstract 
	 *  implementation-dependent "key".
	 *  
	 *  @param msg The message being authenticated.
	 *  @param key The key used for authentication.
	 *  @return Authentication token.
	 */
	public AuthToken createAuthenticationToken(byte[] msg, AbstractAuthenticationSecret key);
	
	/**
	 *  Creates an authentication token for a message based on an abstract 
	 *  implementation-dependent "key".
	 *  
	 *  @param msg The message being authenticated.
	 *  @param key The key used for authentication.
	 *  @param authtoken Authentication token.
	 *  @return True if authenticated, false otherwise.
	 */
	public boolean verifyAuthenticationToken(byte[] msg, AbstractAuthenticationSecret key, AuthToken authtoken);
	
	/** 
	 *  Gets the first round of the password-authenticated key-exchange.
	 *  
	 *  @return First round payload.
	 */
	public byte[] getPakeRound1(Security sec, GlobalProcessIdentifier remoteid);
	
	/** 
	 *  Gets the second round of the password-authenticated key-exchange.
	 *  
	 *  @return Second round payload.
	 */
	public byte[] getPakeRound2(Security sec, GlobalProcessIdentifier remoteid, byte[] round1data);
	
	/**
	 *  Finalizes the password-authenticated key exchange.
	 */
	public void finalizePake(Security sec, GlobalProcessIdentifier remoteid, byte[] round2data);
}
