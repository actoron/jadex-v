package jadex.messaging.impl.security;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jadex.messaging.IMessageFeature;
import jadex.messaging.impl.security.authentication.KeySecret;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.messaging.IIpcFeature;
import jadex.messaging.ISecurityFeature;
import jadex.messaging.impl.security.authentication.AbstractAuthenticationSecret;
import jadex.messaging.impl.security.authentication.AbstractX509PemSecret;
import jadex.messaging.impl.security.authentication.BIKEX448ChaCha20Poly1305Suite;
import jadex.messaging.impl.ipc.IpcFeature;
import jadex.messaging.impl.security.handshake.BasicSecurityMessage;
import jadex.messaging.impl.security.handshake.InitialHandshakeFinalMessage;
import jadex.messaging.impl.security.handshake.InitialHandshakeMessage;
import jadex.messaging.impl.security.handshake.InitialHandshakeReplyMessage;
import jadex.messaging.impl.security.random.SecureThreadedRandom;
import jadex.serialization.ISerializationServices;
//import jadex.providedservice.impl.service.ISecurityHandler;

/**
 *  Security functionality for active component communication.
 *  Performs authentication and 
 */
public class SecurityFeature implements ISecurityFeature//, ISecurityHandler
{
	private static final String LOCAL_GROUP_KEYFILE = "localgroup.key";

	/**
	 *  Does some global initialization when the service is requested the first time.
	 *  Replaces the SUtil default secure random with one that has higher performace.
	 */
	static
	{
		SUtil.SECURE_RANDOM = new SecureThreadedRandom();
	}
	
	/** The singleton instance. */
	public static volatile SecurityFeature security;
	
	/**
	 *  Get the security instance.
	 */
	/*public static final Security get()
	{
		if(security==null)
		{
			synchronized(Security.class)
			{
				security = new Security(GlobalProcessIdentifier.SELF, IpcStreamHandler.get());
			}
		}
		return security;
	}*/
	
	/** The IPC stream handler. */
	protected IpcFeature ipc;
	
	/** The global process identifier. */
	protected GlobalProcessIdentifier gpid;
	
	/** 
	 *  Flag whether to grant default authorization
	 *  (allow basic service calls if host name or group is authenticated).
	 */
	protected boolean defaultauthorization = true;
	
	/** Flag whether to refuse unauthenticated connections. */
	protected boolean refuseuntrusted = false;
	
	/** Flag if connection with hosts without authenticated names are allowed. */
	protected boolean allownoauthname = true;
	
	/** Flag if connection without authenticated group are allowed. */
	protected boolean allownogroup = true;
	
	/** Flag whether to use the default Java trust store. */
	protected boolean loadjavatruststore = true;
	
	/** Flag if the security should create a random default network
	 *  if no network is set.
	 */
	protected boolean createdefaultnetwork = true;
	
	/** Handshake timeout. */
	protected long handshaketimeout = 20000;
	
	/** 
	 *  Lifetime of session keys, after which the handshake is repeated
	 *  and a new session key is generated.
	 */
	protected long sessionkeylifetime = 10 * 60 * 1000; // 10 minutes
	
	/** Available groups. */
	protected Map<String, List<AbstractAuthenticationSecret>> groups = new HashMap<>();
	
	/** The host name certificate if available. */
	protected AbstractX509PemSecret hostnamecertificate;
	
	/** The host names that are trusted and identified by name. */
	protected Set<String> trustedhosts = new HashSet<>();
	
	/** Trusted authorities for certifying platform names. */
	protected Set<X509CertificateHolder> nameauthorities = new HashSet<>();
	
	/** Custom (non-Java default) trusted authorities for certifying platform names. */
	protected Set<X509CertificateHolder> customnameauthorities = new HashSet<>();
	
	/** Available crypto suites. */
	protected Map<String, Class<?>> allowedcryptosuites = new LinkedHashMap<String, Class<?>>();
	
	/** CryptoSuites currently initializing, value=Handshake state. */
	protected RwMapWrapper<GlobalProcessIdentifier, HandshakeState> initializingcryptosuites = new RwMapWrapper<>(new HashMap<>());
	
	/** CryptoSuites currently in use. */
	// TODO: Expiration / configurable LRU required to mitigate DOS attacks.
	protected RwMapWrapper<GlobalProcessIdentifier, ICryptoSuite> currentcryptosuites = new RwMapWrapper<>(new HashMap<>());
	
	/** CryptoSuites that are expiring with expiration time. */
	protected RwMapWrapper<GlobalProcessIdentifier, List<ExpiringCryptoSuite>> expiringcryptosuites = new RwMapWrapper<>(new HashMap<>());
	
	/** Map of entities and associated roles. */
	protected Map<String, Set<String>> roles = new HashMap<String, Set<String>>();
	
	/** Crypto-Suite reset in progress. */
	protected IFuture<Void> cryptoreset; 
	
	/** Last time cleanup duties were performed. */
	protected volatile long lastcleanup;

	/** Flag if local group loading is enabled. */
	protected boolean localgroup = true;
	
	/** The list of group names (used by all service identifiers). */
	protected Set<String> groupnames = new HashSet<>();

	/** The list of group names excluded from default authorization. */
	protected Set<String> nodefaultauthgroups = new HashSet<>(Arrays.asList(new String[] { ISecurityFeature.UNRESTRICTED }));
	
	public SecurityFeature(GlobalProcessIdentifier gpid, IIpcFeature ipc)
	{
		this.gpid = gpid;
		this.ipc = (IpcFeature) ipc;
		
		this.ipc.setSecurityMessageHandler((msg) -> 
		{
			handleMessage(msg);
		});
		
		allowedcryptosuites = new LinkedHashMap<String, Class<?>>();
		allowedcryptosuites.put(BIKEX448ChaCha20Poly1305Suite.class.getName(), BIKEX448ChaCha20Poly1305Suite.class);
		if (loadjavatruststore)
		{
			String tst = System.getProperty("javax.net.ssl.trustStoreType");
			String tsf = System.getProperty("javax.net.ssl.trustStore");
			String tsp = System.getProperty("javax.net.ssl.trustStorePassword");
			
			if (tsf == null && tst == null)
			{
				String javahome = System.getProperty("java.home");
				Path path = Paths.get(javahome, "lib", "security", "jssecacerts");
	            if (!path.toFile().exists())
	            {
	            	path = Paths.get(javahome, "lib", "security", "cacerts");
	            }
				if (path.toFile().exists())
				{
					try
					{
						tsf = path.toFile().getCanonicalPath();
					}
					catch (IOException e)
					{
					}
				}
			}
			
			if (tsp == null)
				tsp = "changeit";
			if (tst == null)
				tst = KeyStore.getDefaultType();
			
			if (tst != null && tsf != null)
			{
				JcaPEMWriter jpw = null;
				try
				{
					KeyStore ks = KeyStore.getInstance(tst);
					InputStream is = null;
					try
					{
						is = new FileInputStream(tsf);
						is = new BufferedInputStream(is);
						ks.load(is, tsp.toCharArray());
						SUtil.close(is);
					}
					catch (Exception e)
					{
					}
					finally
					{
						SUtil.close(is);
					}
					
					Enumeration<String> aliases = ks.aliases();
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					OutputStreamWriter osw = new OutputStreamWriter(baos);
					jpw = new JcaPEMWriter(osw);
					while (aliases.hasMoreElements())
					{
						try
						{
							String alias = aliases.nextElement();
							Certificate cert = ks.getCertificate(alias);
							jpw.writeObject(cert);
							jpw.flush();
							String pem = new String(baos.toByteArray(), SUtil.ASCII);
							baos.reset();
							try
							{
								nameauthorities.add(SSecurity.readCertificateFromPEM(pem));
							}
							catch (Exception e)
							{
							}
						}
						catch (Exception e)
						{
						}
					}
//					ts = System.currentTimeMillis() - ts;
//					System.out.println("READING TOOK " + ts);
				}
				catch (Exception e)
				{
				}
				finally
				{
					SUtil.close(jpw);
				}
			}
		}
	}
	
	//---- ISecurityService methods. ----
	
	/**
	 *  Encrypts and signs the message for a receiver.
	 *  
	 *  @param receiver The receiver.
	 *  @return Encrypted/signed message.
	 */
	public byte[] encryptAndSign(ComponentIdentifier receiver, byte[] message)
	{
		checkCleanup();
		
		ICryptoSuite cs = currentcryptosuites.get(receiver.getGlobalProcessIdentifier());
		if(cs != null && !cs.isExpiring())
			return cs.encryptAndSign(receiver, message);
		
		byte[] ret = null;
		HandshakeState hstate = null;
		synchronized(this)
		{
			doCleanup();
			
			//String rplat = ((IComponentIdentifier) header.getProperty(IMsgHeader.RECEIVER)).getRoot().toString();
			
			cs = currentcryptosuites.get(receiver.getGlobalProcessIdentifier());
			
			if (cs != null && cs.isExpiring())
			{
				if(cs.equals(currentcryptosuites.get(receiver.getGlobalProcessIdentifier())))
				{
					if(SUtil.DEBUG)
						System.out.println("Expiring: "+receiver);
					expireCryptosuite(receiver.getGlobalProcessIdentifier());
					cs = null;
				}
			}
			
			if (cs != null)
			{
				ret = cs.encryptAndSign(receiver, message);
			}
			else
			{
				hstate = initializingcryptosuites.get(receiver.getGlobalProcessIdentifier());
				if(hstate == null)
				{
					if (SUtil.DEBUG)
						System.out.println("Handshake state null, starting new handhake: "+gpid+" "+receiver.getGlobalProcessIdentifier());
					//System.out.println(initializingcryptosuites+" "+System.identityHashCode(initializingcryptosuites));
					initializeHandshake(receiver.getGlobalProcessIdentifier());
					hstate = initializingcryptosuites.get(receiver.getGlobalProcessIdentifier());
				}
			}
		}
		
		if (ret == null)
		{
			IFuture<ICryptoSuite> rfut = hstate != null ? hstate.getResultFuture() : null;
			cs = rfut != null ? rfut.get() : null;
			if (cs != null)
			{
				ret = cs.encryptAndSign(receiver, message);
			}
			else
			{
				throw new UncheckedIOException(new IOException("Communication with " + receiver + " failed."));
			}
		}
		
		return ret;
	}
	
	/**
	 *  Decrypt and authenticates the message from a sender.
	 *  
	 *  @param sender The sender.
	 *  @return Decrypted/authenticated message or null on invalid message.
	 */
	public DecodedMessage decryptAndAuth(GlobalProcessIdentifier sender, byte[] message)
	{
//		System.out.println("received: "+sender+" at "+agent.getId());
		
		checkCleanup();
		
		if (message == null || message.length == 0)
			throw new IllegalArgumentException("Null messages and zero length messages cannot be decrypted.");
		
		ICryptoSuite cs = currentcryptosuites.get(sender);
		if (cs != null && message[0] != -1)
		{
			byte[] cleartext = cs.decryptAndAuth(message);
			if (cleartext != null)
				return new DecodedMessage(cs.getSecurityInfos(), cleartext);
		}
		
		synchronized(this)
		{
			doCleanup();
			
			Future<DecodedMessage> ret = new Future<>();
			
			cs = currentcryptosuites.get(sender);
			byte[] cleartext = null;
			
			if (cs != null)
			{
				cleartext = cs.decryptAndAuth(message);
			}
			
			if (cleartext == null)
			{
				try (IAutoLock l = expiringcryptosuites.readLock())
				{
					List<ExpiringCryptoSuite> explist = expiringcryptosuites.get(sender);
					if (explist != null)
					{
						for (ExpiringCryptoSuite exp : explist)
						{
							cs = exp.suite();
							cleartext = cs.decryptAndAuth(message);
							if (cleartext != null)
								break;
						}
					}
				}
			}
			
			if (cleartext == null)
			{
				HandshakeState hstate = initializingcryptosuites.get(sender);
				if (hstate != null)
				{
					//final byte[] fmessage = message;
					cs = hstate.getResultFuture().get();
					cleartext = cs.decryptAndAuth(message);
					if(cleartext != null)
					{
						ret.setResult(new DecodedMessage(cs.getSecurityInfos(), cleartext));
					}
					else
					{
						//TODO: Implement reencryption
						throw new UnsupportedOperationException("Failed to decrypt message to " + sender + ", reencryption request currently unimplemented");
						/*requestReencryption(sender, message).addResultListener(new IResultListener<byte[]>()
						{
							public void resultAvailable(byte[] result)
							{
								ICryptoSuite cs = currentcryptosuites.get(sender);
								if (cs != null)
									ret.setResult(new DecodedMessage(cs.getSecurityInfos(), result));
								else
									ret.setException(new SecurityException("Could not establish secure communication with (case 1): " + sender + "  " + message));
							};
							
							public void exceptionOccurred(Exception exception)
							{
								ret.setException(exception);
							}
						});*/
					}
					
//					hstate.getResultFuture().addResultListener(new IResultListener<ICryptoSuite>()
//					{
//						public void resultAvailable(ICryptoSuite result)
//						{
//							byte[] cleartext = result.decryptAndAuth(message);
//							if(cleartext != null)
//							{
//								ret.setResult(new DecodedMessage(result.getSecurityInfos(), cleartext));
//							}
//							else
//							{
//								//TODO: Implement reencryption
//								throw new UnsupportedOperationException("Failed to decrypt message to " + sender + ", reencryption request currently unimplemented");
//								/*requestReencryption(sender, message).addResultListener(new IResultListener<byte[]>()
//								{
//									public void resultAvailable(byte[] result)
//									{
//										ICryptoSuite cs = currentcryptosuites.get(sender);
//										if (cs != null)
//											ret.setResult(new DecodedMessage(cs.getSecurityInfos(), result));
//										else
//											ret.setException(new SecurityException("Could not establish secure communication with (case 1): " + sender + "  " + message));
//									};
//									
//									public void exceptionOccurred(Exception exception)
//									{
//										ret.setException(exception);
//									}
//								});*/
//							}
//						}
//						
//						public void exceptionOccurred(Exception exception)
//						{
//							ret.setException(exception);
//						}
//					});
				}
				else
				{
					//TODO: Implement reencryption
					throw new UnsupportedOperationException("Failed to decrypt message to " + sender + ", reencryption request currently unimplemented");
					//requestReencryption(splat, content).addResultListener(new IResultListener<byte[]>() {});
					/*{
						public void resultAvailable(byte[] result)
						{
							ICryptoSuite cs = currentcryptosuites.get(sender);
							if (cs != null)
								ret.setResult(new Tuple2<ISecurityInfo, byte[]>(cs.getSecurityInfos(), result));
							else
								ret.setException(new SecurityException("Could not establish secure communication with (case 2): " + splat.toString() + "  " + content));
						};
						
						public void exceptionOccurred(Exception exception)
						{
							ret.setException(exception);
						}
					});*/
				}
			}
			
			if (cleartext != null)
			{
				return new DecodedMessage(cs.getSecurityInfos(), cleartext);
				//ret.setResult(new Tuple2<ISecurityInfo, byte[]>(cs.getSecurityInfos(), cleartext));
			}
		}
		throw new RuntimeException("Failed to decrypt message from " + sender);
	}
	
	/**
	 *  Checks if host secret is used.
	 *  
	 *  @return True, if a host secret is in use.
	 */
	/*public boolean isUseHostSecret()
	{
		return usesecret;
	}*/
	
	/**
	 *  Sets whether the platform secret should be used.
	 *  
	 *  @param useplatformsecret The flag.
	 */
	/*public void setUsePlatformSecret(final boolean useplatformsecret)
	{
		synchronized(this)
		{
			usesecret = useplatformsecret;
			resetCryptoSuites();
		}
	}*/
	
	/**
	 *  Adds a new group.
	 * 
	 *  @param groupname The group name.
	 *  @param secret The string-encoded secret.
	 */
	public void addGroup(String groupname, String secret)
	{
		if(secret==null || secret.isEmpty())
			throw new IllegalArgumentException("Secret is null or zero length.");

		AbstractAuthenticationSecret asecret = null;
		try
		{
			AbstractAuthenticationSecret.fromString(secret, true);
		}
		catch (IllegalArgumentException e)
		{
			ComponentManager.get().getLogger(SecurityFeature.class).log(System.Logger.Level.WARNING, "Interpreting String secret as password, consider using jadex.messaging.security.authentication.PasswordSecret.");
		}
		addGroup(groupname, asecret);
	}
	
	/**
	 *  Adds a new group.
	 * 
	 *  @param groupname The group name.
	 *  @param asecret The secret.
	 */
	public void addGroup(String groupname, AbstractAuthenticationSecret asecret)
	{
		if(groupname==null || groupname.length()==0)
			throw new IllegalArgumentException("Networkname is null.");
		if(asecret==null)
			throw new IllegalArgumentException("Secret is empty.");
		
		synchronized(this)
		{
			Collection<AbstractAuthenticationSecret> secrets = groups.get(groupname);
			if(secrets != null && secrets.contains(asecret))
				return;
			
			//System.out.println("groupnames before: "+groupnames);
			List<AbstractAuthenticationSecret> groupsecrets = groups.get(groupname);
			if (groupsecrets == null)
			{
				groupsecrets = new ArrayList<>();
				groups.put(groupname, groupsecrets);
			}
			groupsecrets.add(asecret);
			if(asecret.canSign())
				groupnames.add(groupname);
			
			//System.out.println("groupnames after: "+groupnames);
			
			//ServiceRegistry.getRegistry(agent.getId().getRoot()).updateService(null, "networks");
			
			// TODO: Still needed?
			//ServiceRegistry.getRegistry(agent.getId().getRoot()).updateService(null);
			
			resetCryptoSuites();
			//return IFuture.DONE;
		}
	}
	
	/**
	 *  Get access to the stored groups.
	 * 
	 *  @return The stored groups.
	 */
	public Map<String, List<AbstractAuthenticationSecret>> getGroups()
	{
		Map<String, List<AbstractAuthenticationSecret>> ret = new HashMap<>();
		synchronized(this)
		{
			for (Map.Entry<String, List<AbstractAuthenticationSecret>> entry : groups.entrySet())
				ret.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return ret;
	}
	
	/**
	 *  Remove a group or group secret.
	 * 
	 *  @param groupname The network name.
	 *  @param secret The secret, null to remove the group completely.
	 *  @return Null, when done.
	 */
	public void removeGroup(String groupname, String secret)
	{
		synchronized(this)
		{
			if(secret == null)
			{
				groups.remove(groupname);
				groupnames.remove(groupname);
			}
			else
			{
				Collection<AbstractAuthenticationSecret> secrets = groups.get(groupname);
				secrets.remove(AbstractAuthenticationSecret.fromString(secret));
				if (secrets.isEmpty())
				{
					groups.remove(groupname);
					groupnames.remove(groupname);
				}
				else
				{
					boolean removename = true;
					for (AbstractAuthenticationSecret csecret : secrets)
					{
						if (csecret.canSign())
						{
							removename = false;
							break;
						}
					}
					if (removename)
						groupnames.remove(groupname);
				}
			}
			resetCryptoSuites();
		}
	}

	/**
	 *  Marks a group as not part of the default authorization.
	 *
	 *  @param groupname The group name.
	 */
	public void addNoDefaultAuthorizationGroup(String groupname)
	{
		synchronized (this)
		{
			nodefaultauthgroups.add(groupname);
		}
	}

	/**
	 *  Unmarks a group as not part of the default authorization.
	 *
	 *  @param groupname The group name.
	 */
	public void removeNoDefaultAuthorizationGroup(String groupname)
	{
		synchronized (this)
		{
			nodefaultauthgroups.remove(groupname);
		}
	}

	/**
	 *  Returns the groups excluded from default authorization.
	 *  @return The groups excluded from default authorization.
	 */
	public Set<String> getNodefaultAuthorizationGroups()
	{
		return nodefaultauthgroups;
	}

	/**
	 *  Disable loading the local group. Must be invoked before messaging is used.
	 */
	public void disableLocalGroup()
	{
		localgroup = false;
	}
	
	/** 
	 *  Adds an authority for authenticating platform names.
	 *  
	 *  @param pemcertificate The pem-encoded certificate.
	 */
	public void addNameAuthority(String pemcertificate)
	{
		final X509CertificateHolder cert = SSecurity.readCertificateFromPEM(pemcertificate);
		synchronized(this)
		{
			nameauthorities.add(cert);
			customnameauthorities.add(cert);
		}
	}
	
	/** 
	 *  Remvoes an authority for authenticating platform names.
	 *  
	 *  @param pemcertificate The secret, only X.509 secrets allowed.
	 */
	public void removeNameAuthority(String pemcertificate)
	{
		X509CertificateHolder cert = SSecurity.readCertificateFromPEM(pemcertificate);
		synchronized(this)
		{
			if (customnameauthorities.remove(cert))
				nameauthorities.remove(cert);
		}
	}
	
	/** 
	 *  Gets an authority for authenticating host names.
	 *
	 *  @return Null, when done.
	 */
	public Set<String> getNameAuthorities()
	{
		synchronized(this)
		{
			Set<String> ret = new HashSet<>();
			for (X509CertificateHolder cert : SUtil.notNull(nameauthorities))
				ret.add(SSecurity.writeCertificateAsPEM(cert));
			return ret;
		}
	}
	
	/** 
	 *  Gets an authority for authenticating host names.
	 *
	 *  @return Null, when done.
	 */
	public Set<X509CertificateHolder> getNameAuthorityCerts()
	{
		synchronized(this)
		{
			Set<X509CertificateHolder> ret = new HashSet<>(SUtil.notNull(nameauthorities));
			return ret;
		}
	}
	
	/** 
	 *  Gets all authorities not defined in the Java trust store for authenticating host names.
	 *  
	 *  @return List of name authorities.
	 */
	public Set<String> getCustomNameAuthorities()
	{
		synchronized(this)
		{
			Set<String> ret = new HashSet<>();
			for (X509CertificateHolder cert : SUtil.notNull(customnameauthorities))
				ret.add(SSecurity.writeCertificateAsPEM(cert));
			return ret;
		}
	}
	
	/**
	 *  Gets the current group names. 
	 *  @return The current group names.
	 */
	public Set<String> getGroupNames()
	{
		synchronized(this)
		{
			return new HashSet<>(groupnames);	
		}
	}
	
	/** 
	 *  Adds a name of an authenticated host to allow access.
	 *  
	 *  @param host The host name, host name must be authenticated with certificate.
	 */
	public void addTrustedHost(String host)
	{
		synchronized(this)
		{
			trustedhosts.add(host);
		}
	}
	
	/** 
	 *  Removes the name of an authenticated host to deny access.
	 *  
	 *  @param host The host name to remove.
	 */
	public void removeTrustedHost(String host)
	{
		synchronized(this)
		{
			trustedhosts.remove(host);
		}
	}
	
	/**
	 *  Gets the trusted platforms that are specified by names. 
	 *  @return The trusted platforms and their roles.
	 */
	public Set<String> getTrustedHosts()
	{
		synchronized(this)
		{
			return new HashSet<>(trustedhosts);
		}
	}
	
	/**
	 *  Tests if the host is a trusted host. 
	 *  @return True, if the host is trusted.
	 */
	public boolean isTrustedHosts(String host)
	{
		synchronized(this)
		{
			return trustedhosts.contains(host);
		}
	}
	
	/**
	 *  Adds a role for an entity (platform or network name).
	 *  
	 *  @param entity The entity name.
	 *  @param role The role name.
	 */
	public void addRole(final String entity, final String role)
	{
		synchronized(this)
		{
			Set<String> eroles = roles.get(entity);
			if (eroles == null)
			{
				eroles = new HashSet<String>();
				roles.put(entity, eroles);
			}
			
			eroles.add(role);
			
			refreshCryptosuiteRoles();
		}
	}
	
	/**
	 *  Adds a role of an entity (platform or network name).
	 *  
	 *  @param entity The entity name.
	 *  @param role The role name.
	 */
	public void removeRole(final String entity, final String role)
	{
		synchronized(this)
		{
			Set<String> eroles = roles.get(entity);
			if (eroles != null)
			{
				eroles.remove(role);
				if (eroles.isEmpty())
					roles.remove(entity);
			}
			
			refreshCryptosuiteRoles();
		}
	}

	/**
	 *  Loads the local group.
	 */
	public void loadLocalGroup()
	{
		if (localgroup)
		{
			String keyfileeof = "\n\n\n";

			Path socketdir = Path.of(System.getProperty("java.io.tmpdir")).resolve(IMessageFeature.COM_DIRECTORY_NAME);
			socketdir.toFile().mkdirs();

			File dir = socketdir.toFile();
			if (!dir.isDirectory() || !dir.canRead() || !dir.canWrite())
				throw new UncheckedIOException(new IOException("Cannot access communcation directory: " + dir.getAbsolutePath()));

			File localgroupfile = new File(dir.getAbsolutePath() + File.separator + LOCAL_GROUP_KEYFILE);

			String key = null;
			try
			{
				for (int i = 0; i < 10; ++i)
				{
					String lgcontent = new String(SUtil.readFile(localgroupfile), SUtil.UTF8);
					if (lgcontent.endsWith(keyfileeof))
					{
						key = lgcontent.trim();
						System.out.println("Using local group key " + key);
						break;
					}
					SUtil.sleep(100);
				}
			} catch (Exception e)
			{
				// Empty key is enough to deal with this.
			}

			try
			{
				if (key == null)
				{
					boolean res = localgroupfile.createNewFile();

					try (FileInputStream fis = new FileInputStream(localgroupfile);
						 FileOutputStream fos = new FileOutputStream(localgroupfile))
					{
						try (FileLock fl = fos.getChannel().lock(0, Long.MAX_VALUE, false))
						{
							String lgcontent = "";
							try
							{
								lgcontent = new String(SUtil.readFile(localgroupfile), SUtil.UTF8);
							} catch (Exception e)
							{
								// It's just a check and allowed to fail.
							}

							if (!lgcontent.endsWith(keyfileeof))
							{
								key = KeySecret.createRandom().toString();
								fos.write((key + keyfileeof).getBytes(SUtil.UTF8));
								fos.flush();
								System.out.println("Generated local group key " + key);
							}
						} catch (IOException e)
						{
							SUtil.rethrowAsUnchecked(e);
						}
					} catch (IOException e)
					{
						SUtil.rethrowAsUnchecked(e);
					}
				}
			}
			catch (Exception e)
			{
				ComponentManager.get().getLogger(SecurityFeature.class).log(System.Logger.Level.WARNING,
						"Failed to create local group key file, local group disabled: " + localgroupfile.getAbsolutePath());
				return;
			}

			addGroup(ISecurityFeature.LOCAL_GROUP, KeySecret.fromString(key));
		}
	}
	
	/**
	 *  Gets a copy of the current role map.
	 *  
	 *  @return Copy of the role map.
	 */
	public Map<String, Set<String>> getRoleMap()
	{
		synchronized(this)
		{
			return new HashMap<>(roles);
		}
	}

	/**
	 *  Gets a copy of a reverse role map (from roles to groups).
	 *
	 *  @return Copy of a reverse role map.
	 */
	public Map<String, Set<String>> getReverseRoleMap()
	{
		Map<String, Set<String>> rolemap = null;
		synchronized(this)
		{
			rolemap = new HashMap<>(roles);
		}

		Map<String, Set<String>> reverserolemap = new HashMap<>();
		for (Map.Entry<String, Set<String>> roleentry : rolemap.entrySet())
		{
			for (String group : roleentry.getValue())
			{
				Set<String> rolegroups = reverserolemap.get(roleentry.getKey());
				if (rolegroups == null)
				{
					rolegroups = new HashSet<>();
					reverserolemap.put(roleentry.getKey(), rolegroups);
				}
				rolegroups.add(group);
			}
		}

		return reverserolemap;
	}
	
	/**
	 *  Gets the name authorities.
	 */
	public Set<X509CertificateHolder> getInternalNameAuthorities()
	{
		return nameauthorities;
	}
	
	/**
	 *  Gets the trusted platform names.
	 */
	public Set<String> getInternalTrustedHosts()
	{
		return trustedhosts;
	}
	
	/**
	 *  Get the platform name certificate.
	 */
	public AbstractX509PemSecret getHostNameCertificate()
	{
		synchronized(this)
		{
			return hostnamecertificate;
		}
	}
	
	
	
	/**
	 *  Checks whether to use platform secret.
	 *  
	 *  @return True, if used.
	 */
//	public boolean getInternalUsePlatformSecret()
//	{
//		return usesecret;
//	}
	
	/**
	 *  Checks whether to allow untrusted connections.
	 *  
	 *  @return True, if used.
	 */
	public boolean getInternalRefuseUntrusted()
	{
		return refuseuntrusted;
	}
	
	/**
	 *  Checks whether to allow connections without name authentication.
	 *  
	 *  @return True, if used.
	 */
	public boolean getInternalAllowNoAuthName()
	{
		return allownoauthname;
	}
	
	/**
	 *  Checks whether to allow connections without group authentication.
	 *  
	 *  @return True, if used.
	 */
	public boolean getInternalAllowNoGroup()
	{
		return allownogroup;
	}
	
	/**
	 *  Checks whether to allow the default authorization.
	 *  
	 *  @return True, if used.
	 */
	public boolean isDefaultAuthorization()
	{
		synchronized (this) {
			return defaultauthorization;
		}
	}
	
	/**
	 *  Sets the roles of a security info object.
	 *  @param secinf Security info.
	 */
	public void setSecInfoMappedRoles(SecurityInfo secinf)
	{
//		assert agent.isComponentThread();
		Set<String> siroles = new HashSet<String>();
		
		if (secinf.getAuthenticatedHostName() != null)
		{
			Set<String> hostroles = roles.get(secinf.getAuthenticatedHostName());
			if (hostroles != null)
				siroles.addAll(hostroles);
			
			siroles.add(secinf.getAuthenticatedHostName());
		}
		
		
		if (secinf.getGroups() != null)
		{
			for (String group : secinf.getGroups())
			{
				Set<String> r = roles.get(group);
				if (r != null)
					siroles.addAll(r);
				//else
				// Always add group as role?
				siroles.add(group);
			}
		}

		// Admin role is automatically trusted.
		if (siroles.contains(ISecurityFeature.ADMIN))
			siroles.add(ISecurityFeature.TRUSTED);
		
		secinf.setMappedRoles(siroles);
	}
	
	/*
	 *  Checks receiver authorization and, if so, encrypts the message. Otherwise, an exception is issued.
	 *  
	 *  @param receiver Receive ID. 
	 *  @param content Message content.
	 *  @param cs The cryptosuite negotiated with receiver.
	 *  @param resultfuture Optional result future if it already exist, if null a future is created.
	 *  @return Result future containing encrypted message or exception.
	 */
	/*protected byte[] checkReceiverAndEncrypt(GlobalProcessIdentifier receiver, byte[] content, ICryptoSuite cs) //, Future<byte[]> resultfuture)
	{
		//TODO: Implement receiver authorization here.
		return cs.encryptAndSign(content);
		
		//Future<byte[]> ret = resultfuture != null ? resultfuture : new Future<>();
		//ISecurityInfo recinfo = cs.getSecurityInfos();
		//if (isReceiverAuthorized(header, cs.getSecurityInfos()))
			//ret.setResultIfUndone(cs.encryptAndSign(content));
//		else
//		{
//			String rplat = ((IComponentIdentifier) header.getProperty(IMsgHeader.RECEIVER)).getRoot().toString();
//			ret.setException(new SecurityException("Receiving platform " + rplat + " not authorized to receive message."));
//		}
		//return ret;
	}*/
	
	// -------- Cleanup
	
	protected void checkCleanup()
	{
		long delay = handshaketimeout << 2;
		if (lastcleanup + delay < System.currentTimeMillis())
		{
			synchronized(this)
			{
				if (lastcleanup + delay < System.currentTimeMillis())
				{
					lastcleanup = System.currentTimeMillis();
					doCleanup();
				}
			}
		}
	}
	
	/**
	 *  Cleans expired objects.
	 */
	protected void doCleanup()
	{
		// Note: Must have write lock.
		long time = System.currentTimeMillis();

		boolean hasexpiredsuites = false;
		Predicate<Map.Entry<GlobalProcessIdentifier, HandshakeState>> ishsexpired = ent -> time > ent.getValue().getExpirationTime();
		try (IAutoLock l = initializingcryptosuites.readLock())
		{
			hasexpiredsuites = initializingcryptosuites.entrySet().stream().anyMatch(ishsexpired);
		}

		if (hasexpiredsuites)
		{
			try (IAutoLock l = initializingcryptosuites.writeLock())
			{
				for (Iterator<Map.Entry<GlobalProcessIdentifier, HandshakeState>> it = initializingcryptosuites.entrySet().iterator(); it.hasNext(); )
				{
					Map.Entry<GlobalProcessIdentifier, HandshakeState> entry = it.next();
					if (time > entry.getValue().getExpirationTime())
					{
						entry.getValue().getResultFuture().setException(new TimeoutException("Handshake timed out with platform: " + entry.getKey()));
						//System.out.println("Removing handshake data: "+entry.getKey());
						it.remove();
					}
				}
			}
		}

		// Check for expired suites.
		// This is a two-step process because suites have a long lifespan after handshake,
		// i.e. typically there are no expired suites. In order to optimize locking, we
		// first check whether it is even worth to acquire a write lock by checking with a read lock.
		hasexpiredsuites = false;
		Predicate<Map.Entry<GlobalProcessIdentifier, ICryptoSuite>> isexpired = ent -> (ent.getValue().isExpiring() || (ent.getValue().getCreationTime() + sessionkeylifetime) < time);
		
		// Individual locks required because fast path does not lock the global lock.
		try (IAutoLock l = currentcryptosuites.readLock())
		{
			hasexpiredsuites = currentcryptosuites.entrySet().stream().anyMatch(isexpired);
		}
		
		// If we have something expired, we do a thorough check and clean
		// with a write lock in place.
		if (hasexpiredsuites)
		{
			try (IAutoLock l = currentcryptosuites.writeLock())
			{
				currentcryptosuites.entrySet().stream().filter(isexpired).map(e -> e.getKey()).collect(Collectors.toList()).forEach(this::expireCryptosuite);
			}
		}

		hasexpiredsuites = false;

		Predicate<Map.Entry<GlobalProcessIdentifier, List<ExpiringCryptoSuite>>> islisthsexpired = ent ->
				ent.getValue().stream().anyMatch((exp) -> time > exp.timeofexpiration());
		try (IAutoLock l = expiringcryptosuites.readLock())
		{
			hasexpiredsuites = expiringcryptosuites.entrySet().stream().anyMatch(islisthsexpired);
		}

		if (hasexpiredsuites)
		{
			try (IAutoLock l = expiringcryptosuites.writeLock())
			{
				for (GlobalProcessIdentifier gpid : expiringcryptosuites.keySet())
				{
					List<ExpiringCryptoSuite> explist = expiringcryptosuites.get(gpid);
					for (ExpiringCryptoSuite exp : explist)
					{
						if (time > exp.timeofexpiration())
						{
							explist.remove(exp);
							if (explist.isEmpty())
								expiringcryptosuites.remove(gpid);
						}
					}
				}
			}
		}
		
//		for (Iterator<Map.Entry<String, Tuple2<ICryptoSuite, Long>>> it = expiringcryptosuites.entrySet().iterator(); it.hasNext(); )
//		{
//			Map.Entry<String, Tuple2<ICryptoSuite, Long>> entry = it.next();
//			if (time > entry.getValue().getSecondEntity())
//				it.remove();
//		}
	}
	
	//-------- Utility functions -------
	
	/**
	 *  Resets the crypto suite in case of security state change (network secret changes etc.).
	 */
	protected void resetCryptoSuites()
	{
		synchronized(this)
		{
			final List<GlobalProcessIdentifier> gpids = new ArrayList<>();
			try (IAutoLock l = currentcryptosuites.writeLock())
			{
				gpids.addAll(currentcryptosuites.keySet());
				for (GlobalProcessIdentifier gpid : gpids)
					expireCryptosuite(gpid);
			}
			
			if (initializingcryptosuites.size() > 0)
			{
				// Best effort, expiring initializing connections later,
				// so change may not be effective immediately
				SUtil.getExecutor().execute(() ->
				{
					long resetdelay = (long) (handshaketimeout * 1.1);
					SUtil.sleep(resetdelay);
					resetCryptoSuites();
				});
			}
			else
			{
				for(GlobalProcessIdentifier gpid : gpids)
					initializeHandshake(gpid);
				cryptoreset = null;
				
				if(SUtil.DEBUG)
					System.out.println("Cryptosuites reset.");
			}
		}
	}
	
	/**
	 *  Creates a crypto suite of a particular name.
	 * 
	 *  @param name Name of the suite.
	 *  @param convid Conversation ID of handshake.
	 *  @param initializer True, if suite should represent the initializer.
	 *  @return The suite, null if not found.
	 */
	protected ICryptoSuite createCryptoSuite(String name, String convid, boolean initializer)
	{
		ICryptoSuite ret = null;
		try
		{
			Class<?> clazz = allowedcryptosuites.get(name);
			if (clazz != null)
			{
				ret = (ICryptoSuite) clazz.getConstructor(GlobalProcessIdentifier.class).newInstance(gpid);
				ret.setHandshakeId(convid);
				ret.setInitializer(initializer);
			}
		}
		catch (Exception e)
		{
		}
		return ret;
	}
	
	/**
	 *  Expires a cryptosuite.
	 * 
	 *  @param gpid Global process identifier of communication partner.
	 */
	protected void expireCryptosuite(GlobalProcessIdentifier gpid)
	{
		// Note: Must have global lock, individual lock also required
		//		 because fast path does not lock the global lock.
		try (IAutoLock l = currentcryptosuites.writeLock())
		{
			ICryptoSuite cs = currentcryptosuites.get(gpid);
			if (cs != null)
			{
				List<ExpiringCryptoSuite> explist = expiringcryptosuites.get(gpid);
				if (explist == null)
				{
					explist = new ArrayList<>();
					expiringcryptosuites.put(gpid, explist);
				}
				explist.add(new ExpiringCryptoSuite(cs, System.currentTimeMillis() + handshaketimeout));
				currentcryptosuites.remove(gpid);
			}
		}
	}
	
	/**
	 *  Refreshed crypto suite roles.
	 */
	protected void refreshCryptosuiteRoles()
	{
		// Note: Must have global write lock, also needs individual lock to lock out fast path.
		try (IAutoLock l = currentcryptosuites.writeLock())
		{
			for (Map.Entry<GlobalProcessIdentifier, ICryptoSuite> entry : currentcryptosuites.entrySet())
			{
				SecurityInfo secinfo = ((SecurityInfo) entry.getValue().getSecurityInfos());
				setSecInfoMappedRoles(secinfo);
			}
			
			for (Map.Entry<GlobalProcessIdentifier, HandshakeState> entry : initializingcryptosuites.entrySet())
			{
				HandshakeState state = entry.getValue();
				if (state != null)
				{
					ICryptoSuite suite = state.getCryptoSuite();
					if (suite != null)
					{
						SecurityInfo secinfo = (SecurityInfo) suite.getSecurityInfos();
						if (secinfo != null)
							setSecInfoMappedRoles(secinfo);
					}
				}
			}
		}
	}
	
	/**
	 *  Sends a security handshake message.
	 * 
	 *  @param receiver Receiver of the message.
	 *  @param message The message.
	 *  @return Null, when sent.
	 */
	public void sendSecurityHandshakeMessage(GlobalProcessIdentifier receiver, BasicSecurityMessage message)
	{
		message.setMessageId(SUtil.createUniqueId());
		if(SUtil.DEBUG)
			System.out.println("sendSecurityHandshakeMessage0: sending handshake message to: "+receiver+" "+message.getMessageId());
		
		try
		{
			sendSecurityMessage(receiver, message);
			if(SUtil.DEBUG)
				System.out.println("sendSecurityHandshakeMessage2: sent handshake message to: "+receiver+" "+message.getMessageId());
		}
		catch (Exception e)
		{
			if(SUtil.DEBUG)
			{
				System.out.println("sendSecurityHandshakeMessage1: Failure send message to and removing suite for: "+receiver.toString()+" "+message.getMessageId()+"\n"+SUtil.getExceptionStacktrace(e));
			}
			
			//System.out.println("Removing Handshake " + receiver.getRoot().toString()+" "+message.getMessageId());
			HandshakeState state = initializingcryptosuites.remove(receiver);
			if(state != null)
			{
				// Return the actual  exception (likely communication error) instead of a made-up security exception.
				// This is a communication error, not a security error.
				state.getResultFuture().setException(e);
			}
		}
		
	}
	
	/**
	 *  Init handshake with other platform.
	 *  @param remotegpid The global process id.
	 */
	protected void initializeHandshake(GlobalProcessIdentifier remotegpid)
	{
		String convid = SUtil.createUniqueId(gpid.toString());
		HandshakeState hstate = new HandshakeState();
		hstate.setExpirationTime(System.currentTimeMillis() + handshaketimeout);
		hstate.setConversationId(convid);
		hstate.setResultFuture(new Future<ICryptoSuite>());
		if(SUtil.DEBUG)
			System.out.println("Security.initializeHandshake0 " +gpid+" "+convid+" "+handshaketimeout);
		
		initializingcryptosuites.put(remotegpid, hstate);
		
		String[] csuites = allowedcryptosuites.keySet().toArray(new String[allowedcryptosuites.size()]);
		InitialHandshakeMessage ihm = new InitialHandshakeMessage(gpid, convid, csuites);
		if(SUtil.DEBUG)
			System.out.println("Security.initializeHandshake1 " + convid + " " + gpid + " -> " + remotegpid + " Phase: 0 Step: 0 "+initializingcryptosuites+" "+System.identityHashCode(initializingcryptosuites));
		sendSecurityHandshakeMessage(remotegpid, ihm);
	}

	/**
	 *  Returns the allowed access groups from a given set of roles of a Security annotation.
	 *  @param annotationroles Roles specied in the Security annotation.
	 *  @return Groups representing those roles.
	 */
	public Set<String> getPermittedGroups(Set<String> annotationroles)
	{
		Set<String> pgroups = new HashSet<>();
		if (annotationroles == null || annotationroles.size() == 0)
		{
			if (defaultauthorization)
			{
				synchronized (this)
				{
					pgroups.addAll(groups.keySet());
				}
			}
		}
		else
		{
			Map<String, Set<String>> rrm = getReverseRoleMap();
			for (String role : annotationroles)
				pgroups.addAll(rrm.get(role));

			// Remove local group
			if (localgroup)
				pgroups.remove(LOCAL_GROUP);
		}
		return pgroups;
	}
	
	/**
	 *  Sends a security message.
	 * 
	 *  @param receiver Receiver of the message.
	 *  @param message The message.
	 *  @return Null, when sent.
	 */
	protected void sendSecurityMessage(GlobalProcessIdentifier receiver, Object message)
	{
		ComponentIdentifier secid = new ComponentIdentifier(null, receiver);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ISerializationServices.get().encode(baos, ComponentManager.get().getClassLoader(), message);
		ipc.sendMessage(secid, baos.toByteArray());
	}
	
	//-------- Message Handling -------
	
	/**
	 *  Handle security handshake message.
	 *  @param message The message.
	 */
	public void handleMessage(byte[] message)
	{
		Object msg = null;
		try {
			msg = ISerializationServices.get().decode(new ByteArrayInputStream(message), getClass().getClassLoader());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(1);;
		}
		if (msg instanceof InitialHandshakeMessage)
		{
			final InitialHandshakeMessage imsg = (InitialHandshakeMessage) msg;
			GlobalProcessIdentifier sender = imsg.getSender();
			
			if(SUtil.DEBUG)
				System.out.println("handleMessage: initial handshake message: "+gpid+" "+sender+" "+msg);
			
			final Future<ICryptoSuite> fut = new Future<ICryptoSuite>();
			
			HandshakeState state = initializingcryptosuites.get(sender);
			
			// Check if handshake is already happening. 
			if(state != null)
			{
				// Check if duplicate
				if(!state.getConversationId().equals(imsg.getConversationId()))
				{
					if(gpid.toString().compareTo(sender.toString()) < 0)
					{
						fut.addResultListener(new DelegationResultListener<ICryptoSuite>(state.getResultFuture()));
					}
					else
					{
						if(SUtil.DEBUG)
							System.out.println("handleMessage exit: tie break");
						return;
					}
				}
				else
				{
					if(SUtil.DEBUG)
						System.out.println("handleMessage exit: same convid");
					return;
				}
			}
			
			if(imsg.getCryptoSuites() == null || imsg.getCryptoSuites().length < 1)
			{
				if(SUtil.DEBUG)
					System.out.println("handleMessage exit: no crypto suites1");
				return;
			}
			
			String[] offeredsuites = imsg.getCryptoSuites();
			
			String chosensuite = null;
			if (offeredsuites != null)
			{
				for (String suite : offeredsuites)
				{
					if (allowedcryptosuites.containsKey(suite))
					{
						chosensuite = suite;
						break;
					}
				}
			}
			
			if(chosensuite == null)
			{
				if(SUtil.DEBUG)
					System.out.println("handleMessage exit: no crypto suites2");
				return;
			}
			state = new HandshakeState();
			state.setResultFuture(fut);
			state.setConversationId(imsg.getConversationId());
			state.setExpirationTime(System.currentTimeMillis() + handshaketimeout);
			
			ICryptoSuite oldcs = currentcryptosuites.get(sender);
			if (oldcs != null)
			{
				try (IAutoLock l = currentcryptosuites.writeLock())
				{
					if (oldcs.equals(currentcryptosuites.get(sender)))
					{
						// Test for duplicate.
						if (oldcs.getHandshakeId().equals(imsg.getConversationId()))
						{
							if(SUtil.DEBUG)
								System.out.println("handleMessage exit: dup");
							return;
						}
						
						if(SUtil.DEBUG)
							System.out.println("New handshake, removing existing suite: "+sender);
						expireCryptosuite(sender);
					}
				}
			}
			
			initializingcryptosuites.put(sender, state);
			
//			ICryptoSuite oldcs = currentcryptosuites_old.remove(rplat.toString());
//			if (oldcs != null)
//			{
//				System.out.println("Removing suite: "+rplat);
//				expiringcryptosuites.add(rplat.toString(), new Tuple2<ICryptoSuite, Long>(oldcs, System.currentTimeMillis() + timeout));
//			}
			
			InitialHandshakeReplyMessage reply = new InitialHandshakeReplyMessage(gpid, state.getConversationId(), chosensuite);
			
			if(SUtil.DEBUG)
				System.out.println("Security Handshake " + imsg.getConversationId() + " " + gpid + " -> " + sender + " Phase: 0 Step: 1");
			//System.out.println(initializingcryptosuites+" "+System.identityHashCode(initializingcryptosuites));
			sendSecurityHandshakeMessage(imsg.getSender(), reply);
		}
		else if (msg instanceof InitialHandshakeReplyMessage)
		{
			InitialHandshakeReplyMessage rm = (InitialHandshakeReplyMessage) msg;
			HandshakeState state = initializingcryptosuites.get(rm.getSender());
			
			if(state != null)
			{
				String convid = state.getConversationId();
				if (convid != null && convid.equals(rm.getConversationId()) && !state.isDuplicate(rm))
				{
					ICryptoSuite suite = createCryptoSuite(rm.getChosenCryptoSuite(), convid, true);
					
					if (suite == null)
					{
						if(SUtil.DEBUG)
							System.out.println("Removing Handshake " + rm.getConversationId() + ", reason: no matching cryptosuites 1.");
						initializingcryptosuites.remove(rm.getSender());
						state.getResultFuture().setException(new SecurityException("Handshake with remote process " + rm.getSender() + " failed."));
					}
					else
					{
						state.setCryptoSuite(suite);
						InitialHandshakeFinalMessage fm = new InitialHandshakeFinalMessage(gpid, rm.getConversationId(), rm.getChosenCryptoSuite());
						if(SUtil.DEBUG)
							System.out.println("Security Handshake " + convid + " " + gpid + " -> " + rm.getSender() + " Phase: 0 Step: 2, finished Phase 0, entering Phase 1");
						sendSecurityHandshakeMessage(rm.getSender(), fm);
					}
				}
			}
			
		}
		else if (msg instanceof InitialHandshakeFinalMessage)
		{
			InitialHandshakeFinalMessage fm = (InitialHandshakeFinalMessage) msg;
			HandshakeState state = initializingcryptosuites.get(fm.getSender());
			if (state != null)
			{
				String convid = state.getConversationId();
				if(SUtil.DEBUG)
					System.out.println("Security Handshake " + convid + " " + gpid + " -> " + fm.getSender() + " finished Phase 0, entering Phase 1");
				if (convid != null && convid.equals(fm.getConversationId()) && !state.isDuplicate(fm))
				{
					ICryptoSuite suite = createCryptoSuite(fm.getChosenCryptoSuite(), convid, false);
					//agent.getLogger().info("Suite: " + (suite != null?suite.getClass().toString():"null"));
					
					if (suite == null)
					{
						if(SUtil.DEBUG)
							System.out.println("Removing Handshake " + fm.getConversationId() + ", reason: no matching cryptosuites 2.");
						initializingcryptosuites.remove(fm.getSender());
						state.getResultFuture().setException(new SecurityException("Handshake with remote platform " + fm.getSender() + " failed."));
					}
					else
					{
						state.setCryptoSuite(suite);
						if (!suite.handleHandshake(SecurityFeature.this, fm))
						{
							if(SUtil.DEBUG)
								System.out.println(gpid+" finished handshake: " + fm.getSender());
							currentcryptosuites.put(fm.getSender(), state.getCryptoSuite());
							//if(debug)
							//System.out.println("Removing Handshake " + fm.getConversationId() + ", reason: finished handshake.");
							initializingcryptosuites.remove(fm.getSender());
							state.getResultFuture().setResult(state.getCryptoSuite());
							
						}
					}
				}
			}
		}
		else if (msg instanceof BasicSecurityMessage)
		{
			BasicSecurityMessage secmsg = (BasicSecurityMessage) msg;
			HandshakeState state = initializingcryptosuites.get(secmsg.getSender());
			if (state != null && state.getConversationId().equals(secmsg.getConversationId()) && state.getCryptoSuite() != null)
			{
				String convid = state.getConversationId();
				if (convid != null && convid.equals(secmsg.getConversationId()) && !state.isDuplicate(secmsg))
				{
					try
					{
						if(SUtil.DEBUG)
							System.out.println("Security Handshake " + convid + " " + gpid + " -> " + secmsg.getSender() + " processing Phase 1 step");
						if (!state.getCryptoSuite().handleHandshake(SecurityFeature.this, secmsg))
						{
							if(SUtil.DEBUG)
								System.out.println(gpid+
										" finished handshake: " + secmsg.getSender() +
										" trusted:" + state.getCryptoSuite().getSecurityInfos().getRoles().contains(SecurityFeature.TRUSTED)+
										" authenticated groups: " + Arrays.toString(state.getCryptoSuite().getSecurityInfos().getGroups().toArray()));
							currentcryptosuites.put(secmsg.getSender(), state.getCryptoSuite());
							//System.out.println("Removing Handshake " + secmsg.getSender().getRoot().toString());
							initializingcryptosuites.remove(secmsg.getSender());
							state.getResultFuture().setResult(state.getCryptoSuite());
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
						state.getResultFuture().setExceptionIfUndone(e);
						//System.out.println("Removing Handshake " + secmsg.getSender().getRoot().toString()+" "+e);
						initializingcryptosuites.remove(secmsg.getSender());
					}
				}
			}
		}
	}
	
	/**
	 *   Helper for flattening the role map.
	 */
	public static final List<Tuple2<String, String>> flattenRoleMap(Map<String, Set<String>> rolemap)
	{
		List<Tuple2<String, String>> ret = new ArrayList<Tuple2<String,String>>();
		
		for (Map.Entry<String, Set<String>> entry : rolemap.entrySet())
		{
			for (String rolename : entry.getValue())
			{
				ret.add(new Tuple2<String, String>(entry.getKey(), rolename));
			}
		}
		
		return ret;
	}
	
	/** Cryptosuite that is in the process of expiring. */
	private record ExpiringCryptoSuite(ICryptoSuite suite, long timeofexpiration) {};
}
