package jadex.publishservice.impl.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Session 
{
    protected String id;
    
    protected Map<String,Object> attributes = new HashMap<>();
    
    protected List<Connection> connections = new ArrayList<>();
    
    protected Map<String, Conversation> conversations = new HashMap<>();
    
    protected long lastActivity;

    protected long connectionTimeout = 30000;

    Session(String id) 
    {
        this.id = id;
        updateLastActivity();
    }

    void updateLastActivity() 
    {
        lastActivity = System.currentTimeMillis();
    }

    long getLastActivity() 
    {
        return lastActivity;
    }

    public void addConnection(Connection con)
    {
        connections.add(con);
    }

    public List<Connection> getConnections() 
    {
        return connections;
    }

    public Conversation getConversation(String callId)
    {
        return conversations.get(callId);
    }

    public String getId() 
    {
        return id;
    }

    public Map<String, Object> getAttributes() 
    {
        return attributes;
    }

    public Connection findStreamingConnection()
    {
        Connection best = null;

        for(Connection con : connections)
        {
            if(!con.isAlive(connectionTimeout))
                continue;

            if(!con.supports(TransportMode.STREAM))
                continue;

            if(best == null || con.getType().isBetterThan(best.getType()))
                best = con;
        }

        return best;
    }
}