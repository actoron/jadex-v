package jadex.mj.feature.nfproperties.impl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jadex.mj.core.annotation.NameValue;
import jadex.mj.feature.nfproperties.impl.INFProperty;

/**
 *  Non-functional property annotation.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NFProperty
{
	/**
	 *  The name of the property.
	 */
	public String name() default "";
	
	/**
	 *  The type of the property.
	 */
	public Class<? extends INFProperty> value();
	
	/**
	 *  The init parameter values.
	 */
	public NameValue[] parameters() default {};
}
