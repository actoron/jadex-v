package jadex.mj.publishservice.publish.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  The name (for referencing/overriding).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Publish
{
	/**
	 *  The publishing id, e.g. url or name.
	 */
	public String publishid();
	
	/**
	 *  The publishing type, e.g. web service.
	 */
	public String publishtype() default "";
	
	/**
	 *  Target interface of the service to be published or the name of the provided service.
	 */
	public String publishtaget() default "";
	
	/**
	 *  The scope user to find the publish service.
	 * /
	public ServiceScope publishscope() default ServiceScope.PLATFORM;
	
	/**
	 *  Flag if the service should be published to multiple locations.
	 * /
	public boolean multi() default false;
	
	/**
	 * The mapping information (e.g. annotated interface). 
	 */ 
	public Class<?> mapping() default Object.class;
	
	/**
	 *  Additional mapping properties. 
	 * /
	public NameValue[] properties() default {};*/
}
