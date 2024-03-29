package jadex.binary;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import jadex.common.SUtil;
import jadex.common.transformation.STransformation;
import jadex.common.transformation.traverser.ITraverseProcessor;

/**
 *  Context for encoding (serializing) an object in a binary format.
 *
 */
public class EncodingContext extends AbstractEncodingContext
{
	/** Cache for class names. */
	protected Map<Class<?>, String> classnamecache = new HashMap<Class<?>, String>();
	
	/** The binary output */
	protected OutputStream os;
	
	/**
	 *  Creates an encoding context.
	 *  @param usercontext A user context.
	 *  @param preprocessors The preprocessors.
	 *  @param classloader The classloader.
	 */
	public EncodingContext(OutputStream os, Object rootobject, Object usercontext, List<ITraverseProcessor> preprocessors, ClassLoader classloader, SerializationConfig config)
	{
		super(rootobject, usercontext, preprocessors, classloader, config);
		this.os = os;
	}
	
	/**
	 *  Writes a byte.
	 *  @param b The byte.
	 */
	public void writeByte(byte b)
	{
		try
		{
			os.write(b);
			++writtenbytes;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Writes a byte array, appending it to the buffer.
	 *  @param b The byte array.
	 */
	public void write(byte[] b)
	{
		try
		{
			os.write(b);
			writtenbytes += b.length;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
