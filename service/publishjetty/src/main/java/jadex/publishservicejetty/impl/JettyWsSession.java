package jadex.publishservicejetty.impl;

import java.util.UUID;

import org.eclipse.jetty.websocket.api.Session;

import jadex.common.SUtil;
import jadex.publishservice.impl.v2.ws.IWsSession;

public class JettyWsSession implements IWsSession    
{
    protected Session session;

    protected final String id = UUID.randomUUID().toString();

    public JettyWsSession(Session session) 
    {
        this.session = session;
    }

    @Override
    public String getId() 
    {
        return id;
    }

    @Override
    public void sendText(String message)
    {
        try
        {
            session.getRemote().sendString(message);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() 
    {
        try
        {
            session.close();     
        }
        catch(Exception e)
        {
            SUtil.rethrowAsUnchecked(e);
        }
    }
}
