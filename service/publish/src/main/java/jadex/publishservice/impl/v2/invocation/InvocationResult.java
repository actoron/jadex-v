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

    public void setPayload(Object payload) 
    {
        this.payload = payload;
    }

    public Exception getException()
    {
        return exception;
    }

    public void setException(Exception exception) 
    {
        this.exception = exception;
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
}