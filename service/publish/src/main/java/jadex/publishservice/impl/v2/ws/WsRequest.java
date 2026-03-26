package jadex.publishservice.impl.v2.ws;

import jakarta.websocket.Session;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;

public class WsRequest extends Request
{
    protected String callid;

    public WsRequest(String callid, Session session)
    {
        super(session);
        this.callid = callid;
    }

    @Override
    public String extractSessionId()
    {
        return ((Session)getRawRequest()).getId();
    }

    @Override
    public String extractCallId()
    {
        return this.callid;
    }

    public TransportType extractTransportType()
    {
        return TransportType.WS;
    }
}