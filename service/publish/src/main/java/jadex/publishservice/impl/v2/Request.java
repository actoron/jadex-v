package jadex.publishservice.impl.v2;


import jadex.common.SUtil;

public abstract class Request 
{
    protected boolean inited;

    protected String callId;

    protected String sessionId;

    protected Object rawRequest; // z.B. HttpServletRequest, WebSocketMessage, etc.

    protected TransportType type;

    protected Connection connection;

    public Request(Object rawRequest) 
    {
        this.rawRequest = rawRequest;
    }

    protected void ensureInited()
    {
        if(!inited)
        {
            this.callId = extractCallId();

            this.type = extractTransportType();

            this.sessionId = extractSessionId();

            // Session can be null if no header is provided by client
            if(sessionId==null)
                sessionId = SUtil.createUniqueId();
            
            this.inited = true;
        }
    }

    public abstract String extractSessionId();

    public abstract String extractCallId();

    public abstract TransportType extractTransportType();

    public String getSessionId() 
    { 
       ensureInited();
       return sessionId; 
    }

    public String getCallId() 
    { 
        ensureInited();
        return callId; 
    }

    public Object getRawRequest() 
    {
        return rawRequest;
    }

    public Connection getConnection() 
    {
        ensureInited();
        return connection;
    }

    public void setConnection(Connection connection) 
    {
        ensureInited();
        this.connection = connection;
    }

    public TransportType getTransportType()
    {
        ensureInited();
        return type;
    }

    /*public TransportMode getTransportMode()
    {
        return transportMode;
    }*/

    /*public void setTransportMode(TransportMode mode)
    {
        this.transportMode = mode;
    }*/
}