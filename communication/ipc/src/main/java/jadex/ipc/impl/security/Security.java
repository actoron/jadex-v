package jadex.ipc.impl.security;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
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

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import jadex.collection.RwMapWrapper;
import jadex.common.ClassInfo;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.transformation.traverser.SCloner;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.ipc.ISecurity;
import jadex.ipc.impl.IpcStreamHandler;
import jadex.ipc.impl.security.authentication.AbstractAuthenticationSecret;
import jadex.ipc.impl.security.authentication.AbstractX509PemSecret;
import jadex.ipc.impl.security.handshake.BasicSecurityMessage;
import jadex.ipc.impl.security.handshake.InitialHandshakeFinalMessage;
import jadex.ipc.impl.security.handshake.InitialHandshakeMessage;
import jadex.ipc.impl.security.handshake.InitialHandshakeReplyMessage;
import jadex.serialization.ISerializationServices;

/**
 *  Security functionality for active component communication.
 *  Performs authentication and 
 */
public class Security implements ISecurity
{
	/** Default root certificate for global network. */
	//public static final String DEFAULT_GLOBAL_ROOT_CERTIFICATE = "pem:-----BEGIN CERTIFICATE-----|MIICszCCAhWgAwIBAgIVAP5jQirZLKNnSHf1FES8qkWMJyvKMAoGCCqGSM49BAME|MDYxHTAbBgNVBAMMFEphZGV4IEdsb2JhbCBSb290IFgxMRUwEwYDVQQKDAxBY3Rv|cm9uIEdtYkgwHhcNMTgwODAxMDkxNjA5WhcNMjgwNzI5MDkxNjA5WjA2MR0wGwYD|VQQDDBRKYWRleCBHbG9iYWwgUm9vdCBYMTEVMBMGA1UECgwMQWN0b3JvbiBHbWJI|MIGbMBAGByqGSM49AgEGBSuBBAAjA4GGAAQA6K9sA0U88s0/6nLTwZhXwzBesBr/|MpNAqpZtCBe2sD+3sjppYtnug3RUbRFYNZsYPMMHBqOWyo0BR7N5DxeSJ8AB/T/z|zTC9PqjDUcIazUDCf0XsSSx08a3UqBPZ5EzKRtOvf3cx/qCp/0/fND3iKWfrNhng|LxYMS0d/BMlNRE3vQl6jgbwwgbkwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8E|BAMCAoQwSQYDVR0OBEIEQLAcDiIifZpM0BihTvohWfxP5bHk3iHeA/O5vLaTp7o5|Lw+2E2CcyIXfNcMRhQ5lAymDVYBwJjr0ZjgzvXOsJhIwSwYDVR0jBEQwQoBAsBwO|IiJ9mkzQGKFO+iFZ/E/lseTeId4D87m8tpOnujkvD7YTYJzIhd81wxGFDmUDKYNV|gHAmOvRmODO9c6wmEjAKBggqhkjOPQQDBAOBiwAwgYcCQgGYPCBbcI/ai9nAqzuU|1oXIn4KFguj/95xbVm4HBb9wsNrB0K8LtdXsvB4BR2HeRCB0cWqyCKZimBbaJIoD|BTcs2gJBTXfqb/KlKCwrO6KXLOtah5sgASt+QZ3uD6AXBNrBfBjC5nUBWkx/zJd+|sllyYoekCGy/UAvwNIB4aFkTHnQGyS4=|-----END CERTIFICATE-----|";
	
	/** The singleton instance. */
	public static volatile Security security;
	
	/**
	 *  Get the security instance.
	 */
	public static final Security get()
	{
		if(security==null)
		{
			synchronized(Security.class)
			{
				security = new Security();
			}
		}
		return security;
	}
	
	/** 
	 *  Flag whether to grant default authorization
	 *  (allow basic service calls if host name or group is authenticated).
	 */
	protected boolean defaultauthorization = true;
	
	/** Flag whether to refuse unauthenticated connections. */
	protected boolean refuseuntrusted = false;
	
	/** Flag if connection with platforms without authenticated names are allowed. */
	protected boolean allownoauthname = true;
	
	/** Flag if connection with platforms without authenticated networks are allowed. */
	protected boolean allownonetwork = true;
	
	/** Flag whether to use the default Java trust store. */
	protected boolean loadjavatruststore = true;
	
	/** 
	 *  Flag if the security should add a global network
	 *  if no global network is set.
	 */
	//protected boolean addglobalnetwork = true;
	
	/** Flag if the security should create a random default network
	 *  if no network is set.
	 */
	protected boolean createdefaultnetwork = true;
	
	/** Handshake timeout. */
	protected long handshaketimeout = 20000;
	
	/** Handshake timeout scale factor. */
	//protected double handshaketimeoutscale = 2.0;
	
	/** Handshake reset scale factor. */
	//protected double resettimeoutscale = 0.02;
	
	/** 
	 *  Lifetime of session keys, after which the handshake is repeated
	 *  and a new session key is generated.
	 */
	protected long sessionkeylifetime = 10 * 60 * 1000; // 10 minutes
	
	/** Local platform authentication secret. */
	//protected AbstractAuthenticationSecret platformsecret;
	
	/** Remote platform authentication secrets. */
	//protected Map<IComponentIdentifier, AbstractAuthenticationSecret> remoteplatformsecrets = new HashMap<IComponentIdentifier, AbstractAuthenticationSecret>();;
	
	/** Flag whether to allow platforms to be associated with roles (clashes, spoofing problem?). */
//	protected boolean allowplatformroles = false;
	
	/** Available groups. */
//	protected Map<String, AbstractAuthenticationSecret> networks = new HashMap<String, AbstractAuthenticationSecret>();
	protected Map<String, List<AbstractAuthenticationSecret>> groups = new HashMap<>();
	
	/** The platform name certificate if available. */
	protected AbstractX509PemSecret platformnamecertificate;
	
	/** The host names that are trusted and identified by name. */
	protected Set<String> trustedhosts = new HashSet<>();
	
	/** Trusted authorities for certifying platform names. */
	protected Set<X509CertificateHolder> nameauthorities = new HashSet<>();
	
	/** Custom (non-Java default) trusted authorities for certifying platform names. */
	protected Set<X509CertificateHolder> customnameauthorities = new HashSet<>();
	
	/** Available crypto suites. */
	protected Map<String, Class<?>> allowedcryptosuites = new LinkedHashMap<String, Class<?>>();
	
	/** CryptoSuites currently initializing, value=Handshake state. */
	protected Map<GlobalProcessIdentifier, HandshakeState> initializingcryptosuites = new HashMap<>();
	
	/** CryptoSuites currently in use. */
	// TODO: Expiration / configurable LRU required to mitigate DOS attacks.
	protected RwMapWrapper<GlobalProcessIdentifier, ICryptoSuite> currentcryptosuites = new RwMapWrapper<>(new HashMap<>());
	
	/** CryptoSuites that are expiring with expiration time. */
	protected Map<GlobalProcessIdentifier, List<ExpiringCryptoSuite>> expiringcryptosuites = new HashMap<>();
	
	/** Map of entities and associated roles. */
	protected Map<String, Set<String>> roles = new HashMap<String, Set<String>>();
	
	/** Crypto-Suite reset in progress. */
	protected IFuture<Void> cryptoreset; 
	
	/** Last time cleanup duties were performed. */
	protected volatile long lastcleanup;
	
	/** The list of group names (used by all service identifiers). */
	protected Set<String> groupnames;
	
	public void start()
	{
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
//							SUtil.close(jpw);
//							SUtil.close(baos);
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
	
	/**
	 *  Initialization.
	 */
	/*public void start()
	{
		//final Future<Void> ret = new Future<Void>();
		//ret.thenAccept(done -> System.out.println("Sec startup " + (System.currentTimeMillis() - ts)));
		
		//((SerializationServices)SerializationServices.getSerializationServices(agent.getId().getRoot())).setSecurityService(this);
		
		loadSettings().addResultListener(new ExceptionDelegationResultListener<Map<String,Object>, Void>(ret)
		{
			@SuppressWarnings("unchecked")
			public void customResultAvailable(Map<String, Object> settings)
			{
				boolean savesettings = false;
				Map<String, Object> args = agent.getFeature(IArgumentsResultsFeature.class).getArguments();
				for (Object val : args.values())
					savesettings |= val != null;
				
				usesecret = getProperty("usesecret", args, settings, usesecret);
				printsecret = getProperty("printsecret", args, settings, usesecret);
				refuseuntrusted = getProperty("refuseuntrusted", args, settings, refuseuntrusted);
				
				if (args.get("platformnamecertificate") != null)
					platformnamecertificate = (AbstractX509PemSecret) AbstractAuthenticationSecret.fromString((String) args.get("platformnamecertificate"), true);
				else
					platformnamecertificate = getProperty("platformnamecertificate", args, settings, platformnamecertificate);
				
				if (args.get("nameauthorities") != null)
				{
					nameauthorities = new HashSet<>();
					String authstr = (String) args.get("nameauthorities");
					String[] split = authstr.split(",");
					for (int i = 0; i < split.length; ++i)
					{
						if (split[i].length() > 0)
						{
							try
							{
								X509CertificateHolder cert = SSecurity.readCertificateFromPEM(split[i]);
								nameauthorities.add(cert);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					}
				}
				else
				{
					nameauthorities = getProperty("nameauthorities", args, settings, nameauthorities);
				}
				
				customnameauthorities.addAll(nameauthorities);
				
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
//									SUtil.close(jpw);
//									SUtil.close(baos);
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
//							ts = System.currentTimeMillis() - ts;
//							System.out.println("READING TOOK " + ts);
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
				
				if (args.get("trustedplatforms") != null)
				{
					String authstr = (String) args.get("trustedplatforms");
					String[] split = authstr.split(",");
					for (int i = 0; i < split.length; ++i)
					{
						if (split[i].length() > 0)
						{
							trustedplatforms.add(split[i]);
						}
					}
				}
				else
				{
					trustedplatforms = getProperty("trustedplatforms", args, settings, trustedplatforms);
				}
				
				if (args.get("platformsecret") != null)
					platformsecret = AbstractAuthenticationSecret.fromString((String) args.get("platformsecret"), false);
				else
					platformsecret = getProperty("platformsecret", args, settings, platformsecret);
				
				String[] nn = (String[]) args.remove("networknames");
				String[] ns = (String[]) args.remove("networksecrets");
				if (args.get("networknames") != null || args.get("networksecrets") != null)
				{
					
					if (nn == null || ns == null || ns.length != nn.length)
					{
						agent.getLogger().warning("Network names and secrets do not match, ignoring...");
						nn = null;
						ns = null;
					}
				}
				if (nn != null)
				{
					for (int i = 0; i < nn.length; ++i)
						networks.add(nn[i], AbstractAuthenticationSecret.fromString(ns[i]));
				}
				else
				{
					networks = getProperty("networks", args, settings, networks);
				}
				
				File networksfile = new File("networks.cfg");
				if (networksfile.exists())
				{
					InputStream is = null;
					try
					{
						is = new FileInputStream(networksfile);
						is = new BufferedInputStream(is);
						java.util.Properties nwfileprops = new java.util.Properties();
						nwfileprops.load(is);
						SUtil.close(is);
						is = null;
						
						for (String propname : SUtil.notNull(nwfileprops.stringPropertyNames()))
						{
							String secretstr = nwfileprops.getProperty(propname);
							try
							{
								AbstractAuthenticationSecret secret = AbstractAuthenticationSecret.fromString(secretstr, true);
								networks.add(propname, secret);
							}
							catch (Exception e)
							{
							}
						}
					}
					catch (Exception e)
					{
					}
					finally
					{
						SUtil.close(is);
					}
				}
				
				if(addglobalnetwork && !networks.containsKey(GLOBAL_NETWORK_NAME))
					networks.add(GLOBAL_NETWORK_NAME, AbstractAuthenticationSecret.fromString(DEFAULT_GLOBAL_ROOT_CERTIFICATE, true));
				
				if((networks.isEmpty() || (networks.size() == 1 
					&& networks.containsKey(GLOBAL_NETWORK_NAME))) && createdefaultnetwork)
				{
					networks.add(SUtil.createPlainRandomId("default_network", 6), KeySecret.createRandom());
					savesettings = true;
				}
				
				remoteplatformsecrets = getProperty("remoteplatformsecrets", args, settings, remoteplatformsecrets);
				roles = getProperty("roles", args, settings, roles);
				
				if(printsecret)
				{
					for(Map.Entry<String, Collection<AbstractAuthenticationSecret>> entry : networks.entrySet())
					{
						if(entry.getValue() != null && !GLOBAL_NETWORK_NAME.equals(entry.getKey()))
						{
							for(AbstractAuthenticationSecret secret : entry.getValue())
								System.out.println("Available network '" + entry.getKey() + "' with secret " + secret);
						}
					}
				}
				
				if (usesecret && platformsecret == null)
				{
					platformsecret = KeySecret.createRandom();
					savesettings = true;
//					System.out.println("Generated new platform access key: "+platformsecret.toString().substring(KeySecret.PREFIX.length() + 1));
				}
				
				if (printsecret && platformsecret != null)
				{
					String secretstr = platformsecret.toString();
					String pfname = agent.getId().getPlatformName();
					
					if (platformsecret instanceof PasswordSecret)
						System.out.println("Platform " + pfname + " access password: "+secretstr);
					else if (platformsecret instanceof KeySecret)
						System.out.println("Platform " + pfname + " access key: "+secretstr);
					else if (platformsecret instanceof AbstractX509PemSecret)
						System.out.println("Platform " + pfname + " access certificates: "+secretstr);
					else
						System.out.println("Platform " + pfname + " access secret: "+secretstr);
				}
				
				networknames = (Set<String>)Starter.getPlatformValue(agent.getId(), Starter.DATA_NETWORKNAMESCACHE);
//				networknames.addAll(networks.keySet());
				// Only add network names the platform is a member of (secret can sign).
				for (Map.Entry<String, Collection<AbstractAuthenticationSecret>> entry : networks.entrySet())
				{
					for (AbstractAuthenticationSecret secret : entry.getValue())
					{
						if (secret.canSign())
						{
							networknames.add(entry.getKey());
							break;
						}
					}
				}
				// Update networknames in indexer
				ServiceRegistry.getRegistry(agent.getId().getRoot()).updateService(null);
				
				// TODO: Make configurable
				String[] cryptsuites = new String[] { NHCurve448ChaCha20Poly1305Suite.class.getCanonicalName() };
				allowedcryptosuites = new LinkedHashMap<String, Class<?>>();
				for (String cryptsuite : cryptsuites)
				{
					try
					{
						Class<?> clazz = Class.forName(cryptsuite, true, agent.getClassLoader());
						allowedcryptosuites.put(cryptsuite, clazz);
					}
					catch (Exception e)
					{
						ret.setException(e);
						return;
					}
				}
				
				if (savesettings)
					saveSettings();
				
				IMessageFeature msgfeat = agent.getFeature(IMessageFeature.class);
				msgfeat.addMessageHandler(new SecurityMessageHandler());
				msgfeat.addMessageHandler(new ReencryptRequestHandler());
				
				ret.setResult(null);
			}
		});
		
		ret.addResultListener(new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
				// Warn about weak passwords.
				Map<PasswordSecret, String> pwsecrets = new HashMap<>();
				if (platformsecret instanceof PasswordSecret)
					pwsecrets.put((PasswordSecret) platformsecret, "local platform");
				
				for (Map.Entry<IComponentIdentifier, AbstractAuthenticationSecret> entry : remoteplatformsecrets.entrySet())
				{
					if (entry.getValue() instanceof PasswordSecret)
						pwsecrets.put((PasswordSecret) entry.getValue(), "for remote platform '" + entry.getKey().toString() + "'");
				}

				for (Map.Entry<String, Collection<AbstractAuthenticationSecret>> nwentry : networks.entrySet())
				{
					for (AbstractAuthenticationSecret secret : nwentry.getValue())
					{
						if (secret instanceof PasswordSecret)
							pwsecrets.put((PasswordSecret) secret, "network '" + nwentry.getKey() + "'");
					}
				}
				
				for (Map.Entry<PasswordSecret, String> entry : pwsecrets.entrySet())
				{
//					System.out.println("CHECKING " + secret + " " + secret.isWeak());
					if (entry.getKey().isWeak())
						agent.getLogger().severe(agent.getId().getName() + ": Weak password detected for " + entry.getValue() + ", password '" + entry.getKey().getPassword() + "' is too short, please use at least " + PasswordSecret.MIN_GOOD_PASSWORD_LENGTH + " random characters.");
				}
				
				// Reindex services since networks are now available.
				ServiceRegistry.getRegistry(agent.getId().getRoot()).updateService(null);
			}
			
			public void exceptionOccurred(Exception exception)
			{
			}
		});
		
		return ret;
	}*/
	
	//---- ISecurityService methods. ----
	
	/**
	 *  Encrypts and signs the message for a receiver.
	 *  
	 *  @param receiver The receiver.
	 *  @param content The content
	 *  @return Encrypted/signed message.
	 */
	public byte[] encryptAndSign(GlobalProcessIdentifier receiver, byte[] message)
	{
		checkCleanup();
		
		ICryptoSuite cs = currentcryptosuites.get(receiver);
		if(cs != null && !cs.isExpiring())
			return cs.encryptAndSign(message);
		
		byte[] ret = null;
		synchronized(this)
		{
			doCleanup();
			
			//String rplat = ((IComponentIdentifier) header.getProperty(IMsgHeader.RECEIVER)).getRoot().toString();
			
			cs = currentcryptosuites.get(receiver);
			
			if (cs != null && cs.isExpiring())
			{
				if(cs.equals(currentcryptosuites.get(receiver)))
				{
					if(SUtil.DEBUG)
						System.out.println("Expiring: "+receiver);
					expireCryptosuite(receiver);
					cs = null;
				}
			}
			
			if (cs != null)
			{
				ret = cs.encryptAndSign(message);
			}
			else
			{
				HandshakeState hstate = initializingcryptosuites.get(receiver);
				if(hstate == null)
				{
					if (SUtil.DEBUG)
						System.out.println("Handshake state null, starting new handhake: "+GlobalProcessIdentifier.SELF+" "+receiver);
					//System.out.println(initializingcryptosuites+" "+System.identityHashCode(initializingcryptosuites));
					initializeHandshake(receiver);
					hstate = initializingcryptosuites.get(receiver);
				}
				
				cs = hstate.getResultFuture().get();
				if (cs != null)
				{
					ret = cs.encryptAndSign(message);
				}
				else
				{
					throw new UncheckedIOException(new IOException("Communication with " + receiver + " failed."));
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Decrypt and authenticates the message from a sender.
	 *  
	 *  @param sender The sender.
	 *  @param content The content.
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
			
			if (cleartext == null)
			{
				HandshakeState hstate = initializingcryptosuites.get(sender);
				if (hstate != null)
				{
					//final byte[] fmessage = message;
					hstate.getResultFuture().addResultListener(new IResultListener<ICryptoSuite>()
					{
						public void resultAvailable(ICryptoSuite result)
						{
							byte[] cleartext = result.decryptAndAuth(message);
							if(cleartext != null)
							{
								ret.setResult(new DecodedMessage(result.getSecurityInfos(), cleartext));
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
						}
						
						public void exceptionOccurred(Exception exception)
						{
							ret.setException(exception);
						}
					});
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
	 *  Sets a new group.
	 * 
	 *  @param groupname The group name.
	 *  @param secret The secret.
	 */
	public void setGroup(String groupname, String secret)
	{
		if(groupname==null || groupname.length()==0)
			throw new IllegalArgumentException("Networkname is null.");
		if(secret==null || secret.length()==0)
			throw new IllegalArgumentException("Secret is null.");
		
		synchronized(this)
		{
			AbstractAuthenticationSecret asecret = AbstractAuthenticationSecret.fromString(secret);
			
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
	 *  Get access to the stored virtual network configurations.
	 * 
	 *  @return The stored virtual network configurations.
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
	 *  Gets the current groups and secrets. 
	 *  
	 *  @return The current groups and secrets.
	 */
	/*public Map<String, String> getAllKnownGroups()
	{
		Map<String, String> ret = new HashMap<>();
		
		synchronized(this)
		{
			for(Map.Entry<String, Collection<AbstractAuthenticationSecret>> entry : groups.entrySet())
			{
				for(AbstractAuthenticationSecret secret : entry.getValue())
					ret.add(entry.getKey(), secret.toString());
			}
		}
		return ret;
	}*/
	
	/** 
	 *  Adds an authority for authenticating platform names.
	 *  
	 *  @param pemcertificate The pem-encoded certificate.
	 */
	public void addNameAuthority(String pemcertificate)
	{
//		final AbstractAuthenticationSecret asecret = AbstractAuthenticationSecret.fromString(secret);
//		if (!(asecret instanceof AbstractX509PemSecret))
//			return new Future<>(new IllegalArgumentException("Only X509 secrets allowed as name authorities"));
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
	 *  @param secret The secret, only X.509 secrets allowed.
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
	 *  Adds an authority for authenticating platform names.
	 *  
	 *  @param secret The secret, only X.509 secrets allowed.
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
	 *  Gets all authorities not defined in the Java trust store for authenticating platform names.
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
	 *  Gets the current network names. 
	 *  @return The current networks names.
	 */
	/*@Excluded
	public Set<String> getNetworkNamesSync()
	{
		@SuppressWarnings("unchecked")
		Set<String> ret = Collections.EMPTY_SET;
		
		if(networknames!=null)
		{
			String[] nnames = networknames.toArray(new String[0]);
			ret = SUtil.arrayToSet(nnames);
		}	
		
		return ret;
	}*/
	
	/**
	 *  Gets the secret of a platform if available.
	 * 
	 *  @param cid ID of the platform.
	 *  @return Encoded secret or null.
	 */
	/*public IFuture<String> getPlatformSecret(final IComponentIdentifier cid)
	{
		return agent.scheduleStep(new IComponentStep<String>()
		{
			public IFuture<String> execute(IInternalAccess ia)
			{
				AbstractAuthenticationSecret secret = null;
				if(cid == null)
					secret = getInternalPlatformSecret();
				else
					secret = getInternalPlatformSecret(cid);
				return new Future<String>(secret != null ? secret.toString() : null);
			}
		});
	}*/
	
	/**
	 *  Sets the secret of a platform.
	 * 
	 *  @param cid ID of the platform.
	 *  @return Encoded secret or null.
	 */
	/*public IFuture<Void> setPlatformSecret(final IComponentIdentifier cid, final String secret)
	{
		return agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				// TODO: Refresh?
				if (secret == null)
				{
					if (cid == null || agent.getId().getRoot().equals(cid))
						platformsecret = null;
					else
						remoteplatformsecrets.remove(cid);
				}
				else
				{
					AbstractAuthenticationSecret authsec = AbstractAuthenticationSecret.fromString(secret);
					
					if (cid == null || agent.getId().getRoot().equals(cid))
						platformsecret = authsec;
					else
						remoteplatformsecrets.put(cid, authsec);
				}
				
				saveSettings();
				
				if (usesecret)
					return resetCryptoSuites();
				else
					return IFuture.DONE;
			}
		});
	}*/
	
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
	 *  Gets a copy of the current role map.
	 *  
	 *  @return Copy of the role map.
	 */
	public Map<String, Set<String>> getRoleMap()
	{
		synchronized(this)
		{
			return (Map<String, Set<String>>) SCloner.clone(roles);
		}
	}
	
	//---- Internal direct access methods. ----
	
	/**
	 *  Gets the local platform secret.
	 */
//	public AbstractAuthenticationSecret getInternalPlatformSecret()
//	{
//		return platformsecret;
//	}
	
	/**
	 *  Gets the secret of a platform if available.
	 * 
	 *  @param cid ID of the platform.
	 *  @return Secret or null.
	 */
//	public AbstractAuthenticationSecret getInternalPlatformSecret(IComponentIdentifier cid)
//	{
//		cid = cid.getRoot();
//		if (cid.equals(agent.getId().getRoot()))
//			return getInternalPlatformSecret();
//		return remoteplatformsecrets.get(cid.getRoot());
//	}
	
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
	public AbstractX509PemSecret getInternalPlatformNameCertificate()
	{
		return platformnamecertificate;
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
	 *  Checks whether to allow connections without network authentication.
	 *  
	 *  @return True, if used.
	 */
	public boolean getInternalAllowNoNetwork()
	{
		return allownonetwork;
	}
	
	/**
	 *  Checks whether to allow the default authorization.
	 *  
	 *  @return True, if used.
	 */
	public boolean getInternalDefaultAuthorization()
	{
		return defaultauthorization;
	}
	
	/**
	 *  Sets the roles of a security info object.
	 *  @param secinf Security info.
	 *  @param defroles Default roles that should be added.
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
			for (String network : secinf.getGroups())
			{
				Set<String> r = roles.get(network);
				if (r != null)
					siroles.addAll(r);
				else
					siroles.add(network);
			}
		}
		
		// TODO: Admin role is automatically trusted.
		//if (siroles.contains(Security.ADMIN))
		//	siroles.add(Security.TRUSTED);
		
		secinf.setMappedRoles(siroles);
	}
	
	/**
	 *  Checks receiver authorization and, if so, encrypts the message. Otherwise, an exception is issued.
	 *  
	 *  @param receiver Receive ID. 
	 *  @param content Message content.
	 *  @param cs The cryptosuite negotiated with receiver.
	 *  @param resultfuture Optional result future if it already exist, if null a future is created.
	 *  @return Result future containing encrypted message or exception.
	 */
	protected byte[] checkReceiverAndEncrypt(GlobalProcessIdentifier receiver, byte[] content, ICryptoSuite cs) //, Future<byte[]> resultfuture)
	{
		//TODO: Implement receiver authorization here.
		return cs.encryptAndSign(content);
		
		//Future<byte[]> ret = resultfuture != null ? resultfuture : new Future<>();
		//ISecurityInfo recinfo = cs.getSecurityInfos();
		//if (isReceiverAuthorized(header, cs.getSecurityInfos()))
			//ret.setResultIfUndone(cs.encryptAndSign(content));
		/*else
		{
			String rplat = ((IComponentIdentifier) header.getProperty(IMsgHeader.RECEIVER)).getRoot().toString();
			ret.setException(new SecurityException("Receiving platform " + rplat + " not authorized to receive message."));
		}*/
		//return ret;
	}
	
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
		
		// Check for expired suites.
		// This is a two-step process because suites have a long lifespan after handshake,
		// i.e. typically there are no expired suites. In order to optimize locking, we
		// first check whether it is even worth to acquire a write lock by checking with a read lock.
		boolean hasexpiredsuites = false;
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
			currentcryptosuites.getWriteLock().lock();
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
				
				//if(debug)
					System.out.println("Cryptosuites reset.");
			}
		}
	}
	
	/**
	 *  Creates a crypto suite of a particular name.
	 * 
	 *  @param name Name of the suite.
	 *  @param convid Conversation ID of handshake.
	 *  @param remoteversion The remote Jadex version.
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
				ret = (ICryptoSuite) clazz.getConstructor().newInstance();
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
	 *  @param gpid The global process id.
	 */
	protected void initializeHandshake(GlobalProcessIdentifier gpid)
	{
		String convid = SUtil.createUniqueId(gpid.toString());
		HandshakeState hstate = new HandshakeState();
		hstate.setExpirationTime(System.currentTimeMillis() + handshaketimeout);
		hstate.setConversationId(convid);
		hstate.setResultFuture(new Future<ICryptoSuite>());
		if(SUtil.DEBUG)
			System.out.println("Security.initializeHandshake0 " +gpid+" "+convid+" "+handshaketimeout);
		
		initializingcryptosuites.put(gpid, hstate);
		
		String[] csuites = allowedcryptosuites.keySet().toArray(new String[allowedcryptosuites.size()]);
		InitialHandshakeMessage ihm = new InitialHandshakeMessage(GlobalProcessIdentifier.SELF, convid, csuites);
		if(SUtil.DEBUG)
			System.out.println("Security.initializeHandshake1 " + convid + " " + GlobalProcessIdentifier.SELF + " -> " + gpid + " Phase: 0 Step: 0 "+initializingcryptosuites+" "+System.identityHashCode(initializingcryptosuites));
		sendSecurityHandshakeMessage(gpid, ihm);
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
		ISerializationServices.get().encode(baos, this.getClass().getClassLoader(), message);
		IpcStreamHandler.get().sendMessage(secid, ByteBuffer.wrap(baos.toByteArray()));
		//return agent.getFeature(IMessageFeature.class).sendMessage(message, addheader, receiver);
	}
	
	/**
	 *  Request reencryption by source.
	 *  
	 *  @param source Source of the content.
	 *  @param content The encrypted content.
	 *  @return Reply of decryption request, may be exception.
	 */
	/*protected IFuture<byte[]> requestReencryption(GlobalProcessIdentifier source, byte[] content)
	{
		if(SUtil.DEBUG)
			System.out.println("reencryption: "+source+" "+Arrays.hashCode(content) + " " + currentcryptosuites.get(source));
		expireCryptosuite(source);
		
//		Thread.dumpStack();
		
		ReencryptionRequest req = new ReencryptionRequest();
		req.setContent(content);
		
		Future<byte[]> ret = new Future<>();
		agent.getFeature(IMessageFeature.class).sendMessageAndWait(source, req)
			.addResultListener(agent.getFeature(IExecutionFeature.class).createResultListener(new IResultListener<Object>()
		{
			public void resultAvailable(Object result)
			{
				if (result instanceof byte[])
					ret.setResult((byte[]) result);
				else if (result instanceof Exception)
					exceptionOccurred((Exception) result);
				else
					ret.setException(new IllegalArgumentException("Received unknown reply: " + result));
			}
			
			public void exceptionOccurred(Exception exception)
			{
				ret.setException(exception);
			}
		}));
		
		return ret;
	}*/
	
	//-------- Message Handling -------
	
	/**
	 *  Handle security handshake message.
	 *  @param sender The sender.
	 *  @param msg The message.
	 */
	public void handleMessage(byte[] message)
	{
		Object msg = ISerializationServices.get().decode(new ByteArrayInputStream(message), getClass().getClassLoader());
		if (msg instanceof InitialHandshakeMessage)
		{
			final InitialHandshakeMessage imsg = (InitialHandshakeMessage) msg;
			GlobalProcessIdentifier sender = imsg.getSender();
			
			if(SUtil.DEBUG)
				System.out.println("handleMessage: initial handshake message: "+GlobalProcessIdentifier.SELF+" "+sender+" "+msg);
			
			final Future<ICryptoSuite> fut = new Future<ICryptoSuite>();
			
			HandshakeState state = initializingcryptosuites.get(sender.toString());
			
			// Check if handshake is already happening. 
			if(state != null)
			{
				// Check if duplicate
				if(!state.getConversationId().equals(imsg.getConversationId()))
				{
					if(GlobalProcessIdentifier.SELF.toString().compareTo(sender.toString()) < 0)
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
			
			ICryptoSuite oldcs = currentcryptosuites.get(sender.toString());
			if (oldcs != null)
			{
				try (IAutoLock l = currentcryptosuites.writeLock())
				{
					if (oldcs.equals(currentcryptosuites.get(sender.toString())))
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
			
			InitialHandshakeReplyMessage reply = new InitialHandshakeReplyMessage(GlobalProcessIdentifier.SELF, state.getConversationId(), chosensuite);
			
			if(SUtil.DEBUG)
				System.out.println("Security Handshake " + imsg.getConversationId() + " " + GlobalProcessIdentifier.SELF + " -> " + sender + " Phase: 0 Step: 1");
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
						InitialHandshakeFinalMessage fm = new InitialHandshakeFinalMessage(GlobalProcessIdentifier.SELF, rm.getConversationId(), rm.getChosenCryptoSuite());
						if(SUtil.DEBUG)
							System.out.println("Security Handshake " + convid + " " + GlobalProcessIdentifier.SELF + " -> " + rm.getSender() + " Phase: 0 Step: 2, finished Phase 0, entering Phase 1");
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
					System.out.println("Security Handshake " + convid + " " + GlobalProcessIdentifier.SELF + " -> " + fm.getSender() + " finished Phase 0, entering Phase 1");
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
						if (!suite.handleHandshake(Security.this, fm))
						{
							if(SUtil.DEBUG)
								System.out.println(GlobalProcessIdentifier.SELF+" finished handshake: " + fm.getSender());
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
							System.out.println("Security Handshake " + convid + " " + GlobalProcessIdentifier.SELF + " -> " + secmsg.getSender() + " processing Phase 1 step");
						if (!state.getCryptoSuite().handleHandshake(Security.this, secmsg))
						{
							if(SUtil.DEBUG)
								System.out.println(GlobalProcessIdentifier.SELF+
										" finished handshake: " + secmsg.getSender() +
										" trusted:" + state.getCryptoSuite().getSecurityInfos().getRoles().contains(Security.TRUSTED)+
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
						state.getResultFuture().setException(e);
						//System.out.println("Removing Handshake " + secmsg.getSender().getRoot().toString()+" "+e);
						initializingcryptosuites.remove(secmsg.getSender());
					}
				}
			}
		}
	}
	
	/*public void handleReencryptionMessage(GlobalProcessIdentifier sender, ISecurityInfo secinfos, ReencryptionRequest req)
	{
		//String senderpf = ((IComponentIdentifier) header.getProperty(IMsgHeader.SENDER)).getRoot().toString();
		
		Object ret = null;
		
		List<ExpiringCryptoSuite> expsuites = expiringcryptosuites.get(sender);
		if (expsuites != null && expsuites.size() > 0)
		{
//			String checkresults = "";
			for (ExpiringCryptoSuite expsuite : expsuites)
			{
				ISecurityInfo suiteinfos = expsuite.suite().getSecurityInfos();
				
//				checkresults += ""+secinfos.isAdminPlatform()+" "+suiteinfos.isAdminPlatform()+" "+secinfos.isAdminPlatform()
//				+" msgtrust:"+secinfos.isTrustedPlatform()+" suitetrust:"+suiteinfos.isTrustedPlatform()+" "+secinfos.isTrustedPlatform()
//				+" "+secinfos.getAuthenticatedPlatformName()+" "+suiteinfos.getAuthenticatedPlatformName()
//				+" "+suiteinfos.getAuthenticatedPlatformName()+" "+secinfos.getAuthenticatedPlatformName()
//				+" "+Arrays.toString(secinfos.getNetworks().toArray())
//				+" "+Arrays.toString(suiteinfos.getNetworks().toArray())+"\n";
				
				// Re-encryption must be carefully checked for unchanged privileges to avoid spoofing attacks:
				// e.g. Privileged platform A makes privileged request for user passwords, then shuts down.
				// Platform B spoofs name of platform A and thus intercepts response, then requests re-encryption
				// with its own handshake with the original platform. To prevent this, the current handshake privileges
				// must be compared to the original ones to ensure that they are identical.
				if (SUtil.equals(secinfos.getRoles(), suiteinfos.getRoles()))
				{
					Set<String> msgnets = secinfos.getGroups();
					if (msgnets.containsAll(suiteinfos.getGroups()))
					{
						ret = expsuite.suite().decryptAndAuthLocal(req.getContent());
						if (ret != null)
							break;
					}
				}
			}
			if (ret == null)
			{
				ret = new SecurityException("Found expired suites but none match required security criteria.");
//				ret = new SecurityException("Found " + expsuites.size() + " expired suites but none match required security criteria:\n" + checkresults);
			}
		}
		else
		{
			ret = new IllegalStateException("No expired suites found to decrypt message.");
		}
		
		agent.getFeature(IMessageFeature.class).sendReply(header, ret);
	}*/
	
	/**
	 *  Handler dealing with remote reencryption requests.
	 *
	 */
	/*protected class ReencryptRequestHandler implements IUntrustedMessageHandler
	{
		public boolean isHandling(ISecurityInfo secinfos, IMsgHeader header, Object msg)
		{
			return msg instanceof ReencryptionRequest;
		}
		
		public boolean isRemove()
		{
			return false;
		}
		
		
	}*/
	
	//---- IInternalService stuff 
	
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
	
	/**
	 *  Get infos about name authorities.
	 *  Format is [{subjectid,dn,custom},...]
	 *  @return Infos about the name authorities.
	 */
	/*public IFuture<String[][]> getNameAuthoritiesInfo()
	{
		final Set<String> nas = getNameAuthorities().get();
		final Set<String> custom = getCustomNameAuthorities().get();
		Map<String, String> nacerts = new HashMap<>();
		
		String[][] ret = null;
		if(nas != null && nas.size() > 0)
		{
			ret = new String[nas.size()][3];
			
			int i = 0;
			for(String cert : nas)
			{
				String subjectid = null;
				String dn = null;
				InputStream is = null;
				try
				{
					subjectid = SSecurity.getCommonName(SSecurity.readCertificateFromPEM(cert).getSubject());
					dn = SSecurity.readCertificateFromPEM(cert).getSubject().toString();
				}
				catch (Exception e)
				{
				}
				finally
				{
					SUtil.close(is);
				}
				
				nacerts.put(dn, cert);
				ret[i][0] = subjectid != null? subjectid : "";
				ret[i][1] = dn != null ? dn : "";
				ret[i][2] = custom.contains(cert) ? "Custom CA" : "Java CA";
				++i;
			}
		}
		else
		{
			ret = new String[0][0];
		}
		
		return new Future<String[][]>(ret);
	}*/
	
	/**
	 *  Invoke a method reflectively.
	 *  @param methodname The method name.
	 *  @param argtypes The argument types (can be null if method exists only once).
	 *  @param args The arguments.
	 *  @return The result.
	 */
	public IFuture<Object> invokeMethod(String methodname, ClassInfo[] argtypes, Object[] args, ClassInfo rettype)
	{
		return new Future<Object>(new UnsupportedOperationException());
	}
	
	/**
	 *  Get reflective info about the service methods, args, return types.
	 *  @return The method infos.
	 */
	/*public IFuture<MethodInfo[]> getMethodInfos()
	{
		Class<?> iface = sid.getServiceType().getType(agent.getClassLoader());
		
		Set<Method> ms = new HashSet<>();
		
		Set<Class<?>> todo = new HashSet<>();
		todo.add(iface);
		todo.add(IService.class);
		while(todo.size()>0)
		{
			Class<?> cur = todo.iterator().next();
			todo.remove(cur);
			ms.addAll(SUtil.arrayToList(cur.getMethods()));
			
			cur = cur.getSuperclass();
			while(cur!=null && cur.getAnnotation(Service.class)==null)
				cur = cur.getSuperclass();
			
			if(cur!=null)
				todo.add(cur);
		}
		
		MethodInfo[] ret = new MethodInfo[ms.size()];
		Iterator<Method> it = ms.iterator();
		for(int i=0; i<ms.size(); i++)
		{
			MethodInfo mi = new MethodInfo(it.next());
			ret[i] = mi;
		}
		
		return new Future<MethodInfo[]>(ret);
	}*/
	
	/**
	 *  Check the platform password.
	 *  @param secret The platform secret.
	 *  @return True, if platform password is correct.
	 */
	public IFuture<Boolean> checkPlatformPassword(String secret)
	{
		boolean ret = false;
		
		AbstractAuthenticationSecret sec = AbstractAuthenticationSecret.fromString(secret);
		
		/*if(platformsecret!=null)
		{
			if(platformsecret instanceof PasswordSecret && sec instanceof PasswordSecret 
				|| platformsecret instanceof KeySecret && sec instanceof KeySecret)
			{
				ret = platformsecret.equals(sec);
			}
			else if(platformsecret instanceof KeySecret && sec instanceof PasswordSecret)
			{
				PasswordSecret ps = (PasswordSecret)sec;
				byte[] kd = SSecurity.deriveKeyFromPassword(ps.getPassword(), null);
				ret = SUtil.arrayEquals(((KeySecret)platformsecret).getKey(), kd);
			}
		}*/
		
		return new Future<Boolean>(ret);
	}
	
	/** Cryptosuite that is in the process of expiring. */
	private record ExpiringCryptoSuite(ICryptoSuite suite, long timeofexpiration) {};
}
