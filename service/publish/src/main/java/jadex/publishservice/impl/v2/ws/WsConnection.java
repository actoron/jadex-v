package jadex.publishservice.impl.v2.ws;

import jakarta.websocket.Session;
import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.Message;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportType;

public class WsConnection extends Connection
{
    protected Session session;

    public WsConnection(String id, Request request)
    {
        super(id, TransportType.WS);
        this.session = session;
    }

    @Override
    public boolean send(Message message)
    {
        Object payload = message.getResult().getPayload();

        try
        {
            session.getAsyncRemote().sendText(String.valueOf(payload));
            markAlive();
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    @Override
    public void terminate()
    {
        try
        {
            session.close();
        }
        catch(Exception e)
        {
        }
    }

    public Session getSession()
    {
        return session;
    }
}
