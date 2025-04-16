package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *	Plans are started for events or goals.
 *  Can be modeled as methods of the agent or as separate class with extra body method.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Plans.class)
public @interface Plan
{
	/**
	 *  The trigger, i.e. reasons to start the plan.
	 */
	public Trigger trigger() default @Trigger();

//	/**
//	 *  The waitqueue.
//	 */
//	public Trigger waitqueue() default @Trigger();
//	
//	/**
//	 *  The plan priority. 
//	 */
//	public int priority() default 0;
	
//	/**
//	 *  The body (if external plan class).
//	 */
//	public Body body() default @Body();

	/**
	 *  The plan implementation class, when plan is declared on the agent.
	 *  Unused for inner class plans.
	 */
	public Class<?> impl() default Object.class;
}
