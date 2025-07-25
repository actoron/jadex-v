package jadex.messaging.impl.security.handshake;

import jadex.core.impl.GlobalProcessIdentifier;

/**
 *  Initial handshake message.
 */
public class InitialHandshakeMessage extends BasicSecurityMessage
{
	/** Available crypto suites. */
	protected String[] cryptosuites;
	
	/**
	 *  Creates the message.
	 */
	public InitialHandshakeMessage()
	{
	}
	
	/**
	 *  Creates the message.
	 */
	public InitialHandshakeMessage(GlobalProcessIdentifier sender, String conversationid, String[] cryptosuites)
	{
		super(sender, conversationid);
		this.cryptosuites = cryptosuites;
	}
	
	/**
	 *  Gets the crypto suites.
	 *  @return The crypto suites.
	 */
	public String[] getCryptoSuites()
	{
		return cryptosuites;
	}
	
	/**
	 *  Sets the crypto suites.
	 *  @param cryptosuites The crypto suites.
	 */
	public void setCryptoSuites(String[] cryptosuites)
	{
		this.cryptosuites = cryptosuites;
	}
}
