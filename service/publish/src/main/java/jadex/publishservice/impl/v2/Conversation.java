package jadex.publishservice.impl.v2;

import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;

/**
 *  Struct for storing info about a request and the results.
 */
public class Conversation
{
    //protected Queue<Object>	results;

    //protected MappingInfo mappingInfo;

    protected boolean terminated;

    //protected Throwable exception;

    // to check time gap between last request from browser and current result
    // if gap>timeout -> abort future as probably no browser listening any more
    //protected long lastcheck;
    
    protected IFuture<?> future;
    
    protected String sessionid;
    
    protected String callid;

    protected IResponseStrategy responseStrategy;
    
    private long lastActivity = System.currentTimeMillis();

    /**
     *  Create a request info.
     */
    //public RequestInfo(MappingInfo mappingInfo, IFuture<?> future)
    public Conversation(String callid, String sessionid, IFuture<?> future)
    //public ConversationInfo(HttpSession session, IFuture<?> future)
    {
        this.callid = callid;
        this.sessionid = sessionid;
        this.future = future;
        //this.mappingInfo = mappingInfo;
        //this.future = future;
        //this.lastcheck = updateTimestamp();
    }

    public void setResponseStrategy(IResponseStrategy strategy) 
    {
        this.responseStrategy = strategy;
    }

    public IResponseStrategy getResponseStrategy() 
    {
        return responseStrategy;
    }

    public void terminate() 
    {
        if(!terminated)
        {
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

    /*public void terminate() 
    {
        terminated = true;
    }*/

    /**
     *  Check if terminated
     *  @return True if terminated.
     */
    public boolean isTerminated()
    {
        return terminated;
    }

    /**
     * Check, if there is a result that is not yet consumed. Also increases
     * the check timer to detect timeout when browser is disconnected.
     * 
     * @return True if there is a result.
     * /
    public boolean checkForResult()
    {
        this.lastcheck = System.currentTimeMillis();
        return results != null && !results.isEmpty();
    }*/

    /**
     * Add a result.
     * 
     * @param result The result to add
     * /
    public void addResult(Object result)
    {
        if(results == null)
            results = new ArrayDeque<>();
        results.add(result);
    }*/

    /**
     * Get the mappingInfo.
     * 
     * @return The mappingInfo
     * /
    public MappingInfo getMappingInfo()
    {
        return mappingInfo;
    }*/

    /**
     * Get the exception (if any).
     * /
    public Throwable getException()
    {
        return exception;
    }*/

    /**
     * Set the exception.
     * /
    public void setException(Throwable exception)
    {
        this.exception = exception;
    }*/

    /**
     * Get the next result (FIFO order).
     * 
     * @throws NullPointerException if there were never any results
     * @throws NoSuchElementException if the last result was already
     *         consumed.
     * /
    public Object getNextResult()
    {
        return results.remove();
    }*/
    
    /**
     * Get the results.
     * /
    public Object getResults()
    {
        return results;
    }*/

    /**
     *  Renew the timestamp.
     * /
    public long updateTimestamp()
    {
        return lastcheck = System.currentTimeMillis();
    }*/
    
    /**
     *  Get the timestamp of the last check (i.e. last request from browser).
     * /
    public long getTimestamp()
    {
        return lastcheck;
    }*/

    /**
     *  Get the future.
     *  @return the future
     */
    public IFuture<?> getFuture()
    {
        return future;
    }
    
    /**
     * @param future the future to set
     */
    public void setFuture(IFuture<?> future) 
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