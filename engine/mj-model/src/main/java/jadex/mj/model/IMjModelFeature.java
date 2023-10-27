package jadex.mj.model;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.mj.model.modelinfo.IModelInfo;

public interface IMjModelFeature
{
	/**
	 *  The feature can add objects for field or method injections
	 *  by providing an optional parameter guesser. The selection order is the reverse
	 *  init order, i.e., later features can override values from earlier features.
	 */
	public IParameterGuesser	getParameterGuesser();

	public IModelInfo getModel();

	public IValueFetcher getFetcher();
}
