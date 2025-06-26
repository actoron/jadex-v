package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 *  Creator for method injections.
 */
@FunctionalInterface
public interface IMethodInjectionCreator
{
	/**
	 *  Handle the injection code at model time.
	 *  
	 *  @param pojotypes	The type of the component pojo and potential subobject pojos.
	 *  					The fetcher is for the last pojo in the list.
	 *  @param method	The method to be invoked.
	 *  @param contextfetchers	The context specific value fetchers.
	 *  
	 *  @return An injection handle or null, when this creator doesn't match.
	 */
	public IInjectionHandle	getInjectionHandle(List<Class<?>> pojotypes, Method method,
		Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers);
}
