package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 *  Create a handler for fetching the value for a field or parameter.
 */
@FunctionalInterface
public interface IValueFetcherCreator
{
	/**
	 *  Handle the value extraction at model time.
	 *  
	 *  @param pojotypes	The type of the component pojo and potential subobject pojos.
	 *  					The fetcher is for the last pojo in the list.
	 *  
	 *  @param valuetype	The type of the value to be injected.
	 *  
	 *  @param annotation	The annotation on the field.
	 *  
	 *  @return A value fetcher or null, when this creator doesn't match.
	 */
	public IInjectionHandle	getValueFetcher(List<Class<?>> pojotypes, Type valuetype, Annotation annotation);
}
