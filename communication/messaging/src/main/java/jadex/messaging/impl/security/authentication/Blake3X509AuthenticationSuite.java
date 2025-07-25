package jadex.messaging.impl.security.authentication;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jadex.common.SBinConv;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound1Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound2Payload;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.params.Blake3Parameters;
import org.bouncycastle.util.Pack;

import jadex.common.ByteArrayWrapper;
import jadex.common.SUtil;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.impl.security.SSecurity;
import jadex.messaging.impl.security.SecurityFeature;


/**
 *  Symmetric authentication based on Blake2b MACs.
 */
public class Blake3X509AuthenticationSuite implements IAuthenticationSuite
{
	/** Authentication Suite ID. */
	protected static final int AUTH_SUITE_ID = 93482108;
	
	/** Size of the MAC. */
	protected static final int MAC_SIZE = 32;
	
	/** Size of the derived key. */
	protected static final int DERIVED_KEY_SIZE = 32;
	
	/** Size of the salt. */
	protected static final int SALT_SIZE = 32;
	
//	protected static final EdDSAParameterSpec ED25519 = EdDSANamedCurveTable.getByName("Ed25519");
	
	/** State for password-authenticated key exchange. */
	protected Map<PasswordSecret, JadexJPakeParticipant> pakestate;
	
	/** The local process identifier. */
	protected GlobalProcessIdentifier localgpid;
	
	/**
	 *  Creates the suite.
	 */
	public Blake3X509AuthenticationSuite(GlobalProcessIdentifier localgpid)
	{
		pakestate = new HashMap<PasswordSecret, JadexJPakeParticipant>();
		this.localgpid = localgpid;
	}
	
	/**
	 *  Gets the authentication suite ID.
	 *  
	 *  @return The authentication suite ID.
	 */
	public int getId()
	{
		return AUTH_SUITE_ID;
	}
	
	/** 
	 *  Gets the first round of the password-authenticated key-exchange.
	 *  
	 *  @return First round payload.
	 */
	public byte[] getPakeRound1(SecurityFeature security, GlobalProcessIdentifier remoteid)
	{
		byte[] idsalt = new byte[64];
		SSecurity.getSecureRandom().nextBytes(idsalt);
//		System.out.println("ID SALT 1: " + Arrays.toString(idsalt));
		
		List<byte[]> groups = new ArrayList<byte[]>();
		Map<String, List<AbstractAuthenticationSecret>> groupmap = security.getGroups();
		if (groupmap != null && groupmap.size() > 0)
		{
			for (Map.Entry<String, List<AbstractAuthenticationSecret>> entry : groupmap.entrySet())
			{
				if (entry.getValue() != null)
				{
					for (AbstractAuthenticationSecret secret : entry.getValue())
					{
						if (secret instanceof PasswordSecret)
						{
							JadexJPakeParticipant jpake = createJPakeParticipant(localgpid.toString(),((PasswordSecret) secret).getPassword());
							pakestate.put((PasswordSecret) secret, jpake);
							JPAKERound1Payload r1pl = jpake.createRound1PayloadToSend();
							
							groups.add(createSaltedId(entry.getKey(), idsalt));
							groups.add(round1ToBytes(r1pl));
						}
					}
				}
			}
		}
		
		byte[] nwbytes = new byte[0];
		if (groups.size() > 0)
			nwbytes = SBinConv.mergeData(groups.toArray(new byte[groups.size()][]));
		
		return SBinConv.mergeData(idsalt, nwbytes);
	}
	
	/** 
	 *  Gets the second round of the password-authenticated key-exchange.
	 *  
	 *  @return Second round payload.
	 */
	public byte[] getPakeRound2(SecurityFeature security, GlobalProcessIdentifier remoteid, byte[] round1data)
	{
		List<byte[]> r1list = SBinConv.splitData(round1data);
		if (r1list.size() != 2)
			throw new IllegalArgumentException("Illegal round 1 data.");
		
		byte[] idsalt = r1list.get(0);
//		System.out.println("ID SALT 2: " + Arrays.toString(idsalt));
		
		Map<String, List<AbstractAuthenticationSecret>> groupmap = security.getGroups();
		List<byte[]> groups = new ArrayList<byte[]>();
		if (r1list.get(1).length > 0 && groupmap.size() > 0)
		{ 
			Map<ByteArrayWrapper, List<PasswordSecret>> reversemap = new HashMap<ByteArrayWrapper, List<PasswordSecret>>();
			
			for (Map.Entry<String, List<AbstractAuthenticationSecret>> entry : groupmap.entrySet())
			{
				if (entry.getValue() != null)
				{
					for (AbstractAuthenticationSecret secret : entry.getValue())
					{
						if (secret instanceof PasswordSecret)
						{
							ByteArrayWrapper key = new ByteArrayWrapper(createSaltedId(entry.getKey(), idsalt));
							List<PasswordSecret> list = reversemap.get(key);
							if (list == null)
							{
								list = new ArrayList<>();
								reversemap.put(key, list);
							}
							list.add((PasswordSecret) secret);
						}
					}
				}
			}
			
			List<byte[]> gloads = SBinConv.splitData(r1list.get(1));
			if (gloads.size() % 2 > 0)
				throw new IllegalArgumentException("Illegal round 1 data.");
			
			for (int i = 0; i < gloads.size(); i = i + 2)
			{
				ByteArrayWrapper key = new ByteArrayWrapper(gloads.get(i));
				Collection<PasswordSecret> secrets = reversemap.get(key);
				if (secrets != null)
				{
					for (PasswordSecret secret : secrets)
					{
						JadexJPakeParticipant part = pakestate.get(secret);
						
						if (part != null)
						{
							JPAKERound1Payload r1 = bytesToRound1(gloads.get(i + 1));
							
							try
							{
								part.validateRound1PayloadReceived(r1);
								groups.add(key.getArray());
								groups.add(round2ToBytes(part.createRound2PayloadToSend()));
							}
							catch (Exception e)
							{
							}
						}
					}
				}
			}
		}
		
		byte[] gbytes = new byte[0];
		if (groups.size() > 0)
			gbytes = SBinConv.mergeData(groups.toArray(new byte[groups.size()][]));
		
		return SBinConv.mergeData(idsalt, gbytes);
	}
	
	/**
	 *  Finalizes the password-authenticated key exchange.
	 */
	public void finalizePake(SecurityFeature security, GlobalProcessIdentifier remoteid, byte[] round2data)
	{
		List<byte[]> r2list = SBinConv.splitData(round2data);
		if (r2list.size() != 2)
			throw new IllegalArgumentException("Illegal finalization data.");
		
		byte[] idsalt = r2list.get(0);
		
		Map<String, List<AbstractAuthenticationSecret>> groupmap = security.getGroups();
		if (r2list.get(1).length > 0 && groupmap.size() > 0)
		{
			Map<ByteArrayWrapper, JadexJPakeParticipant> reversemap = new HashMap<ByteArrayWrapper, JadexJPakeParticipant>();
			
			for (Map.Entry<String, List<AbstractAuthenticationSecret>> entry : groupmap.entrySet())
			{
				if (entry.getValue() != null)
				{
					for (AbstractAuthenticationSecret secret : entry.getValue())
					{
						if (secret instanceof PasswordSecret)
						{
							JadexJPakeParticipant p = pakestate.get(secret);
							if (p != null)
								reversemap.put(new ByteArrayWrapper(createSaltedId(entry.getKey(), idsalt)), p);
						}
					}
				}
			}
			
			List<byte[]> gloads = SBinConv.splitData(r2list.get(1));
			if (gloads.size() % 2 > 0)
				throw new IllegalArgumentException("Illegal finalization data.");
			
			for (int i = 0; i < gloads.size(); i = i + 2)
			{
				ByteArrayWrapper key = new ByteArrayWrapper(gloads.get(i));
				JadexJPakeParticipant part = reversemap.get(key);
				JPAKERound2Payload r2 = bytesToRound2(gloads.get(i + 1));
				
				try
				{
					part.validateRound2PayloadReceived(r2);
					part.calculateKeyingMaterial();
				}
				catch (Exception e)
				{
				}
			}
		}
		
		for (Iterator<Map.Entry<PasswordSecret, JadexJPakeParticipant>> it = pakestate.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<PasswordSecret, JadexJPakeParticipant> entry = it.next();
			if (entry.getValue() == null || entry.getValue().getDerivedKey() == null)
				it.remove();
		}
	}
	
	/**
	 *  Creates an authentication token for a message based on an abstract 
	 *  implementation-dependent "key".
	 *  
	 *  @param msg The message being authenticated.
	 *  @param secret The secret used for authentication.
	 *  @return Authentication token.
	 */
	public AuthToken createAuthenticationToken(byte[] msg, AbstractAuthenticationSecret secret)
	{
		AuthToken ret = null;
		
		// Generate random salt.
		byte[] salt = new byte[SALT_SIZE];
		SSecurity.getSecureRandom().nextBytes(salt);
		
		// Hash the message.
		byte[] msghash = getMessageHash(msg, salt);
		
		if (secret instanceof SharedSecret)
		{
			SharedSecret ssecret = (SharedSecret) secret;
			
			byte[] dk = null;
			
			if (secret instanceof PasswordSecret)
			{
				JadexJPakeParticipant jpake = pakestate.get(secret);
				
				if (jpake != null)
					dk = jpake.getDerivedKey();
				else
					return null;
					//throw new SecurityException("J-PAKE key not found for password " + secret);
			}
			else
			{
				dk = ssecret.deriveKey(DERIVED_KEY_SIZE, salt);
			}
			
			if (dk == null)
				return ret;
			
			// Generate MAC used for authentication.
			Blake3Digest blake3 = new Blake3Digest();
			blake3.init(Blake3Parameters.key(dk));
			byte[] mac = new byte[SALT_SIZE + MAC_SIZE + 4];
			Pack.intToLittleEndian(AUTH_SUITE_ID, mac, 0);
			System.arraycopy(salt, 0, mac, 4, salt.length);
			blake3.update(msghash, 0, msghash.length);
			blake3.doFinal(mac, salt.length + 4);
			ret = new AuthToken();
			ret.setAuthData(mac);
		}
		else if (secret instanceof AbstractX509PemSecret)
		{
			InputStream is = null;
			try
			{
				AbstractX509PemSecret aps = (AbstractX509PemSecret) secret;
				if (!aps.canSign())
					throw new IllegalArgumentException("Secret cannot be used to sign: " + aps);
				
				byte[] sig = SSecurity.signWithPEM(msghash, aps.openCertificate(), aps.openPrivateKey());
				is = aps.openCertificate();
				String cert = new String(SUtil.readStream(is), SUtil.UTF8);
				byte[] authdata = new byte[sig.length + SALT_SIZE + 4];
				Pack.intToLittleEndian(AUTH_SUITE_ID, authdata, 0);
				System.arraycopy(salt, 0, authdata, 4, salt.length);
				System.arraycopy(sig, 0, authdata, 4 + salt.length, sig.length);
				ret = new X509AuthToken();
				((X509AuthToken) ret).setCertificate(cert);
				ret.setAuthData(authdata);
			}
			catch (Exception e)
			{
				ret = null;
			}
			finally
			{
				SUtil.close(is);
			}
		}
		else
		{
			throw new IllegalArgumentException("Unknown secret type: " + secret);
		}
		
		// Generate authenticator: Salt, MAC.
		return ret;
	}
	
	/**
	 *  Creates an authentication token for a message based on an abstract 
	 *  implementation-dependent "key".
	 *  
	 *  @param msg The message being authenticated.
	 *  @param secret The secret used for authentication.
	 *  @param authtoken Authentication token.
	 *  @return True if authenticated, false otherwise.
	 */
	public boolean verifyAuthenticationToken(byte[] msg, AbstractAuthenticationSecret secret, AuthToken authtoken)
	{
		boolean ret = false;
		try
		{
			if (Pack.littleEndianToInt(authtoken.getAuthData(), 0) != AUTH_SUITE_ID)
				return false;
			
			if (secret instanceof SharedSecret)
			{
				SharedSecret ssecret = (SharedSecret) secret;
				
				if (authtoken.getAuthData().length != SALT_SIZE + MAC_SIZE + 4)
					return false;
				
				// Decode token.
				byte[] salt = new byte[SALT_SIZE];
				System.arraycopy(authtoken.getAuthData(), 4, salt, 0, salt.length);
				
				byte[] msghash = getMessageHash(msg, salt);
				
				byte[] mac = new byte[MAC_SIZE];
				System.arraycopy(authtoken.getAuthData(), SALT_SIZE + 4, mac, 0, mac.length);
				
				// Derive the  key.
				byte[] dk = null;
				if (ssecret instanceof PasswordSecret)
				{
					JadexJPakeParticipant jpake = pakestate.get(secret);
					
					if (jpake != null)
						dk = jpake.getDerivedKey();
				}
				else
				{
					dk = ssecret.deriveKey(DERIVED_KEY_SIZE, salt);
				}
				
				if (dk == null)
					return false;
				
				// Generate MAC
				Blake3Digest blake3 = new Blake3Digest();
				blake3.init(Blake3Parameters.key(dk));
				byte[] gmac = new byte[MAC_SIZE];
				blake3.update(msghash, 0, msghash.length);
				blake3.doFinal(gmac, 0);
				ret = Arrays.equals(gmac, mac);
			}
			else if (secret instanceof AbstractX509PemSecret && authtoken instanceof X509AuthToken)
			{
				// Decode token.
				byte[] salt = new byte[SALT_SIZE];
				System.arraycopy(authtoken.getAuthData(), 4, salt, 0, salt.length);
				
				byte[] msghash = getMessageHash(msg, salt);
				
				AbstractX509PemSecret aps = (AbstractX509PemSecret) secret;
				byte[] sig = new byte[authtoken.getAuthData().length - 4 - salt.length];
				System.arraycopy(authtoken.getAuthData(), 4 + salt.length, sig, 0, sig.length);
				
				String apscert = new String(SUtil.readStream(aps.openCertificate()), SUtil.UTF8);
				LinkedHashSet<X509CertificateHolder> apscertchain = new LinkedHashSet<>(SSecurity.readCertificateChainFromPEM(apscert));
				
				ret = SSecurity.verifyWithPEM(msghash, sig, ((X509AuthToken) authtoken).getCertificate(), apscertchain);
			}
			else
			{
				Logger.getLogger("authentication").warning("Unknown secret type: " + secret);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return ret;
	}
	
	/**
	 *  Create message hash.
	 * 
	 *  @param msg The message.
	 *  @return Hashed message.
	 */
	protected static final byte[] getMessageHash(byte[] msg, byte[] salt)
	{
		Blake3Digest blake3 = new Blake3Digest();
		byte[] msghash = new byte[32];
		blake3.update(msg, 0, msg.length);
		blake3.update(salt, 0, salt.length);
		blake3.doFinal(msghash, 0);
		return msghash;
	}
	
	/**
	 *  Creates a new participant for JPAKE.
	 *  
	 *  @param pid
	 *  @return
	 */
	protected static final JadexJPakeParticipant createJPakeParticipant(String pid, String password)
	{
		return new JadexJPakeParticipant(pid, password, new Blake3Digest());
	}
	
	/**
	 *  Encodes JPAKE round 1.
	 *  
	 *  @param r1pl JPAKE round 1.
	 *  @return Encoded round.
	 */
	protected static final byte[] round1ToBytes(JPAKERound1Payload r1pl)
	{
		byte[] pid = r1pl.getParticipantId().getBytes(SUtil.UTF8);
		byte[] gx1 = r1pl.getGx1().toByteArray();
		byte[] gx2 = r1pl.getGx2().toByteArray();
		byte[] kpx1 = bigIntegerArrayToByteArray(r1pl.getKnowledgeProofForX1());
		byte[] kpx2 = bigIntegerArrayToByteArray(r1pl.getKnowledgeProofForX2());
		
		return SBinConv.mergeData(pid, gx1, gx2, kpx1, kpx2);
	}
	
	/**
	 *  Decodes JPAKE round 1.
	 *  
	 *  @param bytes Encoded round.
	 *  @return JPAKE round 1.
	 */
	protected static final JPAKERound1Payload bytesToRound1(byte[] bytes)
	{
		List<byte[]> list = SBinConv.splitData(bytes);
		
		if (list.size() != 5)
			throw new IllegalArgumentException("Failed to decode round 1 payload.");
		
		return new JPAKERound1Payload(new String(list.get(0), SUtil.UTF8),
									  new BigInteger(list.get(1)),
									  new BigInteger(list.get(2)),
									  byteArrayToBigIntegerArray(list.get(3)),
									  byteArrayToBigIntegerArray(list.get(4)));
	}
	
	/**
	 *  Encodes JPAKE round 2.
	 *  
	 *  @param r1pl JPAKE round 2.
	 *  @return Encoded round.
	 */
	protected static final byte[] round2ToBytes(JPAKERound2Payload r2pl)
	{
		byte[] pid = r2pl.getParticipantId().getBytes(SUtil.UTF8);
		byte[] a = r2pl.getA().toByteArray();
		byte[] kpx2 = bigIntegerArrayToByteArray(r2pl.getKnowledgeProofForX2s());
		
		return SBinConv.mergeData(pid, a, kpx2);
	}
	
	/**
	 *  Decodes JPAKE round 2.
	 *  
	 *  @param bytes Encoded round.
	 *  @return JPAKE round 2.
	 */
	protected static final JPAKERound2Payload bytesToRound2(byte[] bytes)
	{
		List<byte[]> list = SBinConv.splitData(bytes);
		
		if (list.size() != 3)
			throw new IllegalArgumentException("Failed to decode round 1 payload.");
		
		return new JPAKERound2Payload(new String(list.get(0), SUtil.UTF8),
									  new BigInteger(list.get(1)),
									  byteArrayToBigIntegerArray(list.get(2)));
	}
	
	/**
	 *  Hashes an id with a salt.
	 *  
	 *  @param id The clear id.
	 *  @param idsalt The salt.
	 *  @return Salted ID.
	 */
	protected byte[] createSaltedId(String id, byte[] idsalt)
	{
		byte[] idbytes = id.getBytes(SUtil.UTF8);
		Blake3Digest digest = new Blake3Digest();
		digest.update(idsalt, 0, idsalt.length);
		digest.update(idbytes, 0, idbytes.length);
		
		byte[] ret = new byte[32];
		digest.doFinal(ret, 0);
		
		return ret;
	}
	
	/**
	 *  Converts a big integer array to a byte array.
	 *  
	 *  @param bigintarr Big integer array.
	 *  @return Byte array.
	 */
	protected static final byte[] bigIntegerArrayToByteArray(BigInteger[] bigintarr)
	{
		byte[][] list = new byte[bigintarr.length][];
		
		for (int i = 0; i < list.length; ++i)
			list[i] = bigintarr[i].toByteArray();
		
		return SBinConv.mergeData(list);
	}
	
	/**
	 *  Converts a byte array back into a big integer array.
	 *  
	 *  @param bytes The byte array.
	 *  @return The big integer array
	 */
	protected static final BigInteger[] byteArrayToBigIntegerArray(byte[] bytes)
	{
		List<byte[]> list = SBinConv.splitData(bytes);
		BigInteger[] ret = new BigInteger[list.size()];
		
		for (int i = 0; i < ret.length; ++i)
			ret[i] = new BigInteger(list.get(i));
		
		return ret;
	}

}
