package jadex.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jadex.common.SUtil;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.TransformProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.serialization.serializers.JadexBinarySerializer;
import jadex.serialization.serializers.JadexJsonSerializer;

/**
 *  todo: make one service/helper for all components
 * 
 *  Functionality for managing serialization.
 */
public class SerializationServices implements ISerializationServices
{
	//-------- attributes --------
		
	/** The remote reference module */
	//protected RemoteReferenceModule rrm;
	
	/** Default serializer. */
	protected int defaultserializer;
	
	/** All available serializers */
	protected ISerializer[] serializers;
	
	/** Preprocessors for encoding. */
	protected ITraverseProcessor[] preprocessors;
	
	/** Postprocessors for decoding. */
	protected ITraverseProcessor[] postprocessors;
	
	/** Singleton instance. */
	protected static volatile SerializationServices instance;
	
	/**
	 *  Gets a singleton instance.
	 *  @return Instance.
	 */
	public static final SerializationServices get()
	{
		if (instance == null)
		{
			synchronized (SerializationServices.class)
			{
				if (instance == null)
				{
					instance = new SerializationServices();
				}
			}
		}
		return instance;
	}

	/** Creates the management. */
	private SerializationServices()
	{
		serializers = new ISerializer[3];
		
		ISerializer serial = new JadexBinarySerializer();
		serializers[serial.getSerializerId()] = serial;
		defaultserializer = serial.getSerializerId();
		
		serial = new JadexJsonSerializer();
		serializers[serial.getSerializerId()] = serial;
		
		List<ITraverseProcessor> procs = createPreprocessors();
		preprocessors = procs.toArray(new ITraverseProcessor[procs.size()]);
		procs = createPostprocessors();
		postprocessors = procs.toArray(new ITraverseProcessor[procs.size()]);
	}
	
	/**
	 *  Encodes/serializes an object for a particular receiver.
	 *  
	 *  @param os OutputStream to write the object.
	 *  @param cl The classloader used for encoding.
	 *  @param obj Object to be encoded.
	 */
	public void encode(OutputStream os, ClassLoader cl, Object obj)
	{
		try
		{
			os.write(SUtil.intToBytes(defaultserializer));
		}
		catch (IOException e)
		{
			SUtil.throwUnchecked(e);
		}
		serializers[defaultserializer].encode(os, cl, null, preprocessors, obj);
	}
	
	/**
	 *  Decodes/deserializes an object.
	 *  
	 *  @param is InputStream to read.
	 *  @param cl The classloader used for decoding.
	 *  @param header The header object if available, null otherwise.
	 *  @return Object to be encoded.
	 *  
	 */
	public Object decode(InputStream is, ClassLoader cl)
	{
		byte[] intbuf = new byte[4];
		SUtil.readStream(intbuf, is);
		ISerializer ser = serializers[SUtil.bytesToInt(intbuf)];
		
		return ser.decode(is, cl, postprocessors, null, ser);
	}
	
	/**
	 *  Returns all serializers.
	 *  
	 *  @param platform Sending platform.
	 *  @return Serializers.
	 */
	public ISerializer[] getSerializers()
	{
		return serializers;
	}
	
	/**
	 *  Returns a serializer per id.
	 *  @param id The id.
	 *  @return The serializer.
	 */
	public ISerializer getSerializer(int id)
	{
		return id<serializers.length? serializers[id]: null;
	}
	
	/**
	 *  Gets the post-processors for decoding a received message.
	 */
	public ITraverseProcessor[] getPostprocessors()
	{
		return postprocessors;
	}
	
	/**
	 *  Gets the pre-processors for encoding a received message.
	 */
	public ITraverseProcessor[] getPreprocessors()
	{
		return preprocessors;
	}
	
	
	
	
	/**
	 *  Create the preprocessors.
	 */
	public List<ITraverseProcessor> createPostprocessors()
	{
		// Equivalent pre- and postprocessors for binary mode.
		List<ITraverseProcessor> procs = new ArrayList<ITraverseProcessor>();
		
		return procs;
	}
	
	/**
	 * 
	 */
	public List<ITraverseProcessor> createPreprocessors()
	{
		List<ITraverseProcessor> procs = new ArrayList<ITraverseProcessor>();
		
		// Preprocessor to copy the networknames cache object (used by security service and all service ids)
		procs.add(new TransformProcessor());
		
		
		return procs;
	}
	
	/**
	 *  Test if an object has reference semantics. It is a reference when:
	 *  - it implements IRemotable
	 *  - it is an IService, IExternalAccess or IFuture
	 *  - if the object has used an @Reference annotation at type level
	 *  - has been explicitly set to be reference
	 */
	public boolean isLocalReference(Object object)
	{
		return true;
		//return rrm.isLocalReference(object);
	}
	
	/**
	 *  Test if an object is a remote object.
	 */
	public boolean isRemoteObject(Object target)
	{
		return false; //rrm.isRemoteObject(target);
	}
	
	/**
	 *  Get the clone processors.
	 *  @return The clone processors.
	 * /
	public List<ITraverseProcessor> getCloneProcessors()
	{
		return rrm.getCloneProcessors();
	}*/
	
	/**
	 *  Get the clone processors.
	 *  @return The clone processors.
	 */
	public List<ITraverseProcessor> getCloneProcessors()
	{
		return Collections.synchronizedList(Traverser.getDefaultProcessors());
	}
	
	/**
	 *  Injects the security service.
	 *  
	 *  @param secserv The security service.
	 * /
	public void setSecurityService(ISecurityService secserv)
	{
		this.secserv = secserv;
	}*/
	
	/**
	 *  Gets the serialization services.
	 * 
	 *  @param platform The platform ID.
	 *  @return The serialization services.
	 * /
	public static final ISerializationServices getSerializationServices(UUID platform)
	{
		return (ISerializationServices)Starter.getPlatformValue(platform, Starter.DATA_SERIALIZATIONSERVICES);
	}*/

	/**
	 *  Get the remote reference module.
	 *  @return the remote reference module.
	 * /
	public RemoteReferenceModule getRemoteReferenceModule()
	{
		return rrm;
	}*/
	
	/**
	 *  Get the string converters (can convert to and from string, possibly only for some types).
	 *  @return The converters.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, IStringConverter> getStringConverters()
	{
		return (Map<String, IStringConverter>)Arrays.stream(serializers).filter(new Predicate<ISerializer>() 
		{
			public boolean test(ISerializer t) 
			{
				return t instanceof IStringConverter;
			}
		}).collect((Collector)Collectors.toMap(IStringConverter::getType, Function.identity()));
	}
	
	/**
	 *  Convert object to string.
	 *  @param val The value.
	 *  @return The string value.
	 */
	public String convertObjectToString(Object val, Class<?> type, ClassLoader cl, String mediatype, Object context)
	{
		mediatype = mediatype!=null? mediatype: "basic";
		IStringConverter conv = getStringConverters().get(mediatype);
		return conv.convertObject(val, type, cl, context);	
	}
	
	/**
	 *  Convert string to object.
	 *  @param val The value.
	 *  @return The object.
	 */
	public Object convertStringToObject(String val, Class<?> type, ClassLoader cl, String mediatype, Object context)
	{
		mediatype = mediatype!=null? mediatype: "basic";
		IStringConverter conv = getStringConverters().get(mediatype);
		return conv.convertString(val, type, cl, context);
	}
}
