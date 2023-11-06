package jadex.bdi.runtime;

import jadex.bdi.model.MElement;

/**
 *	Base interface for all runtime elements.
 */
public interface IElement
{
	//-------- element methods ---------

	/**
	 *  Get the name.
	 *  @return The name.
	 * /
	public String getName();*/

	/**
	 *  Get the model element.
	 *  @return The model element.
	 */
	public MElement getModelElement();
	
	/**
	 *  Get the id.
	 *  @return The id.
	 */
	public String getId();
	
	/**
	 *  Get the element count.
	 *  @return The element count.
	 */
	public long getCount();
}
