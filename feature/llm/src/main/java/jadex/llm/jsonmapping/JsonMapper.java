package jadex.llm.jsonmapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.core.impl.ComponentManager;
import jadex.future.IFuture;
import jadex.llm.annotation.McpToolParam;
import jadex.publishservice.publish.annotation.ResultMapper;
import jadex.publishservice.publish.json.PublishJsonSerializer;
import jadex.publishservice.publish.mapper.IValueMapper;

import jadex.transformation.jsonserializer.JsonTraverser;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;


public class JsonMapper 
{
    /**
	 *  Get metainfo about parameters from the target method via annotations.
     *  @param method The target method.
     *  @return A tuple with a list of parameter info and a map with target parameter
	 */
	public static Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> getParameterInfos(Method method)
	{
		List<Tuple2<String, String>> ret = new ArrayList<>();
		Map<String, Class<?>> targettypes = new HashMap<>();
		
		Annotation[][] anns = method.getParameterAnnotations();
		Class<?>[] types = method.getParameterTypes();
		
		for(int i=0; i<anns.length; i++)
		{
			boolean done = false;
			for(Annotation ann: anns[i])
			{
				if(ann instanceof PathParam)
				{
					PathParam pp = (PathParam)ann;
					String name = pp.value();
					ret.add(new Tuple2<String, String>("path", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
				else if(ann instanceof QueryParam)
				{
					QueryParam qp = (QueryParam)ann;
					String name = qp.value();
					ret.add(new Tuple2<String, String>("query", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
				else if(ann instanceof FormParam)
				{
					FormParam qp = (FormParam)ann;
					String name = qp.value();
					ret.add(new Tuple2<String, String>("form", name));
					targettypes.put(name, types[i]);
					done = true;
					break;
				}
//				else if(ann instanceof ParameterInfo)
//				{
//					ParameterInfo qp = (ParameterInfo)ann;
//					String name = qp.value();
//					ret.add(new Tuple2<String, String>("name", name));
//					targettypes.put(name, types[i]);
//					done = true;
//					break;
//				}
				/*else
				{
					String name = ""+i;
					ret.add(new Tuple2<String, String>("no", name));
					targettypes.put(name, types[i]);
				}*/
	        }
			if(!done) //if(anns[i].length==0)
			{
				String name = ""+i;
				ret.add(new Tuple2<String, String>("no", name));
				targettypes.put(name, types[i]);
			}
		}
		
		return new Tuple2<>(ret, targettypes);
	}

    /**
	 *  Generate in parameters that are correct wrt order and number of targetparameter (must convert types possibly).
	 */
	public static Object[] generateInParameters(Map<String, Object> inparamsmap, Tuple2<List<Tuple2<String, String>>, Map<String, Class<?>>> pinfos, Class<?>[] types)
	{
		// The order of in parameters is corrected with respect to the
		// target parameter order
		Object[] inparams = new Object[types.length];
		
		// Iterate over given method parameter annotations in order
		//for(Tuple2<String, String> pinfo: pinfos.getFirstEntity())
		List<Integer> todo = new ArrayList<>();
		for(int i=0; i<pinfos.getFirstEntity().size(); i++)
		{
			Tuple2<String, String> pinfo = pinfos.getFirstEntity().get(i);
			
			if("name".equals(pinfo.getFirstEntity()))
			{
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
			}
			else if("path".equals(pinfo.getFirstEntity()))
			{
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
				//binding.get(pinfo.getSecondEntity());
			}
			else if("query".equals(pinfo.getFirstEntity()))
			{
				// query params are in normal parameter map
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
			}
			else if("form".equals(pinfo.getFirstEntity()))
			{
				// query params are in normal parameter map
				inparams[i] = inparamsmap.remove(pinfo.getSecondEntity());
			}
			else
			{
				todo.add(i);
			}
		}
		
		Iterator<String> innames = inparamsmap.keySet().iterator();
		for(int i: todo)
		{
			Tuple2<String, String> pinfo = pinfos.getFirstEntity().get(i);
			String inname = innames.hasNext()? innames.next(): null;
			
			if("no".equals(pinfo.getFirstEntity()) && inname!=null && inparamsmap.get(inname)!=null)
			{
				inparams[i] = inparamsmap.get(inname);
			}
		}
		
		for (int i = 0; i < inparams.length; i++) 
		{
        	if (inparams[i] != null && !types[i].isInstance(inparams[i])) 
            	inparams[i] = convertParameter(inparams[i], types[i]);
        }

		return inparams;
	}

	/**
	 *  Convert a json string to a java object.
	 *  @param val The json string.
	 *  @param type The target class.
	 *  @param cl The classloader.
	 *  @param tomap Flag, if a (nested) map should be read (only possible if type is map too).
	 */
	public static Object convertJsonValue(String val, Class<?> type, ClassLoader cl, boolean tomap)
	{
		List<ITraverseProcessor> procs = null;

		if(tomap && SReflect.isSupertype(Map.class, type))
			procs = JsonTraverser.nestedreadprocs;
		return JsonTraverser.objectFromString(val.toString(), cl, null, type, procs);
	}
	
	/**
	 * Convert a (string) parameter
	 * 
	 * @param val The value.
	 * @param target The target class.
	 * @return The converted object.
	 */
	public static Object convertParameter(Object val, Class<?> target) 
	{
    	if (val == null) 
			return null;

		if (SReflect.isSupertype(target, val.getClass())) 
			return val;

    	IStringConverter conv = PublishJsonSerializer.getBasicTypeSerializer();

		// String -> target type
		if (val instanceof String str && str.length() > 0 && conv != null && conv.isSupportedType(target)) 
		{
			try 
			{
				return conv.convertString(str, target, getClassLoader(), null);
			} 
			catch (Exception e) 
			{
			}
		}

		// Primitive / andere Objekte → String
		// todo: check if primitive type
		if (target == String.class) 
		{
			return val.toString();
		}

    	return val;
	}

	/**
	 * Convert a parameter string to an object if is json or xml.
	 * 
	 * @param sr The media types.
	 * @param val The string value.
	 * @return The decoded object.
	 */
	protected static Object convertParameter(List<String> sr, Object val, Class<?> targetclazz)
	{
		Object ret = val;
		boolean done = false;

		if(val instanceof String)
		{
			if(sr != null && (sr.contains(MediaType.APPLICATION_JSON) || sr.contains(MediaType.WILDCARD)))
			{
				try
				{
                    
					ret = PublishJsonSerializer.get().convertString((String)val, targetclazz, getClassLoader(), null);
					//ret = JsonTraverser.objectFromByteArray(val.getBytes(SUtil.UTF8), component.getClassLoader(), (IErrorReporter)null, null, targetclazz);
					// ret = JsonTraverser.objectFromByteArray(val.getBytes(),
					// component.getClassLoader(), (IErrorReporter)null);
					done = true;
				}
				catch(Exception e)
				{
					//e.printStackTrace();
				}
			}
	
			
			// todo?
			
			/*if(!done && sr != null && (sr.contains(MediaType.APPLICATION_XML) || sr.contains(MediaType.WILDCARD)))
			{
				try
				{
					ret = binser.decode(((String)val).getBytes(StandardCharsets.UTF_8), getClassLoader(), null, null, null);
					//ret = JavaReader.objectFromByteArray(val.getBytes(), component.getClassLoader(), null);
					done = true;
				}
				catch(Exception e)
				{
				}
			}*/
		}
				
		if(!done)
		{
			ret = convertParameter(val, targetclazz);
		}

		return ret;
	}

	/**
	 * Map a result using the result mapper.
	 */
	protected static Object mapResult(Method method, Object ret)
	{
		if(method!=null && method.isAnnotationPresent(ResultMapper.class))
		{
			try
			{
				ResultMapper mm = method.getAnnotation(ResultMapper.class);
				Class<?> pclazz = mm.value();
				IValueMapper mapper;
				// System.out.println("res mapper: "+clazz);
				if(!Object.class.equals(pclazz))
				{
					mapper = (IValueMapper)pclazz.getDeclaredConstructor().newInstance();
					ret = mapper.convertValue(ret);
				}
			}
			catch(Exception e)
			{
				SUtil.throwUnchecked(e);
			}
		}
		// else
		// {
		// NativeResponseMapper mapper = new NativeResponseMapper();
		// ret = mapper.convertValue(ret);
		// }

		return ret;
	}

	public static JsonValue toJsonObject(Object value)
	{
		return toJsonObject(value, false, false);
	}

	public static JsonValue toJsonObject(Object value, boolean writeclass, boolean writeid)
	{
		if(value == null)
			return Json.NULL;

		try
		{
			// Serialize Java object → JSON string
			String json = PublishJsonSerializer.get().convertObject(value, null, getClassLoader(), null, writeclass, writeid);

			// Parse JSON string → JsonObject (MinimalJSON)
			return Json.parse(json);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Cannot serialize result to JSON: " + value, e);
		}
	}

	public static Class<?> getReturnType(Method m)
	{
		Class<?> raw = m.getReturnType();

		if (!IFuture.class.isAssignableFrom(raw))
			return raw;

		Type gt = m.getGenericReturnType();

		if (gt instanceof ParameterizedType pt)
		{
			Type[] args = pt.getActualTypeArguments();
			if (args.length == 1)
			{
				Type t = args[0];

				if (t instanceof Class<?> c)
					return c;

				if (t instanceof ParameterizedType ptt)
					return (Class<?>) ptt.getRawType(); // z.B. List<User>
			}
		}

		// Fallback: IFuture without type info
		return Object.class;
	}

	public static String generateOutputSchema(Method m)
	{
		JsonObject schema = new JsonObject();

		Class<?> ret = m.getReturnType();

		if (IFuture.class.isAssignableFrom(ret))
		{
			ret = getReturnType(m);
		}

		if (ret == void.class || ret == Void.class)
		{
			schema.add("type", "null");
			return schema.toString();
		}

		return generateSchemaForType(ret).toString();
	}

	public static String generateInputSchema(Method m) 
	{
		JsonObject schema = new JsonObject();
		schema.add("type", "object"); // send call as object with named parameters
		JsonObject props = new JsonObject();

		Class<?>[] paramTypes = m.getParameterTypes();
		String[] paramNames = getParameterNames(m); 
		String[] paramDescs = getParameterDescriptions(m);
		boolean[] paramRequired = getParameterRequired(m);

		for(int i=0; i<paramTypes.length; i++) 
		{
			JsonObject prop = generateSchemaForType(paramTypes[i]);
			if(paramDescs[i] != null && !paramDescs[i].isEmpty())
				prop.add("description", paramDescs[i]);
			props.add(paramNames[i], prop);
		}

		schema.add("properties", props);

		JsonArray required = new JsonArray();
		for(int i=0; i<paramNames.length; i++) 
		{
			if(paramRequired[i]) 
				required.add(paramNames[i]);
		}
		schema.add("required", required);

		return schema.toString();
	}

	public static JsonObject generateSchemaForType(Class<?> type) 
	{
		JsonObject schema = new JsonObject();
		if(type == String.class) schema.add("type", "string");
		else if(type == int.class || type == Integer.class) schema.add("type", "integer");
		else if(type == long.class || type == Long.class) schema.add("type", "integer");
		else if(type == double.class || type == Double.class) schema.add("type", "number");
		else if(type == boolean.class || type == Boolean.class) schema.add("type", "boolean");
		else if(Collection.class.isAssignableFrom(type)) 
		{
			schema.add("type", "array");
			schema.add("items", new JsonObject()); // optional: generic types?!
		} 
		else 
		{
			schema.add("type", "object");
			// Optional: recursive schema generation for custom classes (could lead to infinite recursion)
		}
		return schema;
	}

	public static String[] getParameterNames(Method method) 
	{
        Parameter[] params = method.getParameters();
        String[] ret = new String[params.length];

        for (int i = 0; i < params.length; i++) 
		{
            String name = null;

            if (params[i].isNamePresent()) 
			{
                name = params[i].getName();
            }

			if(name==null)
			{
				McpToolParam ann = params[i].getAnnotation(McpToolParam.class);
				if (ann != null) 
					name = ann.name();
			}

            if (name == null) 
                name = "arg" + i;

            ret[i] = name;
        }

		return ret;
	}

	public static String[] getParameterDescriptions(Method method) 
	{
		Parameter[] params = method.getParameters();
		String[] ret = new String[params.length];

		for (int i = 0; i < params.length; i++) 
		{
			String desc = null;

			McpToolParam ann = params[i].getAnnotation(McpToolParam.class);
			if (ann != null) 
			{
				desc = ann.description();
			}

			ret[i] = desc != null ? desc : "";
		}

		return ret;
	}

	public static boolean[] getParameterRequired(Method method) 
	{
		Parameter[] params = method.getParameters();
		boolean[] ret = new boolean[params.length];

		for (int i = 0; i < params.length; i++) 
		{
			McpToolParam ann = params[i].getAnnotation(McpToolParam.class);
			ret[i] = ann != null ? ann.required() : true; // default: required
		}

		return ret;
	}

	public static Map<String, Object> convertJsonObjectToMap(JsonObject args)
	{
		Map<String, Object> ret = new LinkedHashMap<>();

        for(String key : args.names()) 
        {
            Object value = args.get(key);

            if (value instanceof JsonValue jsonVal) 
            {
                if (jsonVal.isNull()) 
                {
                    value = null;
                } 
                else if (jsonVal.isNumber()) 
                {
                    value = jsonVal.asInt(); // oder asLong/asDouble je nach Zieltyp
                } 
                else if (jsonVal.isBoolean()) 
                {
                    value = jsonVal.asBoolean();
                } 
                else if(jsonVal.isString()) 
                {
                    value = jsonVal.asString(); // !!! no extra quotes, just the raw string
                }
                else 
                {
                    value = jsonVal.toString();
                }
            }

            //System.out.println("Param: " + key + " -> " + value);
            ret.
			put(key, value);
        }
		return ret;
	}

    protected static ClassLoader getClassLoader()
	{
		return ComponentManager.get().getClassLoader();
	}
}