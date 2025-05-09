package jadex.messaging.impl.security.authentication;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jadex.core.IComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.security.ICryptoSuite;
import jadex.messaging.security.SecurityFeature;
import jadex.messaging.security.SecurityInfo;

/**
 *  Abstract crypto suite class for handling message IDs / replays.
 *
 */
public abstract class AbstractCryptoSuite implements ICryptoSuite
{
	/** Maximum windows size. */
	protected static final int MAX_WINDOW = 65536;
	
	/** The start value of the message id count. */
	protected static final long MSG_ID_START = Long.MIN_VALUE + Integer.MAX_VALUE;
	
	/** Highest ID received */
	protected long highid = MSG_ID_START;
	
	/** Lowest ID received */
	protected long lowid = MSG_ID_START;
	
	/** Missing IDs with expiration time. (Id, Expiration Time)*/
	protected Set<Long> missingids = new LinkedHashSet<Long>();
	
	/** Creation time of the suite. */
	protected long creationtime = System.currentTimeMillis();
	
	/** The message security info used after key exchange and authentication. */
	protected SecurityInfo secinf;
	
	/** The handshake ID. */
	protected String handshakeid;
	
	/** Checks if a message ID is valid */
	protected synchronized boolean isValid(long msgid)
	{
		boolean ret = false;
		
		if (highid - lowid >= MAX_WINDOW)
		{
			lowid += (highid - lowid) >>> 1;
			for (Iterator<Long> it = missingids.iterator(); it.hasNext(); )
			{
				long id = it.next();
				if (id - lowid < 0)
					it.remove();
			}
		}
		
		if (msgid - lowid >= 0 && ((lowid + MAX_WINDOW) - msgid > 0))
		{
			if (msgid == highid)
			{
				++highid;
				ret = true;
			}
			else if (msgid - highid > 0)
			{
				for (long id = highid; id - msgid < 0; ++id)
					missingids.add(id);
				highid = msgid + 1;
				ret = true;
			}
			else
			{
				ret = missingids.remove(msgid);
			}
		}
		else
		{
			System.out.println("Invalid: " + lowid + " " + highid + " " + msgid);
		}
		
		return ret;
	}
	
	/**
	 *  Sets up the message security infos for future messages.
	 *  
	 *  @param remoteid The ID of the remote platform.
	 *  @param authnets The networks the platform is part of and have been authenticated.
	 *  @param agent The security agent.
	 */
	protected void setupSecInfos(GlobalProcessIdentifier remoteid, List<String> authnets, String authenticatedhost)
	{
		SecurityFeature sec = (SecurityFeature) IComponentManager.get().getFeature(ISecurityFeature.class);
//		Security sec = Security.get();
		secinf = new SecurityInfo();
		secinf.setSender(remoteid);
//		secinf.setPlatformAuthenticated(platformauth);
//		if (authenticatedhost == null && platformauth)
//			secinf.setAuthenticatedPlatformName(remoteid.toString());
//		else
//			secinf.setAuthenticatedPlatformName(authenticatedplatformname);
		secinf.setAuthenticatedHostName(authenticatedhost);
		
		Set<String> fixedroles = new HashSet<>();
		
		//if (platformauth)
		//	fixedroles.add(Security.ADMIN);
		
		secinf.setGroups(new HashSet<>(authnets));
		
		Set<String> sharednets = new HashSet<>(authnets);
		sharednets.retainAll(sec.getGroups().keySet());
		secinf.setSharedNetworks(sharednets);
		
		if (authenticatedhost != null && sec.isTrustedHosts(authenticatedhost))
			fixedroles.add(SecurityFeature.TRUSTED);
		
		if (sec.isDefaultAuthorization() && secinf.getSharedGroups() != null && secinf.getSharedGroups().size() > 0)
			fixedroles.add(SecurityFeature.TRUSTED);
		
		// Admin role is automatically trusted.
		//if (fixedroles.contains(Security.ADMIN))
		//	fixedroles.add(Security.TRUSTED);
		
		secinf.setFixedRoles(fixedroles);
		
		sec.setSecInfoMappedRoles(secinf);
		
		if (!sec.getInternalAllowNoAuthName() && secinf.getAuthenticatedHostName() == null)
			throw new SecurityException("Connections to platforms with unauthenticated platform names are not allowed: " + remoteid);
		
		if (!sec.getInternalAllowNoGroup() && secinf.getGroups().isEmpty())
			throw new SecurityException("Connections to platforms with no authenticated networks are not allowed: " + remoteid);
		
		if (sec.getInternalRefuseUntrusted() && !secinf.getRoles().contains(SecurityFeature.TRUSTED))
			throw new SecurityException("Untrusted connection not allowed: " + remoteid);
	}
	
	/**
	 *  Returns the creation time of the crypto suite.
	 *  
	 *  @return The creation time.
	 */
	public long getCreationTime()
	{
		return creationtime;
	}
	
	/**
	 *  Gets the ID used to identify the handshake of the suite.
	 *  
	 *  @return Handshake ID.
	 */
	public String getHandshakeId()
	{
		return handshakeid;
	}
	
	/**
	 *  Sets the ID used to identify the handshake of the suite.
	 *  
	 *  @param id Handshake ID.
	 */
	public void setHandshakeId(String id)
	{
		handshakeid = id;
	}
	
	/**
	 *  Sets if the suite represents the protocol initializer.
	 * @param initializer True, if initializer.
	 */
	public void setInitializer(boolean initializer)
	{
	}
}
