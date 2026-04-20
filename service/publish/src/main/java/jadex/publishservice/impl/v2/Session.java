package jadex.publishservice.impl.v2;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.core.impl.IDaemonComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.invocation.InvocationResult;

public class Session 
{
    protected final String id;

    protected final Map<String, Object> attributes = new HashMap<>();

    protected final List<Connection> connections = new ArrayList<>();

    protected final Map<String, Conversation> conversations = new HashMap<>();

    protected volatile long lastact;

    protected long conto = 30000;

    protected IComponentHandle agent;

    public Session(String id) 
    {
        this.id = id;
        updateLastActivity();
    }

    public void terminate()
    {
        Iterator<Connection> cit = connections.iterator();
        while(cit.hasNext())
        {
            Connection c = cit.next();
            try 
            {
                c.terminate();
            } 
            catch(Exception e) 
            {
                e.printStackTrace();
            }
            cit.remove();
        }

        Iterator<Map.Entry<String, Conversation>> it = conversations.entrySet().iterator();
        while(it.hasNext())
        {
            Conversation conv = it.next().getValue();
            try 
            {
                conv.terminate(new RuntimeException("Conversation timeout"));
            } 
            catch(Exception e) 
            {
                e.printStackTrace();
            }
            it.remove();
        }

        if(agent!=null)
            agent.terminate();

        System.out.println("Session terminated: "+id);
    }

    public void pruneConnectionsAndConversations(long convto) 
    {
        long now = System.currentTimeMillis();

        Iterator<Connection> cit = connections.iterator();
        while(cit.hasNext())
        {
            Connection c = cit.next();
            if(!c.isAlive(conto))
            {
                try 
                {
                    c.terminate();
                } 
                catch(Exception e) 
                {
                    e.printStackTrace();
                }
                cit.remove();
            }
        }

        Iterator<Map.Entry<String, Conversation>> it = conversations.entrySet().iterator();
        while(it.hasNext())
        {
            Conversation conv = it.next().getValue();
            if(now - conv.getLastActivity() > convto)
            {
                try 
                {
                    conv.terminate(new RuntimeException("Conversation timeout"));
                } 
                catch(Exception e) 
                {
                    e.printStackTrace();
                }
                it.remove();
            }
        }
    }

    public IFuture<Void> scheduleOnSession(IThrowingConsumer<IComponent> step)
    {
        if(agent==null)
        {
            synchronized(this)
            {
                try
                {
                    if(agent==null)
                        agent = IComponentManager.get().create(new IDaemonComponent() {}).get();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }

        return agent.scheduleStep(step);
    }

    public void updateLastActivity() 
    {
        lastact = System.currentTimeMillis();
    }

    public long getLastact() 
    {
        return lastact;
    }

    public String getId() 
    {
        return id;
    }

    public Map<String, Object> getAttributes() 
    {
        return attributes;
    }

    public List<Connection> getConnections() 
    {
        return connections;
    }

    public Map<String, Conversation> getConversations() 
    {
        return conversations;
    }

    public void addConnection(Connection con) 
    {
        connections.add(con);
        updateLastActivity();
    }

    public void removeConnection(String connectionId) 
    {
        connections.removeIf(c -> c.getId().equals(connectionId));
        updateLastActivity();
    }

    public Conversation getOrCreateConversation(String callid, Future<?> future) 
    {
        Conversation conv = conversations.get(callid);
        if (conv == null) 
        {
            conv = new Conversation(callid, id, future);
            conversations.put(callid, conv);
        }
        updateLastActivity();
        return conv;
    }

    public Conversation getConversation(String callid) 
    {
        return conversations.get(callid);
    }

    public void endConversation(String callid, Exception ex) 
    {
        Conversation conv = conversations.remove(callid);
        if (conv != null) 
            conv.terminate(ex);
        updateLastActivity();
    }

    public void dispatch(Message message, PublishContext context) 
    {
        IResponseStrategy strategy = null;
        try
        {
            System.out.println("session dispatch");

            checkCorrectSession(message.getRequest().getSessionId());

            Conversation conv = getConversation(message.getRequest().getCallId());

            if (conv != null && conv.getResponseStrategy() != null) 
            {
                strategy = conv.getResponseStrategy();
            } 
            else 
            {
                strategy = selectStrategy(message, context);

                System.out.println("selected strategy: "+strategy);
            
                if (conv == null || conv.getFuture() instanceof IIntermediateFuture) 
                {
                    conv = getOrCreateConversation(message.getRequest().getCallId(),
                        conv != null ? (Future<?>) conv.getFuture() : null);
                    conv.setResponseStrategy(strategy);
                }
            }

            System.out.println("before strategy send: "+message);
            strategy.send(message);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            if(strategy!=null)
                strategy.terminate();
        }
    }

    protected void checkCorrectSession(String sessionid) 
    {
        if (!this.id.equals(sessionid)) 
            throw new RuntimeException("Message handling in wrong session: " + this.id + " / " + sessionid);
    }
    protected Connection findStreamingConnection() 
    {
        Connection best = null;
        for (Connection con : connections) 
        {
            if (!con.isAlive(conto)) 
                continue;
            if (!con.supports(TransportMode.STREAM)) 
                continue;
            if (best == null || con.getType().isBetterThan(best.getType()))
                best = con;
        }
        return best;
    }

    protected IResponseStrategy selectStrategy(Message message, PublishContext ctx)
    {
        IResponseStrategy ret = null;

        Request req = message.getRequest();

        checkCorrectSession(req.getSessionId());

        Conversation conv = getConversation(req.getCallId());
        Connection rcon = message.getRequest().getConnection();

        System.out.println("publish type is: "+ctx.info().getPublishType());
        TransportType tt = TransportType.fromString(ctx.info().getPublishType());
        boolean forceStreaming = tt.isStreaming()
            || (conv != null && conv.getFuture() instanceof IIntermediateFuture);

        if(!forceStreaming)
        {
            ret = new DirectResponseStrategy(rcon);
        }
        else
        {
            if(rcon != null && rcon.supports(TransportMode.STREAM))
                ret = new StreamingResponseStrategy(rcon);

            Connection scon = findStreamingConnection();

            if(scon!=null)
            {
                if(rcon!=null)
                    ret = new SwitchTransportStrategy(rcon, scon);
                else
                    ret = new StreamingResponseStrategy(scon);
            }
            else if(rcon!=null)
            {
                System.out.println("Streaming required but no streaming connection found, must use direct connection fallback (long polling)");
                ret = new DirectResponseStrategy(rcon);
            }
            else
            {
                throw new RuntimeException("No streaming connection found.");
            }
        }

        return ret;
    }

    public static class DirectResponseStrategy implements IResponseStrategy
    {
        private final Connection con;

        public DirectResponseStrategy(Connection con)
        {
            if(con==null)
                throw new IllegalArgumentException("Connection must not null");
            this.con = con;
        }

        public void send(Message msg) throws Exception
        {
            con.send(msg);
            con.terminate();
        }

        @Override
        public void terminate() 
        {
            con.terminate();    
        }
    }

    public static class StreamingResponseStrategy implements IResponseStrategy
    {
        private final Connection con;

        public StreamingResponseStrategy(Connection con)
        {
            this.con = con;
        }

        public void send(Message msg) throws Exception
        {
            con.send(msg);
        }

        @Override
        public void terminate() 
        {
            // todo: when to terminate connection?!
            //con.terminate();    
        }
    }

    public static class SwitchTransportStrategy implements IResponseStrategy
    {
        private final Connection requestConnection;
        private final Connection streamingConnection;

        public SwitchTransportStrategy(Connection requestConnection, Connection streamingConnection)
        {
            this.requestConnection = requestConnection;
            this.streamingConnection = streamingConnection;
        }
 
        public void send(Message msg) throws Exception
        {
            // todo: connectionTimeout from session
            if(requestConnection.isAlive(30000))
            {
                requestConnection.send(new Message(msg.getRequest(), msg.getResponse()).setResult(new InvocationResult("ack")));
                requestConnection.terminate();
            }

            streamingConnection.send(msg);
        }

        @Override
        public void terminate() 
        {
            requestConnection.terminate();
            // todo: when to terminate connection?!
            //streamingConnection.terminate();   
        }
    }
}