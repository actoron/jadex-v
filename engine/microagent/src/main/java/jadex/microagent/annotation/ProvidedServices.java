package jadex.microagent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  The provided services annotation.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProvidedServices
{
	/**
	 *  The provided services.
	 */
	public ProvidedService[] value();
	
	/**
	 *  Replace content of the base classes.
	 */
	public boolean replace() default false;
}
