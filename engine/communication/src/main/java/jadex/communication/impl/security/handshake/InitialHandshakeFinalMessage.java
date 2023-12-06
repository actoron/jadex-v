package jadex.communication.impl.security.handshake;

import jadex.core.ComponentIdentifier.GlobalProcessIdentifier;

/**
 *  Final message in the initial handshake.
 *
 */
public class InitialHandshakeFinalMessage extends BasicSecurityMessage
{
	/** The chosen crypto suite. */
	protected String chosencryptosuite;
	
	/**
	 *  Creates the message.
	 */
	public InitialHandshakeFinalMessage()
	{
		
	}
	
	/**
	 *  Creates the message.
	 */
	public InitialHandshakeFinalMessage(GlobalProcessIdentifier sender, String conversationid, String chosencryptosuite)
	{
		super(sender, conversationid);
		this.chosencryptosuite = chosencryptosuite;
	}
	
	/**
	 *  Gets the chosen crypto suite.
	 * 
	 *  @return The chosen crypto suite.
	 */
	public String getChosenCryptoSuite()
	{
		return chosencryptosuite;
	}
	
	/**
	 *  Sets the chosen crypto suite.
	 * 
	 *  @param chosencryptosuite The chosen crypto suite.
	 */
	public void setChosenCryptoSuite(String chosencryptosuite)
	{
		this.chosencryptosuite = chosencryptosuite;
	}
}
