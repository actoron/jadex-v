package jadex.bding.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.Component;

/**
 *  Component subtype to select BDI features only for BDI agents.
 */
public class BDINGAgent extends Component
{
	/**
	 *  Create the component.
	 */
	public BDINGAgent(Object pojo, ComponentIdentifier id, Application app)
	{
		super(pojo, id, app);
	}
	
	@Override
	public void doTerminate()
	{
		System.out.println("BDINGAgent.doTerminate() called");
	}
}
