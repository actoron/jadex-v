package jadex.networking.impl.resolve;

import jadex.networking.impl.NetworkFeature;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 *  Resolver using DNS to "guess" possible endpoints.
 */
public class DnsResolver implements IResolver
{
    /**
     *  Creates a new, empty catalog.
     */
    public DnsResolver()
    {
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
        try
        {
            InetAddress[] addrs = InetAddress.getAllByName(host);

            for (InetAddress addr : addrs)
            {
                if (addr instanceof Inet4Address)
                    ret.add(new TcpV4Endpoint((Inet4Address) addr, NetworkFeature.DEFAULT_TCP_PORT));
            }
        }
        catch (UnknownHostException e)
        {
        }
    }
}
