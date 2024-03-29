package jadex.binary;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import jadex.common.SReflect;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.common.transformation.traverser.Traverser.MODE;

/**
 *  Codec for encoding and decoding InetAddress objects.
 */
public class InetAddressCodec extends AbstractCodec
{
	
	/**
	 *  Tests if the decoder can decode the class.
	 *  @param clazz The class.
	 *  @return True, if the decoder can decode this class.
	 */
	public boolean isApplicable(Class<?> clazz)
	{
		return SReflect.isSupertype(InetAddress.class, clazz);
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
		InetAddress ret = null;
		try
		{
			ret = InetAddress.getByName(context.readString());
		}
		catch (UnknownHostException e)
		{
			throw new RuntimeException(e);
		}
		return ret;
	}

	/**
	 *  Encode the object.
	 */
	public Object encode(Object object, Class<?> clazz, List<ITraverseProcessor> preprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, Traverser traverser, ClassLoader targetcl, IEncodingContext ec)
	{
		ec.writeString(((InetAddress)object).getHostAddress());
		
		return object;
	}
}
