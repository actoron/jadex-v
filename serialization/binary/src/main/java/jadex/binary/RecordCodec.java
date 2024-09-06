package jadex.binary;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.annotations.Classname;
import jadex.common.transformation.traverser.ITraverseProcessor;
import jadex.common.transformation.traverser.Traverser;
import jadex.common.transformation.traverser.Traverser.MODE;

/**
 *  Record processor for reading json objects.
 */
public class RecordCodec extends AbstractCodec
{
	/**
	 *  Tests if the decoder can decode the class.
	 *  @param clazz The class.
	 *  @return True, if the decoder can decode this class.
	 */
	public boolean isApplicable(Class<?> clazz)
	{
		return clazz.isRecord();
	}
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Class<?> clazz, boolean clone, ClassLoader targetcl)
	{
		return clazz!=null && clazz.isRecord();
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
		Object ret = null;
		
		boolean isanonclass = context.readBoolean();
		if(isanonclass)
		{
			String correctcl = context.readString();
			
			Classname cl = BeanCodec.getAnonClassName(clazz);
			if (cl == null || (!correctcl.equals(cl.value())))
			{
				clazz = BeanCodec.findCorrectInnerClass(0, SReflect.getClassName(clazz), correctcl, context.getClassloader());
			}
		}
		
		RecordComponent[] rcs = clazz.getRecordComponents();
		Class<?>[] rctypes = Arrays.stream(rcs).map((rc) -> rc.getType()).toArray(Class[]::new);
		Constructor<?> c = null;
		try
		{
			c = clazz.getConstructor(rctypes);
			
			Object[] subobjects = new Object[rcs.length];
			for (int i = 0; i < subobjects.length; ++i)
			{
				subobjects[i] = SBinarySerializer.decodeObject(context);
			}
			
			ret = c.newInstance(subobjects);
			
		}
		catch (Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
		
		return ret;
	}

	/**
	 *  Encode the object.
	 */
	public Object encode(Object object, Class<?> clazz, List<ITraverseProcessor> preprocessors,
			List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, Traverser traverser,
			ClassLoader targetcl, IEncodingContext ec)
	{
		if (!ec.getNonInnerClassCache().contains(clazz))
		{
			if (clazz != null && clazz.isAnonymousClass())
			{
				// Flag class is inner class.
				ec.writeBoolean(true);
				
				Classname cn = BeanCodec.getAnonClassName(clazz);
				
				if(cn == null)
				{
					String msg = String.valueOf(clazz);
					msg += " methods: ";
					for(Method m : clazz.getDeclaredMethods())
						msg += " "+m.getName();
					msg += " fields: ";
					for(Field f : clazz.getDeclaredFields())
						msg += " "+f.getName();
					throw new RuntimeException("Anonymous Class without Classname identifier not supported: " + msg);
				}
				
				ec.writeString(cn.value());
			}
			else
			{
				ec.writeBoolean(false);
				ec.getNonInnerClassCache().add(clazz);
			}
		}
		else
		{
			ec.writeBoolean(false);
		}
		
		RecordComponent[] rcs = clazz.getRecordComponents();
		
		for (int i = 0; i < rcs.length; ++i)
		{
			try
			{
				Object subobject = rcs[i].getAccessor().invoke(object);
				traverser.doTraverse(subobject, rcs[i].getType(), preprocessors, processors, converter, mode, ec.getClassLoader(), ec);
			}
			catch (Exception e)
			{
				throw SUtil.throwUnchecked(e);
			}
		}
		return object;
	}
}
