package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  If present, a goal does not fail/succeed but goes into a paused state
 *  after all plans have been tried.
 *  The condition then triggers a fresh processing of the paused goal.
 *  Has no effect on goals that are in process.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface GoalRecurCondition
{
	/**
	 *  The events this condition should react to.
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
