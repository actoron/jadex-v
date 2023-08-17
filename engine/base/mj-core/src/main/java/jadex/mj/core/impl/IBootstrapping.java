package jadex.mj.core.impl;

import java.util.function.Supplier;

import jadex.mj.core.MjComponent;

/**
 *  A feature provider may implement this interface to execute code before
 *  or immediately after the creation of the component with all features.
 */
public interface IBootstrapping
{
	public <T extends MjComponent> T	bootstrap(Class<T> type, Supplier<T> creator);
}
