package jadex.bdi.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.Component;

/**
 *  Component subtype to select BDI features only for BDI agents.
 */
public class BDIAgent extends Component
{
	/**
	 *  Create the component.
	 */
	public BDIAgent(Object pojo, ComponentIdentifier id, Application app)
	{
		super(pojo, id, app);
	}
}
