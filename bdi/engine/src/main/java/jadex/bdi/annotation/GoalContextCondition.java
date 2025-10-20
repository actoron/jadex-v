package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Add a context condition to a goal.
 *  The method should return a boolean value.
 *  Context true means the goal is either option or active
 *  (i.e. open for deliberation),
 *  while context false means the goal is suspended.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GoalContextCondition
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