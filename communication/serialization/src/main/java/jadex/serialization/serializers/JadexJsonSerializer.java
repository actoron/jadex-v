package jadex.serialization.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jadex.common.SUtil;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.IErrorReporter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.serialization.ISerializer;
import jadex.transformation.jsonserializer.JsonTraverser;

/**
 *  The Jadex JSON serializer. Codec supports parallel
 *  calls of multiple concurrent clients (no method
 *  synchronization necessary).
 *  
 *  Converts object -> byte[] and byte[] -> object.
 */
public class JadexJsonSerializer implements ISerializer, IStringConverter
{
	//-------- constants --------
	
	/** The serializer id. */
	public static final int SERIALIZER_ID = 1;
	
	public static final String TYPE = IStringConverter.TYPE_JSON;
	
	/** The debug flag. */
	protected boolean DEBUG = false;
	
	/** The write processors. */
	public List<ITraverseProcessor> writeprocs;
	
	/** The read processors. */
	public List<ITraverseProcessor> readprocs;
	
	/** The basic string converter. */
	protected IStringConverter converter;
	
	/**
	 *  Create a new serializer.
	 */
	public JadexJsonSerializer()
	{
		writeprocs = Collections.synchronizedList(new ArrayList<ITraverseProcessor>());
		//writeprocs.add(new JsonServiceProcessor());
		writeprocs.addAll(JsonTraverser.writeprocs);
		
		readprocs = Collections.synchronizedList(new ArrayList<ITraverseProcessor>());
		//readprocs.add(new jadex.platform.service.serialization.serializers.jsonread.JsonServiceProcessor());
		readprocs.addAll(JsonTraverser.readprocs);
		
		converter = new JadexBasicTypeSerializer();
	}
	
	//-------- methods --------
	
	/**
	 *  Get the serializer id.
	 *  @return The serializer id.
	 */
	public int getSerializerId()
	{
		return SERIALIZER_ID;
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
		// This is not perfect...
		byte[] enc = encode(val, classloader, preprocs, usercontext);
		try
		{
			os.write(enc);
		}
		catch (IOException e)
		{
			SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Encode data with the serializer.
	 *  @param val The value.
	 *  @param classloader The classloader.
	 *  @param preproc The encoding preprocessors.
	 *  @return The encoded object.
	 */
	public byte[] encode(Object val, ClassLoader classloader, ITraverseProcessor[] preprocs, Object usercontext)
	{
		boolean writeclass = true;
		boolean writeid = true;
		
		if(usercontext instanceof Map)
		{
			Map conv = (Map)usercontext;
			writeclass = conv.get("writeclass") instanceof Boolean? (Boolean)conv.get("writeclass"): true;
			writeid = conv.get("writeid") instanceof Boolean? (Boolean)conv.get("writeid"): true;
		}
		
		byte[] ret = JsonTraverser.objectToByteArray(val, classloader, null, writeclass, writeid, null, preprocs!=null?Arrays.asList(preprocs):null, writeprocs, usercontext, converter);
		
		if(DEBUG)
			System.out.println("encode message: "+(new String(ret, SUtil.UTF8)));
		return ret;
	}

	/**
	 *  Decode an object.
	 *  @return The decoded object.
	 *  @throws IOException
	 */
	public Object decode(byte[] bytes, ClassLoader classloader, ITraverseProcessor[] postprocs, IErrorReporter rep, Object usercontext)
	{
		if(DEBUG)
			System.out.println("decode message: "+(new String((byte[])bytes, SUtil.UTF8)));
		return JsonTraverser.objectFromByteArray(bytes, classloader, rep, null, null, readprocs, postprocs!=null?Arrays.asList(postprocs):null, usercontext, converter);
	}
	
	/**
	 *  Decode an object.
	 *  @return The decoded object.
	 *  @throws IOException
	 */
	public Object decode(InputStream is, ClassLoader classloader, ITraverseProcessor[] postprocs, IErrorReporter rep, Object usercontext)
	{
		
		byte[] bytes = null;
		try
		{
			bytes = SUtil.readStream(is);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		try
		{
			is.close();
		}
		catch (IOException e)
		{
		}
		
		
		return decode(bytes, classloader, postprocs, rep, usercontext);
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
		return JsonTraverser.objectFromString(val, cl, null, type, readprocs, null, context, converter);
	}
	
	private static JadexBasicTypeSerializer bts = new JadexBasicTypeSerializer();
	public Object convertBasicType(String val, Class<?> targettype, ClassLoader cl, Object context)
	{
		return bts.convertString(val, targettype, cl, context);
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
		// does not use type currently?!
		String ret = JsonTraverser.objectToString(val, cl, true, true, null, null, writeprocs, context, converter);
		//if((""+val).indexOf("ChatEvent")!=-1)
		//System.out.println("json: "+ret);
		return ret;
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
	 *  Add a read/write processor pair.
	 */
	public void addProcessor(ITraverseProcessor read, ITraverseProcessor write)
	{
		readprocs.add(0, read);
		writeprocs.add(0, write);
	}
	
	/**
	 *  Test if the type can be converted.
	 *  @param clazz The class.
	 *  @return True if can be converted.
	 */
	public boolean isSupportedType(Class<?> clazz)
	{
		return true;
	}
}