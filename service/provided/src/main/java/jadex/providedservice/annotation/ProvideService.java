package jadex.providedservice.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jadex.providedservice.ServiceScope;

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
	/**
	 *  The service type.
	 *  @return The service type.
	 */
	Class<?> type() default Object.class;
	
	/**
	 *  The service scope.
	 *  @return The service scope.
	 */
	ServiceScope scope() default ServiceScope.VM;
	
	/**
	 *  The service tags.
	 *  @return The service tags.
	 */
	String[] tags() default {};
	
}
