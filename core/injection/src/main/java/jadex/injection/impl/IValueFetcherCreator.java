package jadex.injection.impl;

import java.util.List;

/**
 *  Create a handler for fetching the value for a field.
 */
@FunctionalInterface
public interface IValueFetcherCreator
{
	/**
	 *  Handle the value extraction at model time.
	 *  
	 *  @param pojotypes	The type of the component pojo and potential subobject pojos.
	 *  					The fetcher is for the last pojo in the list.
	 *  @param value_type	The type of the value to be injected.
	 *  
	 *  @return A value fetcher or null, when this creator doesn't match.
	 */
	public IValueFetcher	getValueFetcher(List<Class<?>> pojotypes, Class<?> valuetype);
}
