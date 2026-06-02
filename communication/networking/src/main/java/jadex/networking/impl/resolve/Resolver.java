package jadex.networking.impl.resolve;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  Meta-Resolver that joins multiple resolvers together.
 */
public class Resolver implements IResolver
{
    /** The resolvers */
    private Map<Class<? extends IResolver>, IResolver> resolvers = new HashMap<>();

    public Resolver()
    {
        resolvers.put(CatalogResolver.class, new CatalogResolver());
        resolvers.put(DnsResolver.class, new DnsResolver());
    }

    /**
     *  Resolves the endpoints for a given host.
     *
     *  @param host The host.
     *  @return The endpoints that can be used to communicate with the host.
     */
    public Set<IEndpoint> resolve(String host)
    {
        Set<IEndpoint> ret = new HashSet<>();
        for (IResolver resolver : resolvers.values())
            ret.addAll(resolver.resolve(host));
        return ret;
    }

    /**
     *  Adds a new resolver.
     *  @param resolver New resolver
     */
    public void addResolver(IResolver resolver)
    {
        resolvers.put(resolver.getClass(), resolver);
    }

    /**
     *  Removes a resolver.
     *  @param resolver The resolver
     */
    public void removeResolver(IResolver resolver)
    {
        resolvers.remove(resolver.getClass());
    }

    /**
     *  Gets a resolver by type.
     *  @param type The resolver type.
     *  @return The resolver.
     */
    @SuppressWarnings("unchecked")
    public <T extends IResolver> T getResolver(Class<T> type)
    {
        return (T) resolvers.get(type);
    }
}
