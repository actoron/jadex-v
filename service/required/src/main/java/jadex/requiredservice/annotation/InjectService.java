package jadex.requiredservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  "Subclass" of Inject annotation to be more specific about what should be injected.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectService
{
	// TODO: required service settings (scope, tags, etc)
}
