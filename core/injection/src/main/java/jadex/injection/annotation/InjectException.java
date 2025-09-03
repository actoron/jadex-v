package jadex.injection.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  A method marked with this annotation will get called for each matching exception.
 *  A parameter of type Exception or a sub type is required.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectException
{
	/**
	 *  The exception type to match.
	 *  If false, also sub types are matched.
	 *  Default is false.
	 */
	boolean exactmatch() default false;
}
