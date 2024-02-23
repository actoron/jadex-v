package jadex.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jadex.common.transformation.traverser.IErrorReporter;
import jadex.common.transformation.traverser.ITraverseProcessor;

/**
 *  Encode and decode an object from a byte representation.
 */
public interface ISerializer
{
	/** Constant for accessing the serializer id. */
	public static final String SERIALIZER_ID = "SERIALIZER_ID";
	
	/**
	 *  Get the serializer id.
	 *  @return The serializer id.
	 */
	public int getSerializerId();
	
	/**
	 *  Encode data with the serializer.
	 *  @param os The output stream for writing.
	 *  @param val The value.
	 *  @param classloader The classloader.
	 *  @param preproc The encoding preprocessors.
	 */
	public void encode(OutputStream os, Object val, ClassLoader classloader, ITraverseProcessor[] preprocs, Object usercontext);
	
	/**
	 *  Decode an object.
	 *  @return The decoded object.
	 *  @throws IOException
	 */
	public Object decode(InputStream is, ClassLoader classloader, ITraverseProcessor[] postprocs, IErrorReporter rep, Object usercontext);
}