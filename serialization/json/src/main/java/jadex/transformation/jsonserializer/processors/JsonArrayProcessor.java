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
public class JsonArrayProcessor extends AbstractJsonProcessor
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

		return (object instanceof JsonArray && (clazz==null || clazz.isArray())) || 
			(object instanceof JsonObject && ((JsonObject)object).get(JsonTraverser.ARRAY_MARKER)!=null);
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
		Class<?> clazz = SReflect.getClass(type);
		return clazz!=null && clazz.isArray();
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
		Class<?> clazz = SReflect.getClass(type);
		
		JsonArray array;
		Class<?> compclazz = null;
		JsonValue idx = null;
		if(((JsonValue)object).isArray())
		{
			compclazz = clazz!=null? clazz.getComponentType(): null;
			array = (JsonArray)object;
		}
		else
		{
			JsonObject obj = (JsonObject)object;
			compclazz = JsonTraverser.findClazzOfJsonObject(obj, targetcl);
			array = (JsonArray)obj.get(JsonTraverser.ARRAY_MARKER);
			idx = (JsonValue)obj.get(JsonTraverser.ID_MARKER);
		}
			
		Object ret = getReturnObject(array, compclazz, targetcl);
//		traversed.put(object, ret);
		
		if(idx!=null)
			context.addKnownObject(ret, idx.asInt());
		
		Class<?> ccl = compclazz != null? ret.getClass().getComponentType() : null;
			
		for(int i=0; i<array.size(); i++)
		{
			Object val = array.get(i);
			//System.out.println("val class: " + val != null? val.getClass() : "null");
			Object newval = traverser.doTraverse(val, ccl, conversionprocessors, processors, converter, mode, targetcl, context);
			//System.out.println("newval class: " + newval != null? newval.getClass() : "null");
			if(newval != Traverser.IGNORE_RESULT && newval!=val)
			{
				if (newval != null && SReflect.isBasicType(newval.getClass()))
					Array.set(ret, i, traverser.convertBasicType(converter, newval, compclazz, targetcl, context));
				else
					Array.set(ret, i, newval);
				//Array.set(ret, i, JsonBeanProcessor.convertBasicType(newval, clazz));	
			}
		}
		
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
		
		Class<?> clazz = SReflect.getClass(type);
		Class<?> compclazz = clazz.getComponentType();
		
		if(wr.isWriteClass() || wr.isWriteId())
		{
			// just increase reference count because these helper objects do not count on read side
//			wr.incObjectCount();
			wr.write("{");
			if(wr.isWriteClass())
			{
				wr.writeClass(compclazz);
				wr.write(",");
			}
			if(wr.isWriteId())
			{
				wr.writeId();
				wr.write(",");
			}
			wr.writeString(JsonTraverser.ARRAY_MARKER);
			wr.write(":");
		}
		
		wr.write("[");
		
		for(int i=0; i<Array.getLength(object); i++) 
		{
			if(i>0)
				wr.write(",");
			Object val = Array.get(object, i);
			traverser.doTraverse(val, val!=null? val.getClass(): null, conversionprocessors, processors, converter, mode, targetcl, wr);
		}
		
		wr.write("]");
		
		if(wr.isWriteClass() || wr.isWriteId())
		{
			wr.write("}");
		}
		
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
