package jadex.common.transformation.traverser;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.Traverser.MODE;

/**
 *  No need to process immutable records.
 */
public class RecordProcessor implements ITraverseProcessor
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
		return object.getClass().isRecord();
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
		// Record doesn't adhere to bean spec, grrr.
		Class<?> clazz = SReflect.getClass(type);
		Field[]	fields	= clazz.getDeclaredFields();
		List<Object>	args	= new ArrayList<Object>(fields.length);
		List<Class<?>>	types	= new ArrayList<Class<?>>(fields.length);
		try
		{
			for(Field f: fields)
			{
				f.setAccessible(true);
				Object val	= MethodHandles.lookup().unreflectGetter(f).invoke(object);
				args.add(traverser.doTraverse(val, f.getType(), conversionprocessors, processors, converter, mode, targetcl, context));
				types.add(f.getType());
			}
			
			if(targetcl!=null)
				clazz	= SReflect.findClass(clazz.getName(), null, targetcl);
			
			Constructor<?>	c	= clazz.getConstructor(types.toArray(new Class[types.size()]));
			object	= MethodHandles.lookup().unreflectConstructor(c).invokeWithArguments(args);
		}
		catch(Throwable t)
		{
			SUtil.throwUnchecked(t);
		}

		return object;
	}
}
