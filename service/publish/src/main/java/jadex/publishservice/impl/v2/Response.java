package jadex.publishservice.impl.v2;


public class Response 
{
    protected String callId;

    protected String sessionId;
    
    protected Object rawResponse;

    public Response(Object rawResponse) 
    {
        this.rawResponse = rawResponse;
    }

    public Object getRawResponse() 
    {
        return rawResponse;
    }
}
