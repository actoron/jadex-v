package jadex.injection.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  An annotated field generates events on value changes.
 *  Supports List, Set, Map, Bean (with property change listeners), Dyn, Val.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicValue
{
}
