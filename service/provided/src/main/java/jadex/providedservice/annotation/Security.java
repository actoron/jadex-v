package jadex.providedservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Set the roles that would allow access to a service interface or service method.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Security
{
	public static String UNRESTRICTED = "unrestricted";
	
	//-------- properties --------
	
	/**
	 *  Use predefined role: see constants unrestricted, default and admin.
	 *  Custom role(s): Allow only authentication secrets (e.g. network or platform key)
	 *  that are locally given at least one of the requested roles.
	 */
	public String[] roles();
}
