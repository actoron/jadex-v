package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 *  Configurations annotation. 
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BDIConfigurations
{
	/**
	 *  The configurations.
	 */
	public BDIConfiguration[] value() default {};
	
	/**
	 *  Replace content of the base classes.
	 */
	public boolean replace() default false;
}
