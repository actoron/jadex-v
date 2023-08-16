package jadex.enginecore.service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jadex.enginecore.service.component.multiinvoke.FlattenMultiplexCollector;
import jadex.enginecore.service.component.multiinvoke.IMultiplexCollector;

/**
 *  
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MultiplexCollector
{
	/**
	 *  The multiplex distributor class.
	 */
	public Class<? extends IMultiplexCollector> value() default FlattenMultiplexCollector.class;
}
