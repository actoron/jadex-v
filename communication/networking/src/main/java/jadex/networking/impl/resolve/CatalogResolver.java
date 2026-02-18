package jadex.networking.impl.resolve;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.core.impl.GlobalProcessIdentifier;

import java.util.*;

/**
 *  Resolver using a manually-defined catalog for resolving endpoints.
 */
public class CatalogResolver implements IResolver
{
    /** The catalog, maps from host name to List of endpoints. */
    private RwMapWrapper<String, Set<IEndpoint>> catalog;

    /**
     *  Creates a new, empty catalog.
     */
    public CatalogResolver()
    {
        catalog = new RwMapWrapper<>(new HashMap<>());
    }

    /**
     *  Adds an endpoint for a host.
     *
     *  @param host Host that can be reached using the endpoint.
     *  @param endpoint The endpoint.
     */
    public void addEndpoint(String host, IEndpoint endpoint)
    {
        try (IAutoLock l = catalog.writeLock())
        {
            Set<IEndpoint> endpoints = catalog.get(host);
            if (endpoints == null)
            {
                endpoints = new HashSet<>();
                catalog.put(host,endpoints);
            }
            endpoints.add(endpoint);
        }
    }

    /**
     *  Removes an endpoint for a host.
     *
     *  @param host Host that can be reached using the endpoint.
     *  @param endpoint The endpoint to be removed.
     */
    public boolean removeEndpoint(String host, IEndpoint endpoint)
    {
        try (IAutoLock l = catalog.writeLock())
        {
            Set<IEndpoint> endpoints = catalog.get(host);
            if (endpoints == null)
                return false;
            return endpoints.remove(endpoint);
        }
    }

    /**
     *  Resolves the endpoints for a given host.
     *
     *  @param host The host.
     *  @return The endpoints that can be used to communicate with the host.
     */
    public Set<IEndpoint> resolve(String host)
    {
        try (IAutoLock l = catalog.readLock())
        {
            Set<IEndpoint> endpoints = catalog.get(host);
            if (endpoints == null)
                return Collections.emptySet();
            return new HashSet<>(endpoints);
        }
    }
}
