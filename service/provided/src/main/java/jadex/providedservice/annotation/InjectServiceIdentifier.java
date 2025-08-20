package jadex.providedservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Allow differentiating between multiple provided services when injecting fields/parameters.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectServiceIdentifier
{
	/**
	 *  The service type to inject.
	 *  If empty, the field type is used.
	 */
	Class<?> value() default Object.class;
}