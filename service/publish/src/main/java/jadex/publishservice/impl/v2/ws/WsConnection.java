package jadex.publishservice.impl.v2.ws;

import java.util.Set;

import com.eclipsesource.json.JsonObject;

import jadex.publishservice.impl.v2.Connection;
import jadex.publishservice.impl.v2.JsonMapper;
import jadex.publishservice.impl.v2.Message;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.TransportMode;
import jadex.publishservice.impl.v2.TransportType;

public class WsConnection extends Connection
{
    protected IWsSession session;

    public WsConnection(String id, Request request, IWsSession session)
    {
        super(id, TransportType.WS);
        this.session = session;
        supportedModes = Set.of(TransportMode.STREAM);
    }
    
    @Override
    public boolean send(Message message)
    {
        try
        {
            String msg = format(message);
            System.out.println("Sending message: "+msg);
            getSession().sendText(msg);
            markAlive();
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    protected String format(Message message)
    {
        JsonObject json = new JsonObject();

        json.add("callid", message.getRequest().getCallId());
        json.add("sessionid", getSession().getId());

        if(message.getResult().isFinished())
            json.add("finished", true);

        if(message.getResult().getMax()!=null)
            json.add("max", message.getResult().getMax());

        if(message.isError())
        {
            json.add("error", String.valueOf(message.getResult().getException()));
        }
        else
        {
            Object payload = message.getResult().getPayload();
            if(payload != null)
                json.add("result", JsonMapper.toJsonObject(payload));
        }

        return json.toString();
    }

    @Override
    public void terminate()
    {
        try
        {
            getSession().close();
        }
        catch(Exception e)
        {
        }
    }

    public IWsSession getSession()
    {
        return session;
    }
}
