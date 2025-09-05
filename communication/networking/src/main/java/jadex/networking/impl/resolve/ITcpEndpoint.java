package jadex.networking.impl.resolve;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 *  Interface for TCP-based endpoints.
 */
public interface ITcpEndpoint extends IEndpoint
{
    /**
     *  Gets the address used to connect using TCP.
     *  @return The address used to connect using TCP.
     */
    public InetAddress getAddress();

    /**
     *  Gets the port used to connect using TCP.
     *  @return The port used to connect using TCP.
     */
    public int getPort();
}
