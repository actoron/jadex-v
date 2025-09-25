package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  A goal is succeeded and plan processing is stopped when the 'condition' returns a non-zero result value.
 *  The result value is stored as goal result.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GoalQueryCondition
{
	/**
	 *  The beliefs this condition should react to.
	 */
	public String[] beliefs() default {};
	
	/**
	 *  The parameters this condition should react to.
	 */
	public String[] parameters() default {};
	
//	/**
//	 *  The events this condition should react to.
//	 */
//	public RawEvent[] rawevents() default {};
}
