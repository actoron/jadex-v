package jadex.transformation.jsonserializer.processors;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.List;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.common.Base64;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.common.transformation.traverser.Traverser.MODE;
import jadex.transformation.jsonserializer.JsonTraverser;

/**
 * 
 */
public class JsonByteArrayProcessor extends AbstractJsonProcessor
{
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	protected boolean isApplicable(Object object, Type type, ClassLoader targetcl, JsonReadContext context)
	{
		Class<?> clazz = SReflect.getClass(type);
		return object instanceof JsonObject && (SReflect.isSupertype(byte[].class, clazz) || SReflect.isSupertype(Byte[].class, clazz));
		
	}
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	protected boolean isApplicable(Object object, Type type, ClassLoader targetcl, JsonWriteContext context)
	{
		return object instanceof byte[];
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 * @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	protected Object readObject(Object object, Type type, Traverser traverser, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, ClassLoader targetcl, JsonReadContext context)
	{
		JsonObject obj = (JsonObject)object;
		
		byte[] ret = Base64.decode(obj.get("__base64").asString().getBytes(SUtil.UTF8));
		
		JsonValue idx = (JsonValue)obj.get(JsonTraverser.ID_MARKER);
		if(idx!=null)
			((JsonReadContext)context).addKnownObject(ret, idx.asInt());
		return ret;
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 * @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	protected Object writeObject(Object object, Type type, Traverser traverser, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, ClassLoader targetcl, JsonWriteContext wr)
	{
		wr.addObject(wr.getCurrentInputObject());
		
		wr.write("{");
		
//		wr.writeClass(byte[].class);
		if(wr.isWriteClass())
			wr.write("\"").write(JsonTraverser.CLASSNAME_MARKER).write("\":\"bytearray\",");
		
		if(wr.isWriteId())
		{
			wr.writeId();
			wr.write(",");
		}
		
		wr.write("\"__base64\":");
		wr.write("\"" +new String(Base64.encode((byte[])object), SUtil.UTF8)+ "\"");
		wr.write("}");
		return object;
	}

	/**
	 * 
	 */
	public Object getReturnObject(Object object, Class<?> clazz, ClassLoader targetcl)
	{
		if(clazz!=null && targetcl!=null)
			clazz = SReflect.classForName0(SReflect.getClassName(clazz), targetcl);

		if(clazz==null)
			clazz = Object.class;
		
		int length = ((JsonArray)object).size();
		return Array.newInstance(clazz, length);
	}
}
