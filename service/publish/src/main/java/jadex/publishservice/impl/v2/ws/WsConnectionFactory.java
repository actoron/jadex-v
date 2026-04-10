package jadex.publishservice.impl.v2.ws;

import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.IConnectionFactory;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;

public class WsConnectionFactory implements IConnectionFactory
{
    public boolean canHandle(Request req)
    { 
        return req.getTransportType() == TransportType.WS;
    }

    public Connection create(Request req)
    {
        IWsSession session = ((WsRequest)req).getSession();
        return new WsConnection(req.getSessionId(), req, session);
    }
}

