package jadex.enginecore.component.impl;

import jadex.enginecore.service.types.cms.IComponentDescription;
import jadex.future.IFuture;

/**
 *  Allows a component to have subcomponents.
 */
public interface IInternalSubcomponentsFeature
{
	/**
	 *  Called, when a subcomponent has been created.
	 */
	public IFuture<Void> componentCreated(IComponentDescription desc);
	
	/**
	 *  Called, when a subcomponent has been removed.
	 */
	public IFuture<Void> componentRemoved(IComponentDescription desc);
}
