package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlanContextCondition
{
	/**
	 *  The events this condition should react to.
	 */
	public String[] beliefs() default {};
	
	/**
	 *  The events this condition should react to.
	 */
	public RawEvent[] rawevents() default {};
}
