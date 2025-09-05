package jadex.networking.impl.resolve;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  Meta-Resolver that joins multiple resolvers together.
 */
public class MultiResolver implements IResolver
{
    /** The resolvers */
    private Map<Class<? extends IResolver>, IResolver> resolvers = new HashMap<>();

    /** The singleton instance. */
    private static volatile MultiResolver instance;

    /**
     *  Gets the singleton.
     *  @return The singleton.
     */
    public static MultiResolver get()
    {
        if (instance == null)
        {
            synchronized (MultiResolver.class)
            {
                if (instance == null)
                {
                    instance = new MultiResolver();
                }
            }
        }
        return instance;
    }

    private MultiResolver()
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
        for (IResolver resolver : resolvers)
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
    public IResolver getResolveR(Class<? extends IResolver> type)
    {
        return resolvers.get(type);
    }
}
