package jadex.publishservicejetty.impl;

import java.util.Collection;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException;

import jadex.providedservice.IService;
import jadex.publishservice.IRequestManager;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.RequestManagerFactory;
import jadex.publishservice.impl.v2.ws.IWsSession;
import jadex.publishservice.impl.v2.ws.WsRequest;

@WebSocket
public class WebSocketAdapter
{
    protected IWsSession session;

    protected WsServiceRegistry registry;

    public WebSocketAdapter(WsServiceRegistry registry)
    {
        System.out.println("WebSocketAdapter created");
        this.registry = registry;
    }

    @OnWebSocketConnect
    public void onConnect(Session session)
    {
        this.session = new JettyWsSession(session);
        //System.out.println("WS connected: " + session);
    }

    @OnWebSocketMessage
    public void onMessage(String message)
    {
        //System.out.println("WS message: " + message);

        try
        {
            WsRequest req = new WsRequest(message, session);

            IService service = null;

            if(req.getServiceId()!=null)
                service = registry.getById(req.getServiceId());

            if(service==null && req.getServiceName()!=null)
            {
                Collection<IService> services = registry.getByName(req.getServiceName());
                if(services.size() == 1) 
                    service = services.iterator().next();
                else if(services.size() > 1) 
                    throw new WebSocketException("Ambiguous service for name: " + req.getServiceName());
            }

            if(service==null && req.getServiceType()!=null)
            {
                Collection<IService> services = registry.getByType(req.getServiceType());
                if(services == null || services.isEmpty())
                    throw new WebSocketException("No service found for type: " + req.getServiceType());
                service = services.iterator().next();
            }

            // ok for other types of invocations like terminate
            /*if(service==null)
            {
                System.out.println("Service not found for: "+message);
                throw new RuntimeException("Service not found for: "+message);
            }*/

            PublishContext ctx = new PublishContext(service!=null? registry.getServiceInfo(service.getServiceId()): null, service, null);

            IRequestManager manager = RequestManagerFactory.getInstance();
            
            manager.handleRequest(req, null, ctx);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new WebSocketException(e);
        }
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason)
    {
        System.out.println("WS closed: " + reason);
    }

    @OnWebSocketError
    public void onError(Throwable cause)
    {
        cause.printStackTrace();
    }
}