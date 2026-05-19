package jadex.publishservice.impl.v2.http;

import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.IConnectionFactory;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;

public class HttpConnectionFactory implements IConnectionFactory
{
    
    public boolean canHandle(Request req)
    { 
        return req.getTransportType() == TransportType.REST;
    }

    public Connection create(Request req)
    {
        return new HttpConnection(req.getCallId(), req);
    }
}
