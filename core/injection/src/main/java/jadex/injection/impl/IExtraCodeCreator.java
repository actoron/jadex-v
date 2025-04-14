package jadex.injection.impl;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 *  Create handles for executing extra code, e.g. on start.
 */
@FunctionalInterface
public interface IExtraCodeCreator
{
	/**
	 *  Handle the extra code at model time.
	 *  
	 *  @param pojotypes	The type of the component pojo and potential subobject pojos.
	 *  					The extra code is for the last pojo in the list.
	 *  
	 *  @param contextfetchers	The context specific value fetchers.
	 *  
	 *  @return A handle for extra code or null, when this creator doesn't match.
	 */
	public List<IInjectionHandle>	getExtraCode(List<Class<?>> pojotypes, Map<Class<? extends Annotation>,List<IValueFetcherCreator>> contextfetchers);
}
