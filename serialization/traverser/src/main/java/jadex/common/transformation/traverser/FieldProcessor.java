package jadex.common.transformation.traverser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;

import jadex.common.SAccess;
import jadex.common.SReflect;
import jadex.common.SUtil;
import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.Traverser.MODE;

/**
 *  Processor that traverses all fields of an object.
 */
class FieldProcessor implements ITraverseProcessor
{
	/**
	 *  Create a new field processor.
	 */
	public FieldProcessor()
	{
	}
	
	/**
	 *  Test if the processor is applicable.
	 *  @param object The object.
	 *  @param targetcl	If not null, the traverser should make sure that the result object is compatible with the class loader,
	 *    e.g. by cloning the object using the class loaded from the target class loader.
	 *  @return True, if is applicable. 
	 */
	public boolean isApplicable(Object object, Type type, ClassLoader targetcl, Object context)
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
	public Object process(Object object, Type type, Traverser traverser, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, ClassLoader targetcl, Object context)
	{
//		System.out.println("fp: "+object);
		Class<?> clazz = SReflect.getClass(type);
		Object ret = getReturnObject(object, clazz, targetcl, context);
		TraversedObjectsContext.put(context, object, ret);
		
		try
		{
//			System.out.println("cloned: "+object.getClass());
//			ret = object.getClass().newInstance();
			
			traverseFields(object, conversionprocessors, processors, converter, mode, traverser, targetcl, ret, context);
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
		
		return ret;
	}
	
	/**
	 *  Clone all fields of an object.
	 */
	protected void traverseFields(Object object, List<ITraverseProcessor> conversionprocessors, List<ITraverseProcessor> processors, IStringConverter converter, MODE mode, Traverser traverser, ClassLoader targetcl, Object ret, Object context)
	{
		Class clazz = object.getClass();
			
		while(clazz!=null && clazz!=Object.class) 
		{
			// Get all declared fields (public, protected and private)
			
			Field[] fields = clazz.getDeclaredFields();
			for(int i=0; i<fields.length; i++) 
			{
				if(!Modifier.isStatic(fields[i].getModifiers())) 
				{
					SAccess.setAccessible(fields[i], true);
					Object val = null;
					try
					{
						val = fields[i].get(object);
						if(val!=null) 
						{
							Object newval = traverser.doTraverse(val, fields[i].getType(), conversionprocessors, processors, converter, mode, targetcl, context);
							if(SCloner.isCloneContext(context) || val!=newval)
								fields[i].set(ret, newval);
						}
					}
					catch(Exception e)
					{
						throw SUtil.throwUnchecked(e);
					}
				}
			}
			
			clazz = clazz.getSuperclass();
		}
	}
	
	/**
	 *  Get the object that is returned.
	 */
	public Object getReturnObject(Object object, Class clazz, ClassLoader targetcl, Object context)
	{
		Object ret = object;
		if(SCloner.isCloneContext(context) || targetcl!=null && !clazz.equals(SReflect.classForName0(clazz.getName(), targetcl)))
		{
			if(targetcl!=null)
				clazz	= SReflect.classForName0(clazz.getName(), targetcl);
			
			try
			{
				ret = clazz.newInstance();
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		return ret;
	}
}