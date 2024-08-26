package jadex.ipc.impl.security.authentication;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.agreement.X448Agreement;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.crypto.generators.X448KeyPairGenerator;
import org.bouncycastle.crypto.params.X448KeyGenerationParameters;
import org.bouncycastle.crypto.params.X448PublicKeyParameters;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.pqc.crypto.ExchangePair;
import org.bouncycastle.pqc.crypto.bike.BIKEKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEKeyPairGenerator;
import org.bouncycastle.pqc.crypto.bike.BIKEParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEPublicKeyParameters;
import org.bouncycastle.pqc.crypto.newhope.NHAgreement;
import org.bouncycastle.pqc.crypto.newhope.NHExchangePairGenerator;
import org.bouncycastle.pqc.crypto.newhope.NHKeyPairGenerator;
import org.bouncycastle.pqc.crypto.newhope.NHPublicKeyParameters;
import org.bouncycastle.pqc.crypto.util.PrivateKeyFactory;
import org.bouncycastle.pqc.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.pqc.crypto.util.PublicKeyFactory;
import org.bouncycastle.pqc.crypto.util.SubjectPublicKeyInfoFactory;

import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.Tuple3;
import jadex.ipc.impl.security.SSecurity;


/**
 *  Crypto suite combining NewHope and Curve448 for key exchange and ChaCha20-Poly1305 AEAD.
 *
 */
public class NHCurve448ChaCha20Poly1305Suite extends AbstractChaCha20Poly1305Suite
{
	
	
	/**
	 *  Creates the suite.
	 */
	public NHCurve448ChaCha20Poly1305Suite()
	{
	}
	
	/**
	 *  Gets the encoded public key.
	 * 
	 *  @param ephemeralkey The ephemeral key.
	 */
	protected byte[] getPubKey()
	{
		BikeX448KeyPairs keys = (BikeX448KeyPairs) ephemeralkey;
		byte[] bikepub = ((BIKEPublicKeyParameters) keys.bikekeys.getPublic()).getEncoded();
		byte[] x448pub = ((X448PublicKeyParameters) keys.x448keys.getPublic()).getEncoded();
		
		byte[] ret = null;
		if (remotepublickey != null)
		{
			List<byte[]> rkeydata = SUtil.splitData(remotepublickey);
			BIKEPublicKeyParameters rbikekey = new BIKEPublicKeyParameters(BIKEParameters.bike256, rkeydata.get(0));
			X448PublicKeyParameters rx448key = new X448PublicKeyParameters(rkeydata.get(1));
			
			
		}

		/*byte[] c448priv = null;
		
		byte[] nhpub = null;
		if (ephemeralkey instanceof Tuple2)
		{
			@SuppressWarnings("unchecked")
			Tuple2<ExchangePair, byte[]> tup = (Tuple2<ExchangePair, byte[]>) ephemeralkey;
			nhpub = ((NHPublicKeyParameters)tup.getFirstEntity().getPublicKey()).getPubData();
			c448priv = tup.getSecondEntity();
		}
		else
		{
			@SuppressWarnings("unchecked")
			Tuple3<byte[], NHAgreement, byte[]> tup = (Tuple3<byte[], NHAgreement, byte[]>) ephemeralkey;
			nhpub = tup.getFirstEntity();
			c448priv = tup.getThirdEntity();
		}
		
		byte[] c448pub = new byte[56];
		Curve448.eval(c448pub, 0, c448priv, Curve448ChaCha20Poly1305Suite.CURVE448_CONST_5);
		
		return SUtil.mergeData(nhpub, c448pub);*/
	}
	
	private static record BikeX448KeyPairs(AsymmetricCipherKeyPair bikekeys, AsymmetricCipherKeyPair x448keys) {}; 
	
	/**
	 *  Creates the ephemeral key.
	 *  
	 *  @return The ephemeral key.
	 */
	protected Object createEphemeralKey()
	{
		X448KeyPairGenerator xkpg = new X448KeyPairGenerator();
		xkpg.init(new X448KeyGenerationParameters(SSecurity.getSecureRandom()));
		AsymmetricCipherKeyPair xkeypair = xkpg.generateKeyPair();
		
		BIKEKeyPairGenerator bkpg = new BIKEKeyPairGenerator();
		bkpg.init(new BIKEKeyGenerationParameters(SSecurity.getSecureRandom(), BIKEParameters.bike256));
		AsymmetricCipherKeyPair bkeypair1 = bkpg.generateKeyPair();
		
		return new BikeX448KeyPairs(bkeypair1, xkeypair);
		
		/*byte[] c448 = new byte[56];
		SSecurity.getSecureRandom().nextBytes(c448);
		
		if (remotepublickey == null)
		{
			X448KeyPairGenerator xkpg = new X448KeyPairGenerator();
			X448KeyGenerationParameters xkgp = new X448KeyGenerationParameters(SSecurity.getSecureRandom());
			AsymmetricCipherKeyPair xkeypair = xkpg.generateKeyPair();
			//X448Agreement
			
			BIKEKeyPairGenerator bkpg = new BIKEKeyPairGenerator();
			BIKEKeyGenerationParameters bkgp = new BIKEKeyGenerationParameters(SSecurity.getSecureRandom(), BIKEParameters.bike256);
			AsymmetricCipherKeyPair bkeypair = bkpg.generateKeyPair();
			
			BIKEPublicKeyParameters bpubkey = null;
			BIKEPrivateKeyParameters bprivkey = null;
			try
			{
				bpubkey = (BIKEPublicKeyParameters)PublicKeyFactory.createKey(SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo((BIKEPublicKeyParameters)keypair.getPublic()));
				bprivkey = (BIKEPrivateKeyParameters)PrivateKeyFactory.createKey(PrivateKeyInfoFactory.createPrivateKeyInfo((BIKEPrivateKeyParameters)keypair.getPrivate()));
			}
			catch (Exception e)
			{
				SUtil.throwUnchecked(e);
			}
			
			
			NHKeyPairGenerator kpg = new NHKeyPairGenerator();
			kpg.init(new KeyGenerationParameters(SSecurity.getSecureRandom(), -1));
			
			
			NHAgreement nhagree = new NHAgreement();
			nhagree.init(keypair.getPrivate());
			ret = new Tuple3<byte[], NHAgreement, byte[]>(((NHPublicKeyParameters) keypair.getPublic()).getPubData(), nhagree, c448);
			
		}
		else
		{
			List<byte[]> pubkeys = SUtil.splitData(remotepublickey);
			NHExchangePairGenerator ekpg = new NHExchangePairGenerator(SSecurity.getSecureRandom());
			ret = new Tuple2<ExchangePair, byte[]>(ekpg.generateExchange(new NHPublicKeyParameters(pubkeys.get(0))), c448);
		}
		return ret;*/
	}
	
	/**
	 *  Generates the shared public key.
	 *  
	 *  @param remotepubkey The remote public key.
	 *  @return Shared key.
	 */
	protected byte[] generateSharedKey()
	{
		List<byte[]> pubkeys = SUtil.splitData(remotepublickey);
		
		byte[] nhshared = null;
		byte[] c448 = null;
		if (ephemeralkey instanceof Tuple2)
		{
			@SuppressWarnings("unchecked")
			Tuple2<ExchangePair, byte[]> tup = (Tuple2<ExchangePair, byte[]>) ephemeralkey;
			nhshared = tup.getFirstEntity().getSharedValue();
			c448 = tup.getSecondEntity();
		}
		else
		{
			@SuppressWarnings("unchecked")
			Tuple3<byte[], NHAgreement, byte[]> tup = (Tuple3<byte[], NHAgreement, byte[]>) ephemeralkey;
			NHAgreement nhagree = tup.getSecondEntity();
			nhshared = nhagree.calculateAgreement(new NHPublicKeyParameters(pubkeys.get(0)));
			c448 = tup.getThirdEntity();
		}
		
		byte[] ret = new byte[56];
		if (!Curve448.eval(ret, 0, c448, pubkeys.get(1)))
			throw new SecurityException("Curve448 Handshake failed");
		
		Blake2bDigest blake2b = new Blake2bDigest(512);
		blake2b.update(ret, 0, ret.length);
		blake2b.update(nhshared, 0, nhshared.length);
		ret = new byte[64];
		blake2b.doFinal(ret, 0);
		
		return ret;
	}
	
	/**
	 *  Destroy information.
	 */
	public void destroy()
	{
		if (ephemeralkey instanceof Tuple2)
		{
			@SuppressWarnings("unchecked")
			Tuple2<ExchangePair, byte[]> tup = (Tuple2<ExchangePair, byte[]>) ephemeralkey;
			SSecurity.getSecureRandom().nextBytes(tup.getSecondEntity());
		}
		else if (ephemeralkey instanceof Tuple3)
		{
			@SuppressWarnings("unchecked")
			Tuple3<byte[], NHAgreement, byte[]> tup = (Tuple3<byte[], NHAgreement, byte[]>) ephemeralkey;
			SSecurity.getSecureRandom().nextBytes(tup.getFirstEntity());
			SSecurity.getSecureRandom().nextBytes(tup.getThirdEntity());
		}
		super.destroy();
	}
	
	public static void main(String[] args)
	{
		NHKeyPairGenerator kpg = new NHKeyPairGenerator();
		kpg.init(new KeyGenerationParameters(SSecurity.getSecureRandom(), -1));
		
		AsymmetricCipherKeyPair keypair1 = kpg.generateKeyPair();
		NHAgreement na1 = new NHAgreement();
		na1.init(keypair1.getPrivate());
		
		NHExchangePairGenerator ekpg2 = new NHExchangePairGenerator(SSecurity.getSecureRandom());
		ExchangePair ep2 = ekpg2.generateExchange(keypair1.getPublic());
		byte[] shared2 = ep2.getSharedValue();
		
		byte[] shared1 = na1.calculateAgreement(ep2.getPublicKey());
		
		System.out.println(Arrays.toString(shared1));
		System.out.println(Arrays.toString(shared2));
	}
}
