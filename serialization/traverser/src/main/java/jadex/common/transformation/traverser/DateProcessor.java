package jadex.common.transformation.traverser;


import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

import jadex.common.transformation.IStringConverter;
import jadex.common.transformation.traverser.Traverser.MODE;

/**
 *  Allows processing java.util.Date.
 */
public class DateProcessor implements ITraverseProcessor
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
		return object instanceof Date;
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
		return SCloner.isCloneContext(context)? new Date(((Date)object).getTime()): object;
	}
}

