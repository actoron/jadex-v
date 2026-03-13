package jadex.publishservice.impl.v2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ParameterInfo
{
    public String name() default "";
    public String description() default "";
    public boolean required() default true;
}
