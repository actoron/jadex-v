package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Belief
{
	/**
	 *  A dynamic belief reevaluated on every access.
	 *  Relevant only for bytecode-enhanced field beliefs.
	 */
	public boolean dynamic() default false;
	
	/**
	 *  A dynamic belief is automatically updated when other beliefs change.
	 */
	public String[] beliefs() default {};
	
	/**
	 *  The events this belief should react to.
	 */
	public RawEvent[] rawevents() default {};
	
	/**
	 *  An update rate causes the belief to be reevaluated periodically.
	 */
	public long	updaterate() default 0;
}
