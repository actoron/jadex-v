package jadex.providedservice.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  The value of a field marked with this annotation will be provided as service.
 *  A method marked with this annotation will get called to get and provide a service implementation.
 *  
 *  It is also possible to annotate a class that implements a service interface.
 *  
 *  The annotation can be used to specify detailed settings for the service.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProvideService
{
	// TODO: settings (scope, tags, etc.)
}
