package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  An annotated field generates events on value changes.
 *  Supports List, Set, Map, Bean (with property change listeners), Dyn, Val.
 */
@Target({ElementType.FIELD})//, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Belief
{
//	/**
//	 *  A dynamic belief reevaluated on every access.
//	 *  Relevant only for bytecode-enhanced field beliefs.
//	 */
//	public boolean dynamic() default false;
	
//	/**
//	 *  A dynamic belief is automatically updated when other beliefs change.
//	 *  Supported for Val beliefs.
//	 */
//	public String[] beliefs() default {};

//	/**
//	 *  The events this belief should react to.
//	 */
//	public RawEvent[] rawevents() default {};
	
	// Moved into Dyn object
//	/**
//	 *  An update rate > 0 causes the belief to be reevaluated periodically.
//	 *  Supported for Val beliefs.
//	 */
//	public long	updaterate() default 0;
}
