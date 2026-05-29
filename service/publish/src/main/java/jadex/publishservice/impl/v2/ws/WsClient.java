package jadex.publishservice.impl.v2.ws;

import jakarta.websocket.*;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

import com.eclipsesource.json.*;

import jadex.common.SReflect;
import jadex.core.IComponentManager;
import jadex.execution.future.FutureFunctionality;
import jadex.future.*;
import jadex.future.Future;
import jadex.publishservice.impl.v2.JsonMapper;

public class WsClient
{
    record CallInfo(Future<Object> future, Method method) {}

    protected volatile Session session;

    protected final String url;
    
    protected final Map<String, CallInfo> pending = new ConcurrentHashMap<>();

    protected final int max = 5;
    
    protected final long recondelay = 500;
    
    protected final Timer timer = new Timer(true);

    public WsClient(String url)
    {
        this.url = url;
        connect(0);
    }

    private synchronized void connect(int attempt)
    {
        try
        {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(new Endpoint()
            {
                @Override
                public void onOpen(Session session, EndpointConfig config)
                {
                    WsClient.this.session = session;
                    session.addMessageHandler(String.class, WsClient.this::handleMessage);
                    //System.out.println("WS connected: " + session);
                }

                @Override
                public void onError(Session session, Throwable thr)
                {
                    System.out.println("WebSocket error: " + thr);
                    handleDisconnect(0); // Reset attempt on error
                }

                @Override
                public void onClose(Session session, CloseReason closeReason)
                {
                    System.out.println("WebSocket closed: " + closeReason);
                    handleDisconnect(0); // Reset attempt on normal close
                }
            }, URI.create(url));
        }
        catch (Exception e)
        {
            System.out.println("Initial connection failed: " + e.getMessage());
            handleDisconnect(attempt);
        }
    }

    private void handleDisconnect(int attempt)
    {
        int next = attempt + 1;
        if(next > max)
        {
            pending.values().forEach(ci -> ci.future().setException(new RuntimeException("WebSocket disconnected permanently")));
            pending.clear();
            return;
        }

        long delay = computeDelay(next);
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                System.out.println("Reconnect attempt " + next);
                connect(next);
            }
        }, delay);
    }

    protected long computeDelay(int attempt)
    {
        return attempt * recondelay;
    }

    public IFuture<Object> call(Class<?> service, Method method, Object[] args)
    {
        Future<Object> ret;
        String callId = UUID.randomUUID().toString();

        JsonObject json = new JsonObject();
        json.add("callid", callId);
        json.add("serviceType", service.getName());
        json.add("method", method.getName());

        if(args != null && args.length > 0)
            json.add("parameters", JsonMapper.toJsonObject(args).asObject());

        if(SReflect.isSupertype(IFuture.class, method.getReturnType()))
            ret = FutureFunctionality.createReturnFuture(method, new FutureFunctionality()
        {
            @Override
            public void	handleTerminated(Exception reason)
            {
                JsonObject json = new JsonObject();
                json.add("callid", callId);
                json.add("type", "terminate");
                json.add("terminate", true);
                json.add("reason", reason.toString());

                // send terminate to ws endpoint
                send(json.toString());
            }
            
            /*@Override   
            public void	handleBackwardCommand(Object info)
            {
            }*/
        });
        else
        {
            ret = new Future<>();
        }
        
        pending.put(callId, new CallInfo(ret, method));
        
        // send call to ws endpoint
        send(json.toString());

        return ret;
    }

    private void handleMessage(String msg)
    {
        try
        {
            JsonObject json = Json.parse(msg).asObject();
            String callid = json.getString("callid", null);
            if(callid == null) 
                return;

            CallInfo ci = pending.get(callid);
            if(ci == null)
            {
                System.out.println("CallId not found: " + callid);
                return;
            }

            Future<Object> fut = ci.future();
            if(json.get("error") != null)
            {
                fut.setException(new RuntimeException(json.getString("error", "unknown")));
                pending.remove(callid);
            }
            else
            {
                JsonValue res = json.get("result");
                Object result = res == null ? null
                    : JsonMapper.convertJsonValue(res.toString(), getTargetType(ci.method()), IComponentManager.get().getClassLoader(), false);

                if(fut instanceof IIntermediateFuture)
                {
                    boolean finished = json.getBoolean("finished", false);
                    if(!finished)
                        ((IntermediateFuture)fut).addIntermediateResult(result);
                    else
                    {
                        ((IntermediateFuture)fut).setFinished();
                        pending.remove(callid);
                    }
                }
                else
                {
                    fut.setResult(result);
                    pending.remove(callid);
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void send(String msg)
    {
        if(session == null || !session.isOpen())
        {
            System.out.println("Message dropped, session not open: " + msg);
            return;
        }
        session.getAsyncRemote().sendText(msg);
    }

    private Class<?> getTargetType(Method method)
    {
        Class<?> retType = method.getReturnType();
        if(IFuture.class.isAssignableFrom(retType))
        {
            Type genericType = method.getGenericReturnType();
            if(genericType instanceof ParameterizedType pt)
            {
                Type t = pt.getActualTypeArguments()[0];
                if(t instanceof Class<?> cls) return cls;
            }
            return Object.class;
        }
        else return retType;
    }
}