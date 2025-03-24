package jadex.model;

import jadex.core.IComponentFeature;
import jadex.model.modelinfo.IModelInfo;

public interface IModelFeature extends IComponentFeature
{
	public IModelInfo getModel();
}
