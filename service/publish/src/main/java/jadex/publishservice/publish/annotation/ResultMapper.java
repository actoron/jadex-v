package jadex.publishservice.publish.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jadex.model.annotation.Value;

/**
 *  Result mapper annotation.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResultMapper
{
	/**
	 *  The mapper.
	 */
	public Value value();
}

