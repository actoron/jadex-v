package jadex.injection.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  A field marked with this annotation will be provided outside the component (e.g. as result or service).
 *  A method marked with this annotation will get called to provide a value outside the component (e.g. result or service).
 *  "Subtypes" of the annotation allow the programmer to be more specific (e.g. ProvideResult vs. ProvideService).
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Provide
{
}
