package jadex.publishservice.impl.v2.http;

import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;
import jadex.publishservice.impl.v2.sse.SseConnection;
import jakarta.servlet.http.HttpServletRequest;

public class HttpConnectionFactory 
{
    public Connection create(Request req)
    {
        TransportType type = req.getTransportType();


        if(req instanceof HttpServletRequest)
        {
            HttpServletRequest http = (HttpServletRequest)req.getRawRequest();
        }
        else
        {
            System.out.println("No connection creation based on non-http requests");
        }

        return switch(type)
        {
            case SSE      -> new SseConnection(req.getCallId(), req);
            //case LONGPOLL -> new LongPollConnection(req.getCallId(), req);
            case REST     -> new HttpConnection(req.getCallId(), req);
            default       -> new HttpConnection(req.getCallId(), req);
        };
    }
}
