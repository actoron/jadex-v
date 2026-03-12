package jadex.publishservice.impl.v2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.invocation.InvocationResult;

public class SessionManager 
{
    protected final Map<String, Session> sessions = new ConcurrentHashMap<>();
  
    protected long connectionTimeout = 30000;

    protected long sessionTimeout = 60000;
    
    protected long conversationTimeout = 60000;

    public void prune() 
    {
        long now = System.currentTimeMillis();

        sessions.values().forEach(session -> 
        {
            session.connections.removeIf(con -> !con.isAlive(connectionTimeout));

            session.conversations.values().removeIf(conv -> now - conv.getLastActivity() > conversationTimeout);

            if (session.connections.isEmpty() && session.conversations.isEmpty()
                    && now - session.getLastActivity() > sessionTimeout) 
            {
                sessions.remove(session.id);
            }
        });
    }

    public Session getOrCreateSession(String sessionId) 
    {
        return sessions.computeIfAbsent(sessionId, Session::new);
    }

    public void removeSession(String sessionId) 
    {
        Session session = sessions.remove(sessionId);
        
        if (session != null) 
        {
            session.connections.forEach(Connection::terminate);
            session.connections.clear();
            session.conversations.values().forEach(Conversation::terminate);
            session.conversations.clear();
        }
    }

    //public Connection registerConnection(String sessionId, String connectionId, TransportType type, ITransportChannel channel) 
    public void registerConnection(Connection connection, String sessionId)
    {
        Session session = getOrCreateSession(sessionId);
        session.connections.add(connection);
        session.updateLastActivity();
    }

    public void removeConnection(String sessionId, String id) 
    {
        Session session = sessions.get(sessionId);
        
        if (session == null) 
            return;

        session.connections.removeIf(c -> c.getId().equals(id));

        session.updateLastActivity();
    }

    public void markAlive(String sessionId, String connectionId) 
    {
        Session session = sessions.get(sessionId);
        if (session == null) 
            return;

        for (Connection con : session.connections) 
        {
            if (con.getId().equals(connectionId)) 
            {
                con.markAlive();
                session.updateLastActivity();
                break;
            }
        }
    }

    public Conversation getConversation(String sessionId, String callid)
    {
        Session session = getOrCreateSession(sessionId);
        return session.getConversation(callid);
    }

    public Conversation startConversation(String sessionId, String callId, IFuture<?> future) 
    {
        Session session = getOrCreateSession(sessionId);
        
        Conversation conv = new Conversation(callId, sessionId, future);
        
        session.conversations.put(callId, conv);
        
        session.updateLastActivity();
        
        return conv;
    }

    public void endConversation(String sessionId, String callId) 
    {
        Session session = sessions.get(sessionId);
        if (session == null) 
            return;

        Conversation conv = session.conversations.remove(callId);
        if (conv != null) 
            conv.terminate();
        
        session.updateLastActivity();
    }

    public void dispatch(Message message, PublishContext context) 
    {
        Session session = sessions.get(message.getRequest().getSessionId());
        if(session == null)
        {
            System.out.println("Session not found");
            return;
        }

        Conversation conv = session.getConversation(message.getRequest().getCallId());

        IResponseStrategy strategy;
        if (conv != null && conv.getResponseStrategy() != null) 
        {
            strategy = conv.getResponseStrategy();
        } 
        else 
        {
            strategy = selectStrategy(message, context);
            
            if(message.getResult().getPayload() instanceof IIntermediateFuture)
            {
                conv = startConversation(session.getId(), message.getRequest().getCallId(), (IIntermediateFuture<?>)message.getResult().getPayload());
                conv.setResponseStrategy(strategy);
            }
        }

        try 
        {
            strategy.send(message);
        } 
        catch (Exception ex) 
        {
            ex.printStackTrace();
            strategy.terminate();
        }
    }

    public IResponseStrategy selectStrategy(Message message, PublishContext ctx)
    {
        Request req = message.getRequest();
        Session session = getOrCreateSession(req.getSessionId());
        Conversation conv = session.getConversation(req.getCallId());
        Connection requestCon = message.getRequest().getConnection();

        TransportType tt = TransportType.fromString(ctx.info().getPublishType());
        boolean forceStreaming = tt.isStreaming()
            || (conv != null && conv.getFuture() instanceof IIntermediateFuture);

        if(!forceStreaming)
            return new DirectResponseStrategy(requestCon);

        if(requestCon != null && requestCon.supports(TransportMode.STREAM))
            return new StreamingResponseStrategy(requestCon);

        Connection streaming = session.findStreamingConnection();

        if(streaming!=null)
            return new SwitchTransportStrategy(requestCon, streaming);

        return new DirectResponseStrategy(requestCon);
    }

    public static class DirectResponseStrategy implements IResponseStrategy
    {
        private final Connection con;

        public DirectResponseStrategy(Connection con)
        {
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
            con.terminate();    
        }
    }

    public static class SwitchTransportStrategy implements IResponseStrategy
    {
        public SwitchTransportStrategy(Connection requestConnection, Connection streamingConnection)
        {
            this.requestConnection = requestConnection;
            this.streamingConnection = streamingConnection;
        }

        private final Connection requestConnection;
        private final Connection streamingConnection;

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
            streamingConnection.terminate();   
        }
    }
}