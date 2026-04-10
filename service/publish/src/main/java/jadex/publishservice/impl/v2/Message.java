package jadex.publishservice.impl.v2;

import java.util.HashMap;
import java.util.Map;

import jadex.publishservice.impl.v2.invocation.InvocationResult;

public class Message 
{
    protected Request request;
    
    protected Response response;
    
    protected InvocationResult result;

    protected boolean isError;
    
    protected Map<String,Object> meta = new HashMap<>();

    public Message(Request request, Response response) 
    {
        this.request = request;
        this.response = response;
    }

    public Request getRequest() 
    {
        return request;
    }

    public Response getResponse() 
    {
        return response;
    }

    public InvocationResult getResult()
    { 
        return result; 
    }

    public Message setResult(InvocationResult result) 
    { 
        this.result = result; 
        return this;
    }
    
    public Map<String,Object> getMeta() 
    { 
        return meta; 
    }

    public Message addMetaInfo(String key, Object value)
    {
        meta.put(key, value);
        return this;
    }
    
    public Message setError(boolean isError)
    {
        this.isError = isError;
        return this;
    }

    public boolean isError() 
    {
        return isError;
    }

    @Override
    public String toString() 
    {
        return "Message: "+result+" err:"+isError+" meta="+meta;
    }

}
