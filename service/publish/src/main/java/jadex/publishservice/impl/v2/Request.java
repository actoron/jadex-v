package jadex.publishservice.impl.v2;


import jadex.common.SUtil;
import jakarta.servlet.http.HttpServletRequest;

public abstract class Request 
{
    protected String callId;

    protected String sessionId;

    protected Object rawRequest; // z.B. HttpServletRequest, WebSocketMessage, etc.

    protected TransportType type;

    protected Connection connection;

    //protected TransportMode transportMode = TransportMode.REQUEST_RESPONSE;

    public Request(Object rawRequest) 
    {
        this.rawRequest = rawRequest;

        this.callId = extractCallId();

        this.sessionId = extractSessionId();
		
		// sessionid can be missing if client just sends a REST request instead of a jadex.js request
		
        this.type = extractTransportType();
        //this.connection = extractRequestConnection();

		// Session can be null if no header is provided by client
		if(sessionId==null)
			sessionId = SUtil.createUniqueId();
    }

    public abstract String extractSessionId();

    public abstract String extractCallId();

    //public abstract Connection extractRequestConnection();

    public abstract TransportType extractTransportType();

    public String getSessionId() 
    { 
        return sessionId; 
    }

    public String getCallId() 
    { 
        return callId; 
    }

    public Object getRawRequest() 
    {
        return rawRequest;
    }

    public Connection getConnection() 
    {
        return connection;
    }

    public void setConnection(Connection connection) 
    {
        this.connection = connection;
    }

    public TransportType getTransportType()
    {
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