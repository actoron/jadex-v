package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Define external classes as goals of an agent or capability.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Goals
{
	/**
	 *  The goals.
	 */
	public Goal[] value() default {};
}
