package jadex.messaging;

public interface ICatalogResolver
{
    /**
     *  Adds an endpoint for a host.
     *
     *  @param host Host that can be reached using the endpoint.
     *  @param ipaddress The IP address.
     */
    public void addEndpoint(String host, String ipaddress);
}
