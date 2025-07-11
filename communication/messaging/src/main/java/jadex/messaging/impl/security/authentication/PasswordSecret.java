package jadex.messaging.impl.security.authentication;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Pack;

import jadex.messaging.impl.security.SSecurity;

/**
 *  A secret password used for authentication.
 *
 */
public class PasswordSecret extends SharedSecret
{
	/** Password length weakness threshold. */
	public static final int MIN_GOOD_PASSWORD_LENGTH = 12;
	
	/** Prefix used to encode secret type as strings.*/
	public static final String PREFIX = "pw";
	
	/** The password. */
	protected String password;
	
	/**
	 *  Creates the secret.
	 */
	public PasswordSecret()
	{
	}
	
	/**
	 *  Creates the secret.
	 */
	public PasswordSecret(String encodedpassword)
	{
		this(encodedpassword, true);
	}
	
	/**
	 *  Creates the secret.
	 */
	public PasswordSecret(String password, boolean encoded)
	{
		if (encoded)
		{
			int ind = password.indexOf(':');
			String prefix = password.substring(0, ind);
			
			if (!PREFIX.startsWith(prefix))
				throw new IllegalArgumentException("Not a password secret: " + password);
			this.password = password.substring(ind + 1);
		}
		else
		{
			this.password = password;
		}
	}
	
	/**
	 *  Gets the password.
	 *  
	 *  @return The password.
	 */
	public String getPassword()
	{
		return password;
	}
	
	/**
	 *  Sets the password.
	 *  
	 *  @param password The password.
	 */
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	/**
	 *  Gets the key derivation parameters.
	 *  @return Key derivation parameters.
	 */
	public byte[] getKdfParams()
	{
		//byte[] ret = new byte[16];
		byte[] ret = new byte[12];
		
		Pack.intToBigEndian(SSecurity.ARGON_MEM, ret, 4);
		Pack.intToBigEndian(SSecurity.ARGON_IT, ret, 8);
		
		return ret;
	}
	
	/**
	 *  Derives a key from the password with appropriate hardening.
	 *  
	 *  @param keysize The target key size in bytes to generate.
	 *  @param salt Salt to use.
	 *  @return Derived key.
	 */
	public byte[] deriveKey(int keysize, byte[] salt)
	{
		return SSecurity.deriveKeyFromPassword(password, salt, keysize);
	}
	
	/**
	 *  Derives a key from the password with appropriate hardening.
	 *  
	 *  @param keysize The target key size in bytes to generate.
	 *  @param salt Salt to use.
	 *  @param dfparams Key derivation parameters.
	 *  @return Derived key.
	 */
	public byte[] deriveKey(int keysize, byte[] salt, byte[] dfparams)
	{
		if (dfparams.length != 12)
			return null;
		
		byte[] pw = password.getBytes(StandardCharsets.UTF_8);
		
		int mem = Pack.bigEndianToInt(dfparams, 4);
		int it = Pack.bigEndianToInt(dfparams, 8);
		
		Argon2BytesGenerator argen = new Argon2BytesGenerator();
		Argon2Parameters params = (new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id))
			.withMemoryAsKB(mem)
			.withParallelism(1)
			.withSalt(salt)
			.withIterations(it)
			.build();
		argen.init(params);
		
		byte[] ret = new byte[keysize];
		argen.generateBytes(pw, ret);
		
		return ret;
	}
	
	/**
	 *  Returns if the password is weak.
	 *  
	 *  @return True, if weak.
	 */
	public boolean isWeak()
	{
		return password.length() < MIN_GOOD_PASSWORD_LENGTH;
	}
	
	/** 
	 *  Creates encoded secret.
	 *  
	 *  @return Encoded secret.
	 */
	public String toString()
	{
		return PREFIX + ":" + password;
	}
}
