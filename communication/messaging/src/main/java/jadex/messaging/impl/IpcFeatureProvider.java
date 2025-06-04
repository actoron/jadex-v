package jadex.messaging.impl;

import java.util.Collections;
import java.util.Set;

import jadex.core.IRuntimeFeature;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.core.impl.RuntimeFeatureProvider;
import jadex.messaging.IIpcFeature;
import jadex.messaging.ipc.IpcStreamHandler;

public class IpcFeatureProvider extends RuntimeFeatureProvider<IIpcFeature>
{
	/**
	 *  Get the type of the feature used for accessing.
	 */
	public Class<IIpcFeature> getFeatureType()
	{
		return IIpcFeature.class;
	}
	
	/**
	 *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
	 *  @param self	The component to which the feature belongs.
	 *  @return	The feature instance.
	 */
	public IIpcFeature createFeatureInstance()
	{
		IpcStreamHandler streamhandler = new IpcStreamHandler(GlobalProcessIdentifier.getSelf());
		streamhandler.open();
		
		return streamhandler;
	}
	
	/**
	 *  Get the feature dependencies, i.e. features that are required to be available
	 *  before this one can be requested.
	 *  @return The dependencies.
	 */
	public Set<Class<? extends IRuntimeFeature>> getDependencies()
	{
		return Collections.emptySet();
	}
}
