package jadex.transformation.jsonserializer.processors;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.transformation.BeanIntrospectorFactory;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.BeanProperty;
import jadex.common.transformation.traverser.IBeanIntrospector;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.common.transformation.traverser.Traverser.MODE;
import jadex.transformation.jsonserializer.JsonTraverser;

/**
 *  Bean processor for reading json objects.
 */
public class JsonBeanProcessor extends AbstractJsonProcessor
{
	/** Bean introspector for inspecting beans. */
	protected IBeanIntrospector intro = BeanIntrospectorFactory.get().getBeanIntrospector(5000);
	
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
		return object instanceof JsonObject && (clazz!=null && !SReflect.isSupertype(Map.class, clazz));
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
		return true;
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
		Object ret = null;
		Class<?> clazz = SReflect.getClass(type);
		
		ret = getReturnObject(object, clazz, targetcl);
//		traversed.put(object, ret);
		
		JsonValue idx = (JsonValue)((JsonObject)object).get(JsonTraverser.ID_MARKER);
		if(idx!=null)
			((JsonReadContext)context).addKnownObject(ret, idx.asInt());
		
		try
		{
			readProperties(object, clazz, conversionprocessors, processors, converter, mode, traverser, targetcl, ret, context, intro);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
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
//		System.out.println("fp: "+object);
		wr.addObject(wr.getCurrentInputObject());
		
		wr.write("{");
		
		if(wr.isWriteClass())
		{
			wr.writeClass(object.getClass());
			if(wr.isWriteId())
			{
				wr.write(",").writeId();
			}
//			wr.write(",");
		}
		else if(wr.isWriteId())
		{
			wr.writeId();
//			wr.write(",");
		}
		
		try
		{
//			System.out.println("cloned: "+object.getClass());
//			ret = object.getClass().newInstance();
			
			writeProperties(object, conversionprocessors, processors, converter, mode, traverser, targetcl, wr, intro, !wr.isWriteClass() && !wr.isWriteId());
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
		
		wr.write("}");
		
		return object;
	}
	
	/**
	 *  Clone all properties of an object.
	 */
	protected static void readProperties(Object object, Type type, List<ITraverseProcessor> postprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, Traverser traverser, ClassLoader targetcl, Object ret, JsonReadContext context, IBeanIntrospector intro)
	{
		// Get all declared fields (public, protected and private)
		
		JsonObject jval = (JsonObject)object;
		Class<?> clazz = SReflect.getClass(type);
		Map<String, BeanProperty> props = intro.getBeanProperties(clazz, true, false);
		
		for(String name: jval.names())
		{
			try
			{
				BeanProperty prop = (BeanProperty)props.get(name);
				if(prop!=null && prop.isReadable() && prop.isWritable())
				{
					JsonValue val = jval.get(name);
					if(val!=null && !val.isNull()) 
					{
						Type sot = null;
						if(val instanceof JsonObject)
							sot = JsonTraverser.findClazzOfJsonObject((JsonObject)val, targetcl);
						if(sot==null)
							sot = prop.getGenericType();
						
//						System.out.println("VAL " + ((JsonObject) val).toString());
//						System.out.println("CL " + ((JsonObject) val).getString(JsonTraverser.CLASSNAME_MARKER, null));
//						System.out.println("SOT: " +sot);
						Object newval = traverser.doTraverse(val, sot, postprocessors, processors, converter, mode, targetcl, context);
//						Object newval = traverser.doTraverse(val, sot, rsionprocessors, postprocessors, mode, targetcl, context)
//						Object newval = traverser.doTraverse(val, sot, cloned, null, processors, postprocessors, clone, targetcl, context);

						if(newval != Traverser.IGNORE_RESULT && (object!=ret || val!=newval))
						{
//							if ("result".equals(prop.getName()))
//								System.out.println("PROP SET CALLED");
							
							prop.setPropertyValue(ret, traverser.convertBasicType(converter, newval, prop.getType(), targetcl, context));
							//prop.setPropertyValue(ret, convertBasicType(newval, prop.getType()));
						}
					}
				}
			}
			catch(Exception e)
			{
				throw SUtil.throwUnchecked(e);
			}
		}
	}
	
	/**
	 *  Clone all properties of an object.
	 */
	protected void writeProperties(Object object, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, Traverser traverser, 
		ClassLoader targetcl, JsonWriteContext wr, IBeanIntrospector intro, boolean first)
	{
		Class<?> clazz = object.getClass();
		
		Map<String, BeanProperty> props = intro.getBeanProperties(clazz, true, false);

		for(Iterator<String> it=props.keySet().iterator(); it.hasNext(); )
		{
			try
			{
				String name = (String)it.next();
				
				if(!wr.isPropertyExcluded(clazz, name))
				{	
					BeanProperty prop = (BeanProperty)props.get(name);
					
					if(prop.isReadable() && prop.isWritable())
					{
						Object val = prop.getPropertyValue(object);
						
//						if (val != null && val.getClass().toString().contains("jadex.bridge.ComponentIdentifier"))
//							System.out.println("Contains addresses: " + object.getClass() + " " + object);
						
						if(val!=null) 
						{
							if(!first)
								wr.write(",");
							first = false;
							wr.writeString(name);
							wr.write(":");
							
							traverser.doTraverse(val, prop.getType(), conversionprocessors, processors, converter, mode, targetcl, wr);
						}
					}
				}
			}
			catch(Exception e)
			{
				throw SUtil.throwUnchecked(e);
			}
		}
	}
	
	/**
	 *  Get the object that is returned.
	 */
	public Object getReturnObject(Object object, Class<?> clazz, ClassLoader targetcl)
	{
		Object ret = null;
		
		if(targetcl!=null)
			clazz = SReflect.classForName0(clazz.getName(), targetcl);
		
		Constructor<?> c;
		
		try
		{
			c	= clazz.getConstructor(new Class[0]);
		}
		catch(NoSuchMethodException nsme)
		{
			c	= clazz.getDeclaredConstructors()[0];
		}

		try
		{
			SAccess.setAccessible(c, true);
			Class<?>[] paramtypes = c.getParameterTypes();
			Object[] paramvalues = new Object[paramtypes.length];
			for(int i=0; i<paramtypes.length; i++)
			{
				if(paramtypes[i].equals(boolean.class))
				{
					paramvalues[i] = Boolean.FALSE;
				}
				else if(SReflect.isBasicType(paramtypes[i]))
				{
					paramvalues[i] = 0;
				}
			}
			ret = c.newInstance(paramvalues);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		return ret;
	}
}
