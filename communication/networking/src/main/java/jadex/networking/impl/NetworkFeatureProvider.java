package jadex.networking.impl;

import jadex.core.IRuntimeFeature;
import jadex.core.impl.RuntimeFeatureProvider;
import jadex.messaging.IIpcFeature;
import jadex.networking.INetworkFeature;

import java.util.*;

/**
 *  Provider for the network service.
 */
public class NetworkFeatureProvider extends RuntimeFeatureProvider<INetworkFeature>
{

    /**
     *  Create an instance of the feature. Can be a subclass or interface implementation of the feature type.
     *  @return	The feature instance.
     */
    public INetworkFeature createFeatureInstance()
    {
        NetworkFeature nwfeat = new NetworkFeature();
        return nwfeat;
    }

    /**
     *  Get the feature dependencies, i.e. features that are required to be available
     *  before this one can be requested.
     *  @return The dependencies.
     */
    public Set<Class<? extends IRuntimeFeature>> getDependencies()
    {
        HashSet<Class<? extends IRuntimeFeature>> ret = new HashSet<>();
        ret.add(IIpcFeature.class);
        return ret;
    }

    /**
     *  Determines if the feature is created immediately
     *  on component startup (false) or later on first access (true).
     *  @return	Defaults to false.
     */
    public boolean isLazyFeature()
    {
        return false;
    }

    /**
     *  Get the type of the feature used for accessing.
     */
    public Class<INetworkFeature> getFeatureType()
    {
        return INetworkFeature.class;
    }
}
