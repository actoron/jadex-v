package jadex.networking.impl.resolve;

import jadex.common.SUtil;
import jadex.networking.impl.NetworkFeature;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 *  Address for a TCP endpoint using IPv4.
 */
public class TcpV4Endpoint implements ITcpEndpoint
{
    /** The IPv4 address used to connect using TCP. */
    public Inet4Address address;

    /** The port used to connect with TCP. */
    public int port;

    /**
     *  Creates a new TCP Endpoint.
     *  Bean constructor.
     */
    public TcpV4Endpoint()
    {
    }

    /**
     *  Creates a new TCP Endpoint.
     *  @param address IP address used for the endpoint.
     *  @param port Port used for the endpoint.
     */
    public TcpV4Endpoint(Inet4Address address)
    {
        this.address = address;
        this.port = NetworkFeature.DEFAULT_TCP_PORT;
    }

    /**
     *  Creates a new TCP Endpoint.
     *  @param address IP address used for the endpoint.
     *  @param port Port used for the endpoint.
     */
    public TcpV4Endpoint(String address)
    {
        Inet4Address v4addr = null;
        try
        {
            v4addr = (Inet4Address) InetAddress.getByName(address);
        }
        catch (UnknownHostException e)
        {
            throw SUtil.throwUnchecked(e);
        }
        this.address = v4addr;
        this.port = NetworkFeature.DEFAULT_TCP_PORT;
    }

    /**
     *  Gets the address used to connect using TCP.
     *  @return The address used to connect using TCP.
     */
    public Inet4Address getAddress()
    {
        return address;
    }

    /**
     *  Sets the address used to connect using TCP.
     *  @param address The address used to connect using TCP.
     */
    public TcpV4Endpoint setAddress(Inet4Address address)
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
    public TcpV4Endpoint setPort(int port)
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
        if (obj instanceof TcpV4Endpoint)
        {
            TcpV4Endpoint other = (TcpV4Endpoint) obj;
            return address.equals(other.address) && port == other.port;
        }
        return false;
    }
}
