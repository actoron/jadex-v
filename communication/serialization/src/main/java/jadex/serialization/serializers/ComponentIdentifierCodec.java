package jadex.serialization.serializers;

import java.util.List;

import jadex.binary.AbstractCodec;
import jadex.binary.IDecodingContext;
import jadex.binary.IEncodingContext;
import jadex.binary.SBinarySerializer;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.common.transformation.traverser.Traverser.MODE;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;

/**
 *  Codec for big integers.
 */
public class ComponentIdentifierCodec extends AbstractCodec
{
	/**
	 *  Tests if the decoder can decode the class.
	 *  @param clazz The class.
	 *  @return True, if the decoder can decode this class.
	 */
	public boolean isApplicable(Class<?> clazz)
	{
		return ComponentIdentifier.class.equals(clazz);
	}
	
	/**
	 *  Creates the object during decoding.
	 *  
	 *  @param clazz The class of the object.
	 *  @param context The decoding context.
	 *  @return The created object.
	 */
	public Object createObject(Class<?> clazz, IDecodingContext context)
	{
		String localname = context.readString();
		GlobalProcessIdentifier gpid = (GlobalProcessIdentifier) SBinarySerializer.decodeObject(context);
		return new ComponentIdentifier(localname, gpid);
	}
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Class<?> clazz, boolean clone, ClassLoader targetcl)
	{
		return isApplicable(clazz);
	}
	
	/**
	 *  Encode the object.
	 */
	public Object encode(Object object, Class<?> clazz, List<ITraverseProcessor> preprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, Traverser traverser, ClassLoader targetcl, IEncodingContext ec)
	{
		ComponentIdentifier cid = (ComponentIdentifier) object;
		ec.writeString(cid.getLocalName());
		traverser.doTraverse(cid.getGlobalProcessIdentifier(), cid.getGlobalProcessIdentifier().getClass(), preprocessors, processors, converter, mode, ec.getClassLoader(), ec);
		return object;
	}
}
