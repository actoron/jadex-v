package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 *  Creator for method injections.
 */
@FunctionalInterface
public interface IMethodInjectionCreator
{
	/**
	 *  Handle the injection code at model time.
	 *  
	 *  @param model	The injection model to access getters and helper methods.
	 *  @param method	The method to be invoked.
	 *  @param annotation	The annotation to be handled.
	 *  
	 *  @return An injection handle or null, when this creator doesn't match.
	 */
	public IInjectionHandle	getInjectionHandle(InjectionModel model,  Method method, Annotation annotation);
}
