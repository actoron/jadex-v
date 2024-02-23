package jadex.bdi.model;

import jadex.model.modelinfo.IModelInfo;

/**
 *  Common interface for micro- and xml-based BDI agent models.
 */
public interface IBDIModel 
{
	/**
	 *  Get the component model.
	 */
	public IModelInfo	getModelInfo();
	
	/**
	 *  Get the mcapa.
	 *  @return The mcapa.
	 */
	public MCapability getCapability();
}
