package jadex.networking.impl.resolve;


import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import jadex.common.SUtil;
import jadex.networking.impl.NetworkFeature;

/**
 *  Address for a TCP endpoint using IPv6.
 */
public class TcpV6Endpoint implements ITcpEndpoint
{
    /** The IPv6 address used to connect using TCP. */
    public Inet6Address address;

    /** The port used to connect with TCP. */
    public int port;

    /**
     *  Creates a new TCP Endpoint.
     *  Bean constructor.
     */
    public TcpV6Endpoint()
    {
    }

    /**
     *  Creates a new TCP Endpoint.
     *  @param address IP address used for the endpoint.
     *  @param port Port used for the endpoint.
     */
    public TcpV6Endpoint(Inet6Address address)
    {
        this.address = address;
        if (address == null)
            throw new RuntimeException("Host cannot be null: " + address);
        this.port = NetworkFeature.DEFAULT_TCP_PORT;
    }

    /**
     *  Creates a new TCP Endpoint.
     *  @param address IP address used for the endpoint.
     *  @param port Port used for the endpoint.
     */
    public TcpV6Endpoint(String address)
    {
        Inet6Address v6addr = null;
        try
        {
            v6addr = (Inet6Address) InetAddress.getByName(address);
        }
        catch (UnknownHostException e)
        {
            throw SUtil.throwUnchecked(e);
        }
        if (v6addr == null)
            throw new RuntimeException("Host cannot be resolved: " + address);
        this.address = v6addr;
        this.port = NetworkFeature.DEFAULT_TCP_PORT;
    }

    /**
     *  Gets the address used to connect using TCP.
     *  @return The address used to connect using TCP.
     */
    public Inet6Address getAddress()
    {
        return address;
    }

    /**
     *  Sets the address used to connect using TCP.
     *  @param address The address used to connect using TCP.
     */
    public TcpV6Endpoint setAddress(Inet6Address address)
    {
        this.address = address;
        return this;
    }

    /**
     *  Gets the port used to connect using TCP.
     *  @return The port used to connect using TCP.
     */
    public int getPort()
    {
        return port;
    }

    /**
     *  Sets the port used to connect using TCP.
     *  @param port The port used to connect using TCP.
     */
    public TcpV6Endpoint setPort(int port)
    {
        this.port = port;
        return this;
    }

    /**
     *  Creates a hash code for the endpoint.
     *  @return The hash code.
     */
    public int hashCode()
    {
        int hash = port;
        hash = 31 * hash + address.hashCode();
        return hash;
    }

    /**
     *  Compares two endpoints.
     *  @param obj The other object.
     *  @return True, if the address and port match.
     */
    public boolean equals(Object obj)
    {
        if (obj instanceof TcpV6Endpoint)
        {
            TcpV6Endpoint other = (TcpV6Endpoint) obj;
            return address.equals(other.address) && port == other.port;
        }
        return false;
    }
}
