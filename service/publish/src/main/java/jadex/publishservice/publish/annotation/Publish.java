package jadex.publishservice.publish.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jadex.publishservice.PublishType;

/**
 *  The name (for referencing/overriding).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Publish
{
	/**
	 *  Automapping flag, if true the methods are automatically mapped.
	 */
	public boolean automapping() default true;

	/**
	 *  The publishing id, e.g. url or name.
	 */
	public String publishid();
	
	/**
	 *  The publishing type, e.g. web service.
	 */
	public String publishtype() default "rest";//PublishType.REST.getId();
	
	/**
	 *  Target interface of the service to be published service.
	 *  Needed when publish is used directly in agent class.
	 */
	public Class<?> publishinterface() default Object.class;
	
	/**
	 *  Target publish name of the provided service.
	 */
	public String publishname() default "";
	
	/**
	 * The mapping information (e.g. annotated interface). 
	 */ 
	public Class<?> mapping() default Object.class;
	
	/* *
	 *  Additional mapping properties. 
	 * /
	public NameValue[] properties() default {};*/

	/* *
	 *  The scope user to find the publish service.
	 * /
	public ServiceScope publishscope() default ServiceScope.PLATFORM;*/
	
	/* *
	 *  Flag if the service should be published to multiple locations.
	 * /
	public boolean multi() default false;*/
}
