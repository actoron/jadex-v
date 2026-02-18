package jadex.injection.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  A field marked with this annotation will be provided as result.
 *  A method marked with this annotation will get called to provide a result.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProvideResult
{
	/**
	 *  Use this name instead of the field/method name (default).
	 */
	String value() default "";
}
