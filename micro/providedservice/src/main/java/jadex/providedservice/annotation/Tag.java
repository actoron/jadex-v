package jadex.providedservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Repeatable(Tags.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Tag
{
	/**
	 *  The tags as strings or expression.
	 */
	public String value();

	/**
	 *  Condition to check if the value/tag should be included. 
	 * /
	public String include() default "";*/
}
