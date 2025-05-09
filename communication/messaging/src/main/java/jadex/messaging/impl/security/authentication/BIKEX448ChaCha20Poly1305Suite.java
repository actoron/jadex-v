package jadex.messaging.impl.security.authentication;

import java.util.Arrays;
import java.util.List;

import jadex.common.SBinConv;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.agreement.X448Agreement;
import org.bouncycastle.crypto.digests.Blake3Digest;
import org.bouncycastle.crypto.generators.X448KeyPairGenerator;
import org.bouncycastle.crypto.params.X448KeyGenerationParameters;
import org.bouncycastle.crypto.params.X448PublicKeyParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEKEMExtractor;
import org.bouncycastle.pqc.crypto.bike.BIKEKEMGenerator;
import org.bouncycastle.pqc.crypto.bike.BIKEKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEKeyPairGenerator;
import org.bouncycastle.pqc.crypto.bike.BIKEParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEPublicKeyParameters;

import jadex.common.SUtil;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.security.SSecurity;


/**
 *  Crypto suite combining BIKE and X448 for generating a ChaCha20-Poly1305 AEAD key.
 *
 */
public class BIKEX448ChaCha20Poly1305Suite extends AbstractChaCha20Poly1305Suite
{
	/** Key generated for symmetric key exchange. */
	protected GeneratedBIKEX448Key generatedkey;
	
	/** The locally generated BIKE secret. */
	protected SecretWithEncapsulation locallygeneratedbikesecret;
	
	/** The remote x448 public key. */
	protected byte[] x448remotepublickey;
	
	/**
	 *  Creates the suite.
	 */
	public BIKEX448ChaCha20Poly1305Suite(GlobalProcessIdentifier localgpid)
	{
		super(localgpid);
	}
	
	/**
	 *  Gets the encoded ephemeral public key.
	 * 
	 */
	protected byte[] getPubKey()
	{
		if (generatedkey == null)
		{
			BIKEKeyPairGenerator bkg = new BIKEKeyPairGenerator();
			bkg.init(new BIKEKeyGenerationParameters(SSecurity.getSecureRandom(), BIKEParameters.bike256));
			AsymmetricCipherKeyPair bkp = bkg.generateKeyPair();
			
			X448KeyPairGenerator xkg = new X448KeyPairGenerator();
			xkg.init(new X448KeyGenerationParameters(SSecurity.getSecureRandom()));
			AsymmetricCipherKeyPair xkp = xkg.generateKeyPair();
			
			byte[] pubkey = SBinConv.mergeData(((BIKEPublicKeyParameters) bkp.getPublic()).getEncoded(),
											((X448PublicKeyParameters) xkp.getPublic()).getEncoded());
			
			generatedkey = new GeneratedBIKEX448Key(bkp, xkp, pubkey);
		}
		
		return generatedkey.pubkey();
	}
	
	/**
	 *  Gets the local secret encapsulated by the remote public key.
	 * 
	 *  @return The local encapsulated secret.
	 */
	protected byte[] getEncapsulatedSecret()
	{
		if (remotepublickey == null)
			throw new IllegalStateException("Requested encapsulated secret without public key");
		
		List<byte[]> bremotepubkeys = SBinConv.splitData(remotepublickey);
		BIKEPublicKeyParameters bikeparams = new BIKEPublicKeyParameters(BIKEParameters.bike256, bremotepubkeys.get(0));
		x448remotepublickey = bremotepubkeys.get(1);
		
		BIKEKEMGenerator bgen = new BIKEKEMGenerator(SSecurity.getSecureRandom());
		locallygeneratedbikesecret = bgen.generateEncapsulated(bikeparams);
		
		return locallygeneratedbikesecret.getEncapsulation();
	}
	
	/**
	 *  Creates the ChaCha key.
	 *  
	 *  @return The ChaCha key.
	 */
	protected byte[] createChaChaKey(byte[] encapsulatedsecret)
	{
		BIKEKEMExtractor bkext = new BIKEKEMExtractor((BIKEPrivateKeyParameters) generatedkey.bikekey().getPrivate());
		byte[] remotebikesecret = bkext.extractSecret(encapsulatedsecret);
		
		X448Agreement x448agreement = new X448Agreement();
		x448agreement.init(generatedkey.x448key.getPrivate());
		byte[] x448secret = new byte[x448agreement.getAgreementSize()];
		x448agreement.calculateAgreement(new X448PublicKeyParameters(x448remotepublickey), x448secret, 0);
		
		Blake3Digest dig = new Blake3Digest();
		dig.update(remotebikesecret, 0, remotebikesecret.length);
		dig.update(locallygeneratedbikesecret.getSecret(), 0, locallygeneratedbikesecret.getSecret().length);
		byte[] bikeremotelocal = new byte[32];
		dig.doFinal(bikeremotelocal, 0);
		
		dig = new Blake3Digest();
		dig.update(locallygeneratedbikesecret.getSecret(), 0, locallygeneratedbikesecret.getSecret().length);
		dig.update(remotebikesecret, 0, remotebikesecret.length);
		byte[] bikelocalremote = new byte[32];
		dig.doFinal(bikelocalremote, 0);
		
		byte[] bikesecret = SSecurity.xor(bikelocalremote, bikeremotelocal);
		
		dig = new Blake3Digest();
		dig.update(bikesecret, 0, bikesecret.length);
		dig.update(x448secret, 0, x448secret.length);
		byte[] rawchacha = new byte[32];
		dig.doFinal(rawchacha, 0);
		
		locallygeneratedbikesecret = null;
		x448agreement = null;
		x448remotepublickey = null;
		
		return rawchacha; // convertToChaChaKey(rawchacha);
	}
	
	public static void main(String[] args)
	{
		BIKEX448ChaCha20Poly1305Suite suite1 = new BIKEX448ChaCha20Poly1305Suite(new GlobalProcessIdentifier(100, GlobalProcessIdentifier.SELF.host()));
		BIKEX448ChaCha20Poly1305Suite suite2 = new BIKEX448ChaCha20Poly1305Suite(new GlobalProcessIdentifier(200, GlobalProcessIdentifier.SELF.host()));
		
		suite2.remotepublickey =  suite1.getPubKey();
		suite1.remotepublickey =  suite2.getPubKey();
		
		byte[] encsec1 = suite1.getEncapsulatedSecret();
		byte[] encsec2 = suite2.getEncapsulatedSecret();
		
		byte[] chacha1 = suite1.createChaChaKey(encsec2);
		byte[] chacha2 = suite2.createChaChaKey(encsec1);
		
		System.out.println(Arrays.toString(chacha1));
		System.out.println(Arrays.toString(chacha2));
	}
	
	private record GeneratedBIKEX448Key(AsymmetricCipherKeyPair bikekey, AsymmetricCipherKeyPair x448key, byte[] pubkey) {};
}
