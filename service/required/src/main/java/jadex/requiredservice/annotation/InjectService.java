package jadex.requiredservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  "Subclass" of Inject annotation to be more specific about what should be injected.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface InjectService
{
	// TODO: more required service settings (scope, tags, etc)
	
	/** search=one-off, query=persistent, default=search for field/parameter vs. default=query for method. */
	public enum Mode{DEFAULT, SEARCH, QUERY}

	Mode mode() default Mode.DEFAULT;
}
