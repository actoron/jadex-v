package jadex.serialization;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.core.impl.Component;

/**
 *  Functionality for managing serialization.
 *
 */
public interface ISerializationServices
{
	/**
	 *  Gets an instance of the serialization services.
	 *  @return Instance of serialization services, thread safe.
	 */
	public static ISerializationServices get()
	{
		return SerializationServices.get();
	};
	
	/**
	 *  Encodes/serializes an object for a particular receiver.
	 *  
	 *  @param os OutputStream to write the object.
	 *  @param cl The classloader used for encoding.
	 *  @param obj Object to be encoded.
	 */
	public void encode(OutputStream os, ClassLoader cl, Object obj);
	
	/**
	 *  Decodes/deserializes an object.
	 *  
	 *  @param is InputStream to read.
	 *  @param cl The classloader used for decoding.
	 *  @return Object to be encoded.
	 *  
	 */
	public Object decode(InputStream is, ClassLoader cl);
	
	/**
	 *  Test if an object is a remote object.
	 */
	public boolean isRemoteObject(Object target);
	
	/**
	 *  Test if an object has reference semantics. It is a reference when:
	 *  - it implements IRemotable
	 *  - it is an IService, IExternalAccess or IFuture
	 *  - if the object has used an @Reference annotation at type level
	 *  - has been explicitly set to be reference
	 */
	public boolean isLocalReference(Object object);
	
	/**
	 *  Gets the pre-processors for encoding a received message.
	 */
	public List<ITraverseProcessor> getPreprocessors();
	
	/**
	 *  Gets the post-processors for decoding a received message.
	 */
	public List<ITraverseProcessor> getPostprocessors();
	
	/**
	 *  Get the clone processors.
	 *  @return The clone processors.
	 */
	public List<ITraverseProcessor> getCloneProcessors();
	
	/**
	 *  Get the string converters (can convert to and from string, possibly only for some types).
	 *  @return The converters by name (constants in IStringConverter).
	 */
	public Map<String, IStringConverter> getStringConverters();
	
	/**
	 *  Convert object to string.
	 *  @param val The value.
	 *  @return The string value.
	 * /
	public String convertObjectToString(Object val, Class<?> type, ClassLoader cl, String mediatype, Object context);*/
	
	/**
	 *  Convert string to object.
	 *  @param val The value.
	 *  @return The object.
	 * /
	public Object convertStringToObject(String val, Class<?> type, ClassLoader cl, String mediatype, Object context);*/

	
//	/**
//	 *  Gets the remote reference management.
//	 *
//	 *  @return The remote reference management.
//	 */
//	public IRemoteReferenceManagement getRemoteReferenceManagement();
//
//	/**
//	 *  Gets the remote reference module.
//	 *
//	 *  @return The remote reference module.
//	 */
//	public IRemoteReferenceModule getRemoteReferenceModule();
//
//	/**
//	 *  Sets the remote reference module.
//	 *
//	 *  @param rrm The remote reference module.
//	 */
//	public void setRemoteReferenceModule(IRemoteReferenceModule rrm);
	
//	/**
//	 *  Returns the serializer for sending.
//	 *  
//	 *  @param platform Sending platform.
//	 *  @param receiver Receiving platform.
//	 *  @return Serializer.
//	 */
//	public ISerializer getSendSerializer(IComponentIdentifier receiver);
	
	/**
	 *  Returns all serializers.
	 *  @return Serializers.
	 */
	public ISerializer[] getSerializers();
	
	/**
	 *  Returns a serializer per id.
	 *  @param id The id.
	 *  @return The serializer.
	 */
	public ISerializer getSerializer(int id);
	
//	/**
//	 *  Returns the codecs for sending.
//	 *  
//	 *  @param receiver Receiving platform.
//	 *  @return Codecs.
//	 */
//	public ICodec[] getSendCodecs(IComponentIdentifier receiver);
	
//	/**
//	 *  Returns all codecs.
//	 *  
//	 *  @return Codecs.
//	 */
//	public Map<Integer, ICodec> getCodecs();
	
//	/**
//	 *  Gets the post-processors for decoding a received message.
//	 */
//	public ITraverseProcessor[] getPostprocessors();
//	
//	/**
//	 *  Gets the pre-processors for encoding a received message.
//	 */
//	public ITraverseProcessor[] getPreprocessors();
}
