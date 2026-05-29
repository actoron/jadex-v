package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Declare external plans used by the agent,
 *  i.e. plans defined in separate classes (not as inner classes).
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Plans
{
	/**
	 *  The plans.
	 */
	public Plan[] value() default {};
}
