package jadex.publishservice.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jadex.common.ClassInfo;
import jadex.common.SUtil;
import jadex.common.transformation.BasicTypeConverter;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.IStringObjectConverter;
import jadex.common.transformation.traverser.IErrorReporter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.serialization.ISerializer;

/**
 *  Serializer impl for converting basic types including Jadex specific ones like ComponentIdentifier etc.
 */
public class JadexBasicTypeSerializer implements ISerializer, IStringConverter
{
	//-------- constants --------
	
	/** The serializer id. */
	public static final int SERIALIZER_ID = 2;

	public static final String TYPE = IStringConverter.TYPE_BASIC;
	
	/** The debug flag. */
	protected boolean DEBUG = false;
	
	protected BasicTypeConverter converter;
	
	//-------- methods --------
	
	/**
	 *  Get the serializer id.
	 *  @return The serializer id.
	 */
	public int getSerializerId()
	{
		//throw new UnsupportedOperationException();
		return SERIALIZER_ID;
	}
	
	 /** The basic type converter. */
//  public static BasicTypeConverter BASICCONVERTER;
  
	public JadexBasicTypeSerializer()
	{
		converter = new BasicTypeConverter();
  		/*converter.addConverter(IComponentIdentifier.class, new IStringObjectConverter()
		{
			public Object convertString(String val, Object context) throws Exception
			{
				return new ComponentIdentifier(val);
			}
		});*/
  		converter.addConverter(ClassInfo.class, new IStringObjectConverter()
		{
			public Object convertString(String val, Object context) throws Exception
			{
				return new ClassInfo(val);
			}
		});
	}
	
	/**
	 *  Encode data with the serializer.
	 *  @param os The output stream for writing.
	 *  @param val The value.
	 *  @param classloader The classloader.
	 *  @param preproc The encoding preprocessors.
	 */
	public void encode(OutputStream os, Object val, ClassLoader classloader, ITraverseProcessor[] preprocs, Object usercontext)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 *  Decode an object.
	 *  @return The decoded object.
	 *  @throws IOException
	 */
	public Object decode(byte[] bytes, ClassLoader classloader, ITraverseProcessor[] postprocs, IErrorReporter rep, Object usercontext)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Decode an object.
	 *  @return The decoded object.
	 *  @throws IOException
	 */
	public Object decode(InputStream is, ClassLoader classloader, ITraverseProcessor[] postprocs, IErrorReporter rep, Object usercontext)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 *  Convert a string to an object.
	 *  @param val The string.
	 *  @param type The target type.
	 *  @param context The context.
	 *  @return The object.
	 */
	public Object convertString(String val, Class<?> type, ClassLoader cl, Object context)
	{
		try
		{
			return converter.convertString(val, type, context);
		}
		catch(Exception e)
		{
			return SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Convert an object to a string.
	 *  @param val The object.
	 *  @param type The encoding type.
	 *  @param context The context.
	 *  @return The object.
	 */
	public String convertObject(Object val, Class<?> type, ClassLoader cl, Object context)
	{
		try
		{
			return converter.convertObject(val, type, context);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Get the type of string that can be processed (xml, json, plain).
	 *  @return The object.
	 */
	public String getType()
	{
		return TYPE;
	}
	
	/**
	 *  Test if the type can be converted.
	 *  @param clazz The class.
	 *  @return True if can be converted.
	 */
	public boolean isSupportedType(Class<?> clazz)
	{
		return converter.isSupportedType(clazz);
	}
}