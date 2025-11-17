package jadex.transformation.jsonserializer.processors;

import java.lang.reflect.Type;
import java.util.List;

import com.eclipsesource.json.JsonValue;

import jadex.common.SReflect;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.common.transformation.traverser.Traverser.MODE;

/**
 *  Handle primitive types and null.
 */
public class JsonPrimitiveProcessor implements ITraverseProcessor
{
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Type type, ClassLoader targetcl, Object context)
	{
		boolean ret = false;
		JsonValue val = (JsonValue)object;
		ret = val.isString() || val.isBoolean() || val.isNumber() || val.isNull();
		return ret;
	}
	
	/**
	 *  Process an object.
	 *  @param object The object.
	 * @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return The processed object.
	 */
	public Object process(Object object, Type type, Traverser traverser, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, ClassLoader targetcl, Object context)
	{

		Object ret = null;
		Class<?> clazz = SReflect.getClass(type);
		
		JsonValue val = (JsonValue)object;
		if(val.isNumber())
		{
			if(Double.class.equals(clazz) || double.class.equals(clazz))
			{
				ret = val.asDouble();
			}
			else if(Float.class.equals(clazz) || float.class.equals(clazz))
			{
				ret = val.asFloat();
			}
			else if(Integer.class.equals(clazz) || int.class.equals(clazz))
			{
				try
				{
					ret = val.asInt();
				}
				catch(Exception e)
				{
					ret = (int)val.asDouble();
				}
			}
			else if(Long.class.equals(clazz) || long.class.equals(clazz))
			{
				try
				{
					ret = val.asLong();
				}
				catch(Exception e)
				{
					ret = (long)val.asDouble();
				}
			}
			else if(Short.class.equals(clazz) || short.class.equals(clazz))
			{
				try
				{
					ret = (short)val.asInt();
				}
				catch(Exception e)
				{
					ret = (short)val.asDouble();
				}
			}
			else if(String.class.equals(clazz))
			{
				ret = val.toString();
			}
			else 
			{
				ret = val.asDouble();
				try 
				{
					long num = val.asLong();
					if (num <= Integer.MAX_VALUE && num >= Integer.MIN_VALUE) 
					{
						ret = (int) num;
					} 
					else 
					{
						ret = num;
					}
				} 
				catch (Exception e) 
				{
					ret = val.asDouble();
				}
			}
		}
		else if(val.isBoolean())
		{
			ret = val.asBoolean();
		}
		else if(val.isString())
		{
			ret = val.asString();
		}
		
//		traversed.put(object, ret);
		
		return ret;
	}
}
