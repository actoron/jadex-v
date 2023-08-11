package jadex.mj.core;

import jadex.common.IValueFetcher;

public interface IValueFetcherProvider 
{
	/**
	 *  The feature can inject parameters for expression evaluation
	 *  by providing an optional value fetcher. The fetch order is the reverse
	 *  init order, i.e., later features can override values from earlier features.
	 */
	public IValueFetcher getValueFetcher();
}
