package jadex.injection.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  A field marked with this annotation will get the value injected before OnStart is called.
 *  A method marked with this annotation will get called after OnStart, whenever the required object (e.g. a service) is available.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject
{
}
