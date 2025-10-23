package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  A condition to be monitored while the plan is running.
 *  The plan is aborted when the condition become sinvalid.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlanContextCondition
{
//	/**
//	 *  The events this condition should react to.
//	 */
//	public String[] beliefs() default {};
//	
//	/**
//	 *  The events this condition should react to.
//	 */
//	public RawEvent[] rawevents() default {};
}
