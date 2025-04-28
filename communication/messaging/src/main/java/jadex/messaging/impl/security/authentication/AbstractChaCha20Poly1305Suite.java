package jadex.messaging.impl.security.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import jadex.core.ComponentIdentifier;
import jadex.messaging.security.authentication.AbstractAuthenticationSecret;
import jadex.messaging.security.authentication.AbstractX509PemSecret;
import jadex.messaging.security.authentication.X509PemStringsSecret;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.Pack;

import jadex.common.ByteArrayWrapper;
import jadex.common.SUtil;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.ISecurityInfo;
import jadex.messaging.security.SSecurity;
import jadex.messaging.security.SecurityFeature;
import jadex.messaging.security.handshake.BasicSecurityMessage;
import jadex.messaging.security.handshake.InitialHandshakeFinalMessage;


/**
 *  Crypto suite based on Curve448 and ChaCha20-Poly1305 AEAD.
 *
 */
public abstract class AbstractChaCha20Poly1305Suite extends AbstractCryptoSuite
{
	//--------------- Handshake state -------------------
	
	
	/** The global process identifier. */
	protected GlobalProcessIdentifier gpid;
	
	/** The remote public key */
	protected byte[] remotepublickey;
	
	/** Agreed-on random state / challenge. */
	protected byte[] challenge;
	
	/** The authentication suite. */
	protected IAuthenticationSuite authsuite;
	
	/** Next step in the handshake protocol. */
	protected int nextstep;
	
	/** Hashed network names reverse lookup */
	//protected MultiCollection<ByteArrayWrapper, Tuple2<String, AbstractAuthenticationSecret>> hashednetworks;
	protected Map<jadex.common.ByteArrayWrapper, String> hashedgroupnames;
	
	// -------------- Operational state -----------------
	
	/** The ChaCha20 key. */
	protected byte[] key = new byte[32];
	
	/** The current message ID. */
	protected AtomicLong msgid = new AtomicLong(AbstractCryptoSuite.MSG_ID_START);
	
	/** Prefix used for the ChaCha20 nonce. */
	protected int nonceprefix;
	
	/** The local process identifier. */
	protected GlobalProcessIdentifier localgpid;
	
	/**
	 *  Creates the suite.
	 */
	public AbstractChaCha20Poly1305Suite(GlobalProcessIdentifier localgpid)
	{
		this.localgpid = localgpid;
	}
	
	/**
	 *  Encrypts and signs the message for a receiver.
	 *  
	 *  @param receiver The receiver.
	 *  @param content The content
	 *  @return Encrypted/signed message.
	 */
	public byte[] encryptAndSign(ComponentIdentifier receiver, byte[] content)
	{
		return chacha20Poly1305Enc(content, key, nonceprefix, receiver, msgid.getAndIncrement());
	}
	
	/**
	 *  Decrypt and authenticates the message from a sender.
	 *  
	 *  @param sender The sender.
	 *  @param content The content.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	public byte[] decryptAndAuth(byte[] content)
	{
		ChaChaDecryptedMessage decmsg = chacha20Poly1305Dec(content, key, ~nonceprefix);
		byte[] ret = decmsg.msg();
		if (ret != null && !isValid(decmsg.msgid()))
			ret = null;
		return ret;
	}
	
	/**
	 *  Decrypt and authenticates a locally encrypted message.
	 *  
	 *  @param content The content.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	/*public byte[] decryptAndAuthLocal(byte[] content)
	{
		byte[] ret = chacha20Poly1305Dec(content, key, nonceprefix);
		return ret;
	}*/
	
	/**
	 *  Gets the security infos related to the authentication state.
	 *  
	 *  @return The security infos for decrypted messages.
	 */
	public ISecurityInfo getSecurityInfos()
	{
		return secinf;
	}
	
	/**
	 *  Returns if the suite is expiring and should be replaced.
	 *  
	 *  @return True, if the suite is expiring and should be replaced.
	 */
	public boolean isExpiring()
	{
		return msgid.get() < AbstractCryptoSuite.MSG_ID_START;
	}
	
	/**
	 *  Handles handshake messages.
	 *  
	 *  @param agent The security agent object.
	 *  @param incomingmessage A message received from the other side of the handshake,
	 *  					   set to null for initial message.
	 *  @return True, if handshake continues, false when finished.
	 *  @throws SecurityException if handshake failed.
	 */
	public boolean handleHandshake(SecurityFeature sec, BasicSecurityMessage incomingmessage)
	{
		boolean ret = true;
		
//		System.out.println("got message " + incomingmessage.getClass().getName() + " " + incomingmessage.getConversationId() + " " + incomingmessage.getMessageId() + " " + nextstep + " " + System.identityHashCode(this));
		
		if (nextstep == 0 && incomingmessage instanceof InitialHandshakeFinalMessage)
		{
//			ts = System.currentTimeMillis();
			authsuite = new Blake3X509AuthenticationSuite(localgpid);
			StartExchangeMessage sem = new StartExchangeMessage(localgpid, incomingmessage.getConversationId());
			challenge = new byte[32];
			SSecurity.getSecureRandom().nextBytes(challenge);
			sem.setChallenge(challenge);
			sem.setPakeRound1Data(authsuite.getPakeRound1(sec, incomingmessage.getSender()));
			sem.setPublicKey(getPubKey());
			
			sec.sendSecurityHandshakeMessage(incomingmessage.getSender(), sem);
			nextstep = 1;
		}
		else if (nextstep == -1 && incomingmessage instanceof StartExchangeMessage)
		{
			StartExchangeMessage sem = (StartExchangeMessage) incomingmessage;
			remotepublickey = sem.getPublicKey();
			
			AckExchangeMessage reply = new AckExchangeMessage(localgpid, sem.getConversationId());
			reply.setPublicKey(getPubKey());
			reply.setEncapsulatedsecret(getEncapsulatedSecret());
			challenge = new byte[32];
			SSecurity.getSecureRandom().nextBytes(challenge);
			reply.setChallenge(challenge);
			
			challenge = new byte[32];
			Blake3Digest dig = new Blake3Digest();
			//Blake2bDigest dig = new Blake2bDigest(256);
			dig.update(sem.getChallenge(), 0, sem.getChallenge().length);
			dig.update(reply.getChallenge(), 0, reply.getChallenge().length);
			dig.doFinal(challenge, 0);
			
			authsuite = new Blake3X509AuthenticationSuite(localgpid);
			
			GlobalProcessIdentifier remoteid = sem.getSender();
			try
			{
				reply.setPakeRound1Data(authsuite.getPakeRound1(sec, remoteid));
				reply.setPakeRound2Data(authsuite.getPakeRound2(sec, remoteid, sem.getPakeRound1Data()));
			}
			catch (Exception e)
			{
			}
			
			sec.sendSecurityHandshakeMessage(incomingmessage.getSender(), reply);
			nextstep = -2;
			
			hashedgroupnames = getHashedGroupNames(sec.getGroups().keySet(), challenge);
		}
		else if (nextstep == 1 && incomingmessage instanceof AckExchangeMessage)
		{
			AckExchangeMessage ack = (AckExchangeMessage) incomingmessage;
			remotepublickey = ack.getPublicKey();
			
			Blake3Digest dig = new Blake3Digest();
			dig.update(challenge, 0, challenge.length);
			dig.update(ack.getChallenge(), 0, ack.getChallenge().length);
			challenge = new byte[32];
			dig.doFinal(challenge, 0);
			
			hashedgroupnames = getHashedGroupNames(sec.getGroups().keySet(), challenge);
			
			GlobalProcessIdentifier remoteid = ack.getSender();
			
			KeyExchangeMessage reply = new KeyExchangeMessage(localgpid, ack.getConversationId());
			
			try
			{
				reply.setPakeRound2Data(authsuite.getPakeRound2(sec, remoteid, ack.getPakeRound1Data()));
				authsuite.finalizePake(sec, remoteid, ack.getPakeRound2Data());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			reply.setEncapsulatedSecret(getEncapsulatedSecret());
			
			reply.setGroupSigs(getGroupSignatures(getPubKey(), sec.getGroups()));
			
			reply.setHostNameSig(getHostNameSignature(getPubKey(), sec.getHostNameCertificate()));
			
			sec.sendSecurityHandshakeMessage(incomingmessage.getSender(), reply);
			nextstep = 2;
		}
		else if (nextstep == -2 && incomingmessage instanceof KeyExchangeMessage)
		{
			KeyExchangeMessage kx = (KeyExchangeMessage) incomingmessage;
			
			GlobalProcessIdentifier remoteid = kx.getSender();
			
			try
			{
				authsuite.finalizePake(sec, remoteid, kx.getPakeRound2Data());
			}
			catch (Exception e)
			{
			}
			
			String remotehostname = kx.getSender().host();
			String authenticatedhostname = null;
			if (verifyHostNameSignature(remotepublickey, kx.getHostNameSig(), sec.getInternalNameAuthorities(), remotehostname))
				authenticatedhostname = remotehostname;
			
			//boolean platformauth = verifyPlatformSignatures(remotepublickey, kx.getPlatformSecretSigs(), agent.getInternalPlatformSecret());
			//platformauth &= agent.getInternalUsePlatformSecret();
			List<String> authnets = verifyGroupSignatures(remotepublickey, kx.getGroupSigs(), sec.getGroups());
			setupSecInfos(remoteid, authnets, authenticatedhostname);
			
			KeyExchangeMessage reply = new KeyExchangeMessage(localgpid, kx.getConversationId());
			reply.setEncapsulatedSecret(getEncapsulatedSecret());
			/*if (agent.getInternalUsePlatformSecret())
					reply.setPlatformSecretSigs(getPlatformSignatures(pubkey, agent, remoteid));*/
			reply.setGroupSigs(getGroupSignatures(getPubKey(), sec.getGroups()));
			
			reply.setHostNameSig(getHostNameSignature(getPubKey(), sec.getHostNameCertificate()));
			
			key = createChaChaKey(kx.getEncapsulatedSecret());
			
			nonceprefix = Pack.littleEndianToInt(challenge, 0);
			
			sec.sendSecurityHandshakeMessage(incomingmessage.getSender(), reply);
			nextstep = -3;
		}
		else if (nextstep == 2 && incomingmessage instanceof KeyExchangeMessage)
		{
			KeyExchangeMessage kx = (KeyExchangeMessage) incomingmessage;
			key = createChaChaKey(kx.getEncapsulatedSecret());
			
			GlobalProcessIdentifier remoteid = kx.getSender();
			
			String remotehostname = kx.getSender().host();
			String authenticatedhostname = null;
			if (verifyHostNameSignature(remotepublickey, kx.getHostNameSig(), sec.getNameAuthorityCerts(), remotehostname))
				authenticatedhostname = remotehostname;
			
			/*boolean platformauth = verifyPlatformSignatures(remotepublickey, kx.getPlatformSecretSigs(), agent.getInternalPlatformSecret());
			platformauth &= agent.getInternalUsePlatformSecret();*/
			List<String> authnets = verifyGroupSignatures(remotepublickey, kx.getGroupSigs(), sec.getGroups());
			setupSecInfos(remoteid, authnets, authenticatedhostname);
			
			// Removed, checked during setupsecinf
//			if (agent.getInternalRefuseUnauth() && (secinf.getRoles() == null || secinf.getRoles().isEmpty()))
//				throw new SecurityException("Unauthenticated connection not allowed.");
			
			nonceprefix = Pack.littleEndianToInt(challenge, 0);
			nonceprefix = ~nonceprefix;
//			System.out.println("Shared Key1: " + Arrays.toString(key) + " " + secinf.isAuthenticated());
			
			// Delete handshake state
			//ephemeralkey = null;
			challenge = null;
			hashedgroupnames = null;
			remotepublickey = null;
			authsuite = null;
			
			ReadyMessage rdy = new ReadyMessage(localgpid, kx.getConversationId());
			sec.sendSecurityHandshakeMessage(kx.getSender(), rdy);
			
			ret = false;
			nextstep = Integer.MAX_VALUE;
//			System.out.println("Handshake took: " + (System.currentTimeMillis() - ts));
		}
		else if (nextstep == -3 && incomingmessage instanceof ReadyMessage)
		{
//			System.out.println("Shared Key2: " + Arrays.toString(key) + " " + secinf.isAuthenticated());
			
			// Delete handshake state
			//ephemeralkey = null;
			remotepublickey = null;
			challenge = null;
			hashedgroupnames = null;
			authsuite = null;
			
			ret = false;
			nextstep = Integer.MIN_VALUE;
		}
		else
		{
			throw new SecurityException("Protocol violation detected.");
		}
		
		return ret;
	}
	
	/**
	 *  Sets if the suite represents the initializer.
	 * @param initializer True, if initializer.
	 */
	public void setInitializer(boolean initializer)
	{
		if (initializer)
			nextstep = -1;
	}
	
	/**
	 *  Destroy information.
	 */
	public void destroy()
	{
		challenge = null;
		authsuite = null;
		if (key != null)
		{
			//byte[] raw = new byte[key.length << 2];
			SSecurity.getSecureRandom().nextBytes(key);
			//Pack.littleEndianToInt(raw, 0, key);
		}
		key = null;
		nonceprefix = 0;
		msgid.set(0);
		secinf = null;
	}
	
	/**
	 *  Generates a token verifying the platform name.
	 *  
	 *  @param key The key.
	 *  @param secret The name certificate.
	 *  @return The token.
	 */
	public AuthToken getHostNameSignature(byte[] key, AbstractX509PemSecret secret)
	{
		AuthToken ret = null;
		if (secret != null)
		{
			ret = signKey(challenge, key, secret);
		}
		return ret;
	}
	
	/**
	 *  Verifies a token verifying the platform name.
	 */
	public boolean verifyHostNameSignature(byte[] key, AuthToken platformnamesig, Set<X509CertificateHolder> nameauthorities, String platformname)
	{
		boolean ret = false;
		try
		{
			if (platformnamesig instanceof X509AuthToken)
			{
				X509AuthToken sig = (X509AuthToken) platformnamesig;
				for (X509CertificateHolder nameauthority : nameauthorities)
				{
//					X509PemStringsSecret nasecret = new X509PemStringsSecret(SSecurity.writeCertificateAsPEM(nameauthority), null, null);
					X509PemStringsSecret nasecret = new X509PemStringsSecret(SSecurity.writeCertificateAsPEM(nameauthority), null);
					boolean verified = verifyKey(challenge, key, nasecret, sig);
					if (verified)
					{
						ret = SSecurity.checkEntity(SSecurity.readCertificateFromPEM(sig.getCertificate()), platformname);
						if (ret)
							break;
					}
				}
			}
		}
		catch (Exception e)
		{
		}
		return ret;
	}
	
	/**
	 *  Signs a key with group secrets.
	 *  
	 *  @param key The key (public key).
	 *  @param groups The group secrets.
	 *  
	 *  @return Map hashed group name -> signature.
	 */
	protected List<GroupSignature> getGroupSignatures(byte[] key, Map<String, List<AbstractAuthenticationSecret>> groups)
	{
		List<GroupSignature> groupsigs = new ArrayList<>();
		
		if (hashedgroupnames.size() > 0)
		{
			for (Map.Entry<ByteArrayWrapper, String> entry : hashedgroupnames.entrySet())
			{
				Collection<AbstractAuthenticationSecret> secrets = groups.get(entry.getValue());
				
				if (secrets != null && secrets.size() > 0)
				{
					for (AbstractAuthenticationSecret secret : secrets)
					{
						if (secret.canSign())
						{
							AuthToken sig = signKey(challenge, key, secret);
							if (sig != null)
								groupsigs.add(new GroupSignature(entry.getKey(), sig));
						}
					}
				}
			}
		}
		return groupsigs;
	}
	
	/**
	 *  Verifies network signatures of a key.
	 *  
	 *  @param key The key.
	 *  @param groupsigs The signatures.
	 *  @return List of network that authenticated the key.
	 */
	protected List<String> verifyGroupSignatures(byte[] key, List<GroupSignature> groupsigs, Map<String, List<AbstractAuthenticationSecret>> groups)
	{
		List<String> ret = new ArrayList<String>();
		
		if (groupsigs != null)
		{
			for (GroupSignature gsig : groupsigs)
			{
				String groupname = hashedgroupnames.get(gsig.groupnamehash());
				if (groupname != null)
				{
					Collection<AbstractAuthenticationSecret> secrets = groups.get(groupname);
					if (secrets != null)
					{
						boolean authenticated = false;
						for (AbstractAuthenticationSecret secret : secrets)
						{
							authenticated = verifyKey(challenge, key, secret, gsig.signature());
							if (authenticated)
								break;
						}
						if (!authenticated)
							throw new SecurityException("Remote platform presented unverifiable group signature for group " + groupname + ", handshake terminated.");
						ret.add(groupname);
					}
				}
			}
		}
		Logger.getLogger("security").fine("Remote networks verified: " + Arrays.toString(ret.toArray()));
		return ret;
	}
	
	/**
	 *  Signs a key for authentication.
	 *  
	 *  @param challenge Nonce / challenge received from remote.
	 *  @param key The key to sign.
	 *  @param secret Secret used for authentication.
	 *  @return Signature.
	 */
	protected AuthToken signKey(byte[] challenge, byte[] key, AbstractAuthenticationSecret secret)
	{
		byte[] sigmsg = new byte[key.length + challenge.length];
		System.arraycopy(key, 0, sigmsg, 0, key.length);
		System.arraycopy(challenge, 0, sigmsg, key.length, challenge.length);
		return authsuite.createAuthenticationToken(sigmsg, secret);
	}
	
	/**
	 *  Verifies a key for authentication.
	 *  
	 *  @param challenge Nonce / challenge received from remote.
	 *  @param key The key to verify.
	 *  @param secret Secret used for authentication.
	 *  @return True, if authenticated.
	 */
	protected boolean verifyKey(byte[] challenge, byte[] key, AbstractAuthenticationSecret secret, AuthToken authtoken)
	{
		byte[] sigmsg = new byte[key.length + challenge.length];
		System.arraycopy(key, 0, sigmsg, 0, key.length);
		System.arraycopy(challenge, 0, sigmsg, key.length, challenge.length);
		return authsuite.verifyAuthenticationToken(sigmsg, secret, authtoken);
	}
	
	/**
	 *  Hashes the network name.
	 *  
	 *  @param gname The group name.
	 *  @param salt The salt.
	 *  @return Hashed network name.
	 */
	protected static final ByteArrayWrapper hashGroupName(String gname, byte[] salt)
	{
		Blake3Digest dig = new Blake3Digest();
		byte[] bgname = gname.getBytes(SUtil.UTF8);
		dig.update(bgname,0,bgname.length);
		dig.update(salt, 0, salt.length);
		byte[] hgwname = new byte[dig.getDigestSize()];
		dig.doFinal(hgwname, 0);
		
		return new ByteArrayWrapper(hgwname);
	}
	
	/**
	 *  Creates a reverse look-up map of hashed network names.
	 *  
	 *  @param networks The networks.
	 *  @param salt Salt to use.
	 *  @return Reverse look-up map.
	 */
	protected static final Map<ByteArrayWrapper, String> getHashedGroupNames(Set<String> names, byte[] salt)
	{
		Map<ByteArrayWrapper, String> ret = new HashMap<>();
		for (String name : names)
			ret.put(hashGroupName(name, salt), name);
		return ret;
	}
	
	/**
	 *  Encrypts content using an RFC 7539-like AEAD construction.
	 *  
	 *  @param content Clear text being encrypted.
	 *  @param key Key used for encryption/authentication.
	 *  @param nonceprefix Local nonce prefix used.
	 *  @param msgid Current message ID.
	 *  @return 
	 */
	protected static final byte[] chacha20Poly1305Enc(byte[] content, byte[] key, int nonceprefix, ComponentIdentifier receiver, long msgid)
	{
		byte[] nonce = new byte[12];
		SUtil.intIntoBytes(nonceprefix, nonce, 0);
		SUtil.longIntoBytes(msgid, nonce, 4);

		ChaCha20Poly1305 cipher = new ChaCha20Poly1305();
		AEADParameters keyparam = new AEADParameters(new KeyParameter(key), 128, nonce, null);//aadbytes);
		cipher.init(true, keyparam);

		int msgidoff = 8; // Offset of the long size message ID
		String receiverstring = receiver.toString();
		int receiverstringlengthoffset = 4; // Offset for the int-size receiver string length
		int receiverlen = SUtil.stringSizeAsUtf8(receiverstring);
		int prefixoffset = receiverlen + msgidoff + receiverstringlengthoffset; // Offset for the message ID plus receiver string
		byte[] encmsg = new byte[cipher.getOutputSize(content.length) + prefixoffset]; // Create output array

		SUtil.longIntoBytes(msgid, encmsg, 0); // Copy message ID into output
		SUtil.stringIntoByteArray(receiverstring, receiverlen, encmsg, msgidoff); // Write receiver ID as UTF8 string

		cipher.processAADBytes(encmsg, msgidoff, receiverstringlengthoffset + receiverlen);
		int len = cipher.processBytes(content, 0, content.length, encmsg, prefixoffset);
		try
		{
			cipher.doFinal(encmsg, len + prefixoffset);
		} catch (Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
		
		return encmsg;
	}
	
	/**
	 *  Decrypts content using an RFC 7539-like AEAD construction.
	 *  
	 *  @param content Clear text being encrypted.
	 *  @param key Key used for encryption/authentication.
	 *  @param nonceprefix Local nonce prefix used.
	 *  @return Clear text or null if authentication failed.
	 */
	protected static ChaChaDecryptedMessage chacha20Poly1305Dec(byte[] content, byte[] key, int nonceprefix)
	{
		long msgid = SUtil.bytesToLong(content, 0); // Decode message ID.
		int msgidoff = 8; // Offset of the long size message ID

		byte[] nonce = new byte[12];
		SUtil.intIntoBytes(nonceprefix, nonce, 0);
		System.arraycopy(content, 0, nonce, 4, 8); // No need to reencode msgid into nonce, we can just copy it

		int receiverstringlengthoffset = 4; // Offset for the int-size receiver string length
		int cidlen = SUtil.bytesToInt(content, msgidoff);
		String cidstr = new String(content,  + receiverstringlengthoffset + msgidoff, cidlen, SUtil.UTF8);
		int prefixoffset = cidlen + receiverstringlengthoffset + msgidoff; // Offset for the message ID plus receiver string

		ComponentIdentifier receiver = ComponentIdentifier.fromString(cidstr);

		ChaCha20Poly1305 cipher = new ChaCha20Poly1305();
		AEADParameters keyparam = new AEADParameters(new KeyParameter(key), 128, nonce, null);
		cipher.init(false, keyparam);
		cipher.processAADBytes(content, msgidoff, cidlen + receiverstringlengthoffset);
		byte[] decmsg = new byte[cipher.getOutputSize(content.length - prefixoffset)];
		int len = cipher.processBytes(content, prefixoffset, content.length - prefixoffset, decmsg, 0);
		try
		{
			cipher.doFinal(decmsg, len);
		} catch (Exception e)
		{
			// Validation failed.
			decmsg = null;
		}

		return new ChaChaDecryptedMessage(msgid, receiver, decmsg);
	}
	
	public static final ChaCha20Poly1305 getChaChaCipherX(byte[] content, byte[] key, int nonceprefix, Long msgid)
	{
		boolean enc = true;
		if (msgid == null)
		{
			msgid = Pack.littleEndianToLong(content, 8);
			enc = false;
		}
		
		byte[] nonce = new byte[12];
		SUtil.intIntoBytes(nonceprefix, nonce, 0);
		SUtil.intIntoBytes((int)(msgid.longValue() >>> 32), nonce, 4);
		SUtil.intIntoBytes((int) msgid.longValue(), nonce, 8);
		
		ChaCha20Poly1305 cipher = new ChaCha20Poly1305();
		AEADParameters keyparam = new AEADParameters(new KeyParameter(key), 128, nonce, null);
		cipher.init(enc, keyparam);
		return cipher;
	}
	
	/**
	 *  Rounds up the
	 * @param size
	 * @return
	 */
	protected static final int pad16Size(int size)
	{
		return (size + 15) & ~15;
	}
	
	/**
	 *  Gets the encoded public key.
	 * 
	 *  @return The local public key.
	 */
	protected abstract byte[] getPubKey();
	
	/**
	 *  Gets the local secret encapsulated by the remote public key.
	 * 
	 *  @return The local encapsulated secret.
	 */
	protected abstract byte[] getEncapsulatedSecret();
	
	/**
	 *  Creates the ephemeral key.
	 *  
	 *  @return The ephemeral key.
	 */
	//protected abstract Object createEphemeralKey(byte[] encapsulatedsecret);
	
	/**
	 *  Generates the shared public key.
	 *  
	 *  @param remotepubkey The remote public key.
	 *  @return Shared key.
	 */
	//protected abstract byte[] generateSharedKey();
	
	/**
	 *  Creates the ChaCha key.
	 *  
	 *  @return The ChaCha key.
	 */
	protected abstract byte[] createChaChaKey(byte[] encapsulatedsecret);
	
	/**
	 *  Gets the shared ChaCha key.
	 * 
	 *  @param genkey The exchanged key material.
	 */
	protected int[] convertToChaChaKey(byte[] genkey)
	{
		Blake3Digest digest = new Blake3Digest();
		digest.update(genkey, 0, genkey.length);
		genkey = new byte[32];
		digest.doFinal(genkey, 0);
		int[] ret = new int[8];
		Pack.littleEndianToInt(genkey, 0, ret);
		return ret;
	}
	
	/**
	 *  Message for starting the exchange.
	 *
	 */
	protected static class StartExchangeMessage extends BasicSecurityMessage
	{
		/** Challenge for the exchange authentication. */
		protected byte[] challenge;
		
		/** PAKE round 1 data. */
		protected byte[] pakeround1data;
		
		/** Public key used for the key exchange. */
		protected byte[] publickey;
		
		/**
		 *  Creates the message.
		 */
		public StartExchangeMessage()
		{
		}
		
		/**
		 *  Creates the message.
		 */
		public StartExchangeMessage(GlobalProcessIdentifier sender, String conversationid)
		{
			super(sender, conversationid);
		}
		
		/**
		 *  Gets the challenge.
		 *
		 *  @return The challenge.
		 */
		public byte[] getChallenge()
		{
			return challenge;
		}

		/**
		 *  Sets the challenge.
		 *
		 *  @param challenge The challenge.
		 */
		public void setChallenge(byte[] challenge)
		{
			this.challenge = challenge;
		}
		
		/**
		 *  Gets the PAKE Round 1 data.
		 *  
		 *  @return The PAKE Round 1 data.
		 */
		public byte[] getPakeRound1Data()
		{
			return pakeround1data;
		}
		
		/**
		 *  Sets the PAKE Round 1 data.
		 *  
		 *  @param pakeround1data The PAKE Round 1 data.
		 */
		public void setPakeRound1Data(byte[] pakeround1data)
		{
			this.pakeround1data = pakeround1data;
		}
		
		/**
		 *  Gets the public key used for key exchange.
		 * 
		 *  @return The public key used for key exchange.
		 */
		public byte[] getPublicKey()
		{
			return publickey;
		}
		
		/**
		 *  Sets the public key used for key exchange.
		 * 
		 *  @publickey The public key used for key exchange.
		 */
		public void setPublicKey(byte[] publickey)
		{
			this.publickey = publickey;
		}
	}
	
	/**
	 *  Message for acknowledging the start of the exchange.
	 *
	 */
	protected static class AckExchangeMessage extends BasicSecurityMessage
	{
		/** Challenge for the exchange authentication. */
		protected byte[] challenge;
		
		/** PAKE round 1 data. */
		protected byte[] pakeround1data;
		
		/** PAKE round 2 data. */
		protected byte[] pakeround2data;
		
		/** Public key used for the key exchange. */
		protected byte[] publickey;
		
		/** Encapsulated secret */
		protected byte[] encapsulatedsecret;
		
		/**
		 *  Creates the message.
		 */
		public AckExchangeMessage()
		{
		}
		
		/**
		 *  Creates the message.
		 */
		public AckExchangeMessage(GlobalProcessIdentifier sender, String conversationid)
		{
			super(sender, conversationid);
		}
		
		/**
		 *  Gets the challenge.
		 *
		 *  @return The challenge.
		 */
		public byte[] getChallenge()
		{
			return challenge;
		}

		/**
		 *  Sets the challenge.
		 *
		 *  @param challenge The challenge.
		 */
		public void setChallenge(byte[] challenge)
		{
			this.challenge = challenge;
		}
		
		/**
		 *  Gets the PAKE Round 1 data.
		 *  
		 *  @return The PAKE Round 1 data.
		 */
		public byte[] getPakeRound1Data()
		{
			return pakeround1data;
		}
		
		/**
		 *  Sets the PAKE Round 1 data.
		 *  
		 *  @param pakeround1data The PAKE Round 1 data.
		 */
		public void setPakeRound1Data(byte[] pakeround1data)
		{
			this.pakeround1data = pakeround1data;
		}
		
		/**
		 *  Gets the PAKE Round 2 data.
		 *  
		 *  @return The PAKE Round 2 data.
		 */
		public byte[] getPakeRound2Data()
		{
			return pakeround2data;
		}
		
		/**
		 *  Sets the PAKE Round 2 data.
		 *  
		 *  @param pakeround1data The PAKE Round 2 data.
		 */
		public void setPakeRound2Data(byte[] pakeround2data)
		{
			this.pakeround2data = pakeround2data;
		}
		
		/**
		 *  Gets the public key used for key exchange.
		 * 
		 *  @return The public key used for key exchange.
		 */
		public byte[] getPublicKey()
		{
			return publickey;
		}
		
		/**
		 *  Sets the public key used for key exchange.
		 * 
		 *  @publickey The public key used for key exchange.
		 */
		public void setPublicKey(byte[] publickey)
		{
			this.publickey = publickey;
		}
		
		/**
		 *  Gets the encapsulated secret.
		 *  
		 *  @return The encapsulated secret.
		 */
		public byte[] getEncapsulatedSecret()
		{
			return encapsulatedsecret;
		}
		
		/**
		 *  Sets the encapsulated secret.
		 *  
		 *  @param encapsulatedsecret The encapsulated secret.
		 */
		public void setEncapsulatedsecret(byte[] encapsulatedsecret)
		{
			this.encapsulatedsecret = encapsulatedsecret;
		}
	}
	
	protected static class KeyExchangeMessage extends BasicSecurityMessage
	{
		/** Signatures for verifying the host name. */
		protected AuthToken hostnamesig;
		
		/** Signatures based on the local and remote platform access secrets. */
		//protected Tuple2<AuthToken, AuthToken> platformsecretsigs;
		
		/** Group signatures of the public key. */
		protected List<GroupSignature> groupsigs;
		
		/** PAKE round 2 data. */
		protected byte[] pakeround2data;
		
		/** Encapsulated secret */
		protected byte[] encapsulatedsecret;
		
		/**
		 *  Creates the message.
		 */
		public KeyExchangeMessage()
		{
		}
		
		/**
		 *  Creates the message.
		 */
		public KeyExchangeMessage(GlobalProcessIdentifier sender, String conversationid)
		{
			super(sender, conversationid);
		}
		
		/**
		 *  Gets the PAKE Round 2 data.
		 *  
		 *  @return The PAKE Round 2 data.
		 */
		public byte[] getPakeRound2Data()
		{
			return pakeround2data;
		}
		
		/**
		 *  Sets the PAKE Round 2 data.
		 *  
		 *  @param pakeround1data The PAKE Round 2 data.
		 */
		public void setPakeRound2Data(byte[] pakeround2data)
		{
			this.pakeround2data = pakeround2data;
		}
		
		/**
		 *  Gets the encapsulated secret.
		 *  
		 *  @return The encapsulated secret.
		 */
		public byte[] getEncapsulatedSecret()
		{
			return encapsulatedsecret;
		}
		
		/**
		 *  Sets the encapsulated secret.
		 *  
		 *  @param encapsulatedsecret The encapsulated secret.
		 */
		public void setEncapsulatedSecret(byte[] encapsulatedsecret)
		{
			this.encapsulatedsecret = encapsulatedsecret;
		}
		
		/**
		 *  Gets the group signatures of the public key.
		 *  
		 *  @return The group signatures of the public key.
		 */
		public List<GroupSignature> getGroupSigs()
		{
			return groupsigs;
		}
		
		/**
		 *  Sets the group signatures of the public key.
		 *  
		 *  @param groupsigs The network signatures of the public key.
		 */
		public void setGroupSigs(List<GroupSignature> groupsigs)
		{
			this.groupsigs = groupsigs;
		}

		/**
		 *  Gets the platform secret signatures.
		 *
		 *  @return The platform secret signatures.
		 */
		/*public Tuple2<AuthToken, AuthToken> getPlatformSecretSigs()
		{
			return platformsecretsigs;
		}*/

		/**
		 *  Sets the platform secret signatures.
		 *
		 *  @param platformsecretsigs The platform secret signatures.
		 */
		/*public void setPlatformSecretSigs(Tuple2<AuthToken, AuthToken> platformsecretsigs)
		{
			this.platformsecretsigs = platformsecretsigs;
		}*/
		
		/**
		 *  Gets the host name signature.
		 *  
		 *  @return The signature.
		 */
		public AuthToken getHostNameSig()
		{
			return hostnamesig;
		}
		
		/**
		 *  Gets the host name signature.
		 *  
		 *  @return The signature.
		 */
		public void setHostNameSig(AuthToken hostnamesig)
		{
			this.hostnamesig = hostnamesig;
		}
	}
	
	/**
	 *  Message signaling the handshake is done.
	 *
	 */
	protected static final class ReadyMessage extends BasicSecurityMessage
	{
		/**
		 *  Creates the message.
		 */
		public ReadyMessage()
		{
		}
		
		/**
		 *  Creates the message.
		 */
		public ReadyMessage(GlobalProcessIdentifier sender, String conversationid)
		{
			super(sender, conversationid);
		}
	}

	public static record ChaChaDecryptedMessage(long msgid, ComponentIdentifier receiver, byte[] msg) {};
	
	public record GroupSignature(ByteArrayWrapper groupnamehash, AuthToken signature) {};
}
