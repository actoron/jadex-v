package jadex.networking.impl.resolve;

import java.util.Set;

public interface IResolver
{
    /**
     *  Resolves the endpoints for a given host.
     *
     *  @param host The host.
     *  @return The endpoints that can be used to communicate with the host.
     */
    public Set<IEndpoint> resolve(String host);
}
