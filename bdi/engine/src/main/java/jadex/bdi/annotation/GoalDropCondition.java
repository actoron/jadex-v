package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Annotated to a method that should return a boolean value.
 *  When the return value is true, the goal is dropped.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GoalDropCondition
{
//	/**
//	 *  The events this condition should react to.
//	 */
//	public String[] beliefs() default {};
//	
//	/**
//	 *  The parameters this condition should react to.
//	 */
//	public String[] parameters() default {};
//	
//	/**
//	 *  The events this condition should react to.
//	 */
//	public RawEvent[] rawevents() default {};
}
