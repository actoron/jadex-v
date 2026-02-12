package jadex.bpmn.runtime;

import jadex.core.IComponentFeature;
import jadex.model.modelinfo.IModelInfo;

/**
 *  User-accessible methods for BPMN components.
 */
public interface IBpmnComponentFeature extends IComponentFeature
{
	/**
	 *  Get the model.
	 */
	public IModelInfo getModel();
}
