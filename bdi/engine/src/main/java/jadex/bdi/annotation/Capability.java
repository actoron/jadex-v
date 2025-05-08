package jadex.bdi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Marker for a sub-object to be used as BDI capability, i.e. enable beliefs, goals, plans etc.
 */
@Target({ElementType.FIELD/*, ElementType.TYPE*/})
@Retention(RetentionPolicy.RUNTIME)
public @interface Capability
{
//	/**
//	 *  Belief mappings from outer beliefs to inner abstract beliefs.
//	 */
//	public Mapping[]	beliefmapping() default {};
	
//	/**
//	 *  Flag to indicate pure Java code (not to be bytecode enhanced).
//	 */
//	public boolean	pure() default false;
}
