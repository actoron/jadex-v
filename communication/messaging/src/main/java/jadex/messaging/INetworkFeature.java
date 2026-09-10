package jadex.messaging;

import jadex.core.ComponentIdentifier;
import jadex.core.IRuntimeFeature;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.IFuture;

/**
 *  Interface for the service providing network communication across hosts.
 */
public interface INetworkFeature extends IRuntimeFeature
{
    /**
     *  Sets TCP port to use instead of the default 5650.
     *  Warning: This can make it harder for remote Jadex instance to connect.
     *  @param tcpport The TCP port.
     */
    public void setTcpPort(int tcpport);

    /**
     *  Sets the IPv4 address to bind for incoming TCP connections.
     *  @param ipv4bindaddress The IPv4 address to bind for incoming TCP connections.
     */
    public void setIpv4BindAddress(String ipv4bindaddress);

    /**
     *  Sets the IPv6 address to bind for incoming TCP connections.
     *  @param ipv6bindaddress The IPv6 address to bind for incoming TCP connections.
     */
    public void setIpv6bindaddress(String ipv6bindaddress);

    /**
     *  Returns the catalog resolver for configuring fixed
     *  host -> IP resolving.
     * 
     *  @return The catalog resolver.
     */
    public ICatalogResolver getCatalogResolver();

    /**
	 *  Sends a message to a component using the network.
	 *  
     *  @param origin Origin of the message.
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public IFuture<Void> sendMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] message);
}