package jadex.publishservice.impl.v2;

import java.util.Set;

public abstract class Connection 
{    
    protected Set<TransportMode> supportedModes;

    protected String id;

    protected TransportType type;

    protected long lastAlive = System.currentTimeMillis();

    protected boolean terminated = false;

    public Connection(String id, TransportType type)
    {
        this.id = id;
        this.type = type;
    }

    public TransportType getType()
    {
        return type;
    }

    public void markAlive() 
    {
        lastAlive = System.currentTimeMillis();
    }

    public boolean isAlive(long timeout) 
    {
        return !terminated && System.currentTimeMillis() - lastAlive <= timeout;
    }

    public abstract boolean send(Message message) throws Exception;

    public void terminate() 
    {
        terminated = true;
    }

    public String getId() 
    {
        return id;
    }

    public boolean supports(TransportMode mode)
    {
        return supportedModes.contains(mode);
    }

}