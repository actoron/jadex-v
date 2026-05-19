package jadex.publishservice.impl.v2;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.future.ITerminableFuture;

/**
 *  Struct for storing info about a request and the results.
 */
public class Conversation
{
    protected boolean mustterminate;

    protected boolean terminated;

    protected Future<?> future;
    
    protected String sessionid;
    
    protected String callid;

    protected IResponseStrategy responseStrategy;
    
    private long lastActivity = System.currentTimeMillis();

    /**
     *  Create a request info.
     */
    public Conversation(String callid, String sessionid, Future<?> future)
    {
        this.callid = callid;
        this.sessionid = sessionid;
        this.future = future;
    }

    public void setResponseStrategy(IResponseStrategy strategy) 
    {
        this.responseStrategy = strategy;
    }

    public IResponseStrategy getResponseStrategy() 
    {
        return responseStrategy;
    }

    public void terminate(Exception ex) 
    {
        if(!terminated)
        {
            Future<?> fut = getFuture();
            if(fut instanceof ITerminableFuture)
            {
                if(ex!=null)
                    ((ITerminableFuture)fut).terminate(ex); 
                else
                    ((ITerminableFuture)fut).terminate();
                
                //System.out.println("terminate on: "+cinfo.getFuture().hashCode());
            }
            else //if(clientterm)
            {
                System.out.println("WARNING: future cannot be terminated: "+this);
            }
		
            setTerminated(true);
        
            if (responseStrategy != null) 
                responseStrategy.terminate();
        }
    }

    public void updateActivity() 
    {
        lastActivity = System.currentTimeMillis();
    }

    public long getLastActivity() 
    {
        return lastActivity;
    }

    /**
     *  Set it to terminated.
     */
    public void setTerminated(boolean term)
    {
        terminated = term;
    }

    /**
     *  Check if terminated
     *  @return True if terminated.
     */
    public boolean isTerminated()
    {
        return terminated;
    }

     /**
     *  Set it to terminated.
     */
    public void setMustTerminate(boolean term)
    {
        mustterminate = term;
    }

    /**
     *  Check if must terminate
     *  @return True if must terminate.
     */
    public boolean isMustTerminate()
    {
        return mustterminate;
    }

    /**
     *  Get the future.
     *  @return the future
     */
    public Future<?> getFuture()
    {
        return future;
    }
    
    /**
     * @param future the future to set
     */
    public void setFuture(Future<?> future) 
    {
        this.future = future;
    }

    /**
     * @return the session
     */
    public String getSessionId() 
    {
        return sessionid;
    }

    /**
     * @param session the session to set
     */
    public void setSessionId(String sessionid) 
    {
        this.sessionid = sessionid;
    }
    
    /**
     *  Get the callid.
     *  @return the callid.
     */
    public String getCallId() 
    {
        return callid;
    }

    /**
     *  Set the callid.
     *  @param callid The callid.
     */
    public void setCallId(String callid) 
    {
        this.callid = callid;
    }

    /**
     *  Test if it is an intermediate future.
     *  @return True, if is intermediate future.
     */
    public boolean isIntermediateFuture()
    {
        return future instanceof IIntermediateFuture;
    }

    public String toString() 
    {
        return "ConversationInfo [terminated=" + terminated + ", future=" + future
            + ", sessionid=" + sessionid + ", callid=" + callid + "]";
    }
}