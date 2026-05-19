package jadex.publishservice.impl.v2.invocation;

import jadex.core.IComponentManager;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.publishservice.IRequestManager.PublishContext;
import jadex.publishservice.impl.v2.Request;
import jadex.publishservice.impl.v2.Session;
import jadex.publishservice.impl.v2.SessionManager;

public class Invocation 
{
    protected Request request;

    protected PublishContext context;
    
    protected Throwable exception;

    protected InvocationResult res;

    public Invocation(Request request, PublishContext context) 
    {
        this(request, context, null);
    }

    public Invocation(Request request, PublishContext context, InvocationResult res) 
    {
        this.request = request;
        this.context = context;
        this.res = res;
    }

    public ISubscriptionIntermediateFuture<InvocationResult> invoke(Session sesman)
    {
        SubscriptionIntermediateFuture<InvocationResult> ret = new SubscriptionIntermediateFuture<>();

        if(res!=null && res.getException()!=null)
        {
            ret.addIntermediateResult(res);
            ret.setFinished();
        }
        else if(res!=null)
        {
            ret.setException(res.getException());
        }
        else
        {
            ret.setException(new IllegalArgumentException("No result in invocation"));
        }

        return ret;
    }

    /*public InvocationResult getResult() 
    {
        return result;
    }*/

    /*public void setResult(InvocationResult result) 
    {
        this.result = result;
    }*/   

    public Request getRequest() 
    {
        return request;
    }

    public ClassLoader getClassLoader()
    {
        return IComponentManager.get().getClassLoader();
    }

    public PublishContext getContext() 
    {
        return context;
    }

    public Throwable getException() 
    {
        return exception;
    }

}
