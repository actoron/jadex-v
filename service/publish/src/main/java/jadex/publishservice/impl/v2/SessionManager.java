package jadex.publishservice.impl.v2;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager 
{
    protected final Map<String, Session> sessions = new ConcurrentHashMap<>();
  
    protected long conto = 30000;

    protected long sessto = 60000;
    
    protected long convto = 60000;

    public void prune() 
    {
        for(Session s : sessions.values())
        {
            s.scheduleOnSession(agent ->
            {
                s.pruneConnectionsAndConversations(convto);

                if(s.getConnections().isEmpty() 
                    && s.getConversations().isEmpty()
                    && System.currentTimeMillis() - s.getLastact() > sessto) 
                {
                    s.terminate();
                    sessions.remove(s.getId());
                }
            });
        }
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
            session.conversations.values().forEach(c -> c.terminate(new RuntimeException("Session timeout")));
            session.conversations.clear();
        }
    }

}