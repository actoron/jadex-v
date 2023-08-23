package jadex.mj.feature.providedservice.impl.service.impl.interceptors;

/**
 * 
 */
public class ConditionException extends RuntimeException
{
	/**
	 * 
	 */
    public ConditionException(String message) 
    {
    	super(message);
    }

    /**
     * 
     */
    public ConditionException(String message, Throwable cause) 
    {
        super(message, cause);
    }

    /**
     * 
     */
    public ConditionException(Throwable cause) 
    {
        super(cause);
    }
}
