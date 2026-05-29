package jadex.publishservice.impl.v2.invocation;

import java.util.HashMap;
import java.util.Map;


public class InvocationResult
{
    protected Object payload;

    protected Exception exception;

    protected boolean finished;

    protected Integer max;

	//protected List<String> resulttypes;

    //protected MappingInfo mi;

    //protected int status;

    protected Map<String,Object> meta = new HashMap<>();

    public InvocationResult()
    {
    }

    public InvocationResult(Object payload)
    {
        this.payload = payload;
    }

    public InvocationResult(Exception exception)
    {
        this.exception = exception;
    }

    public Object getPayload() 
    { 
        return payload; 
    }

    public InvocationResult setPayload(Object payload) 
    {
        this.payload = payload;
        return this;
    }

    public Exception getException()
    {
        return exception;
    }

    public InvocationResult setException(Exception exception) 
    {
        this.exception = exception;
        return this;
    }
    
    public InvocationResult addMetaInfo(String key, Object value)
    {
        meta.put(key, value);
        return this;
    }

    public Object getMetaInfo(String key)
    {
        return meta.get(key);
    }

    public InvocationResult setFinished(boolean finished) 
    {
        this.finished = finished;
        return this;
    }

    public boolean isFinished() 
    {
        return finished;
    }

    public InvocationResult setMax(Integer max) 
    {
        this.max = max;
        return this;
    }

    public Integer getMax() 
    {
        return max;
    }

    @Override
    public String toString() 
    {
        return "InvocationResult(" + "payload " + payload + ", exception " + exception+")";
    }
}