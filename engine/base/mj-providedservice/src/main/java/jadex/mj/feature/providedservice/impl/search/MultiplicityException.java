package jadex.mj.feature.providedservice.impl.search;

import jadex.future.FutureTerminatedException;

/**
 *  Exception that the multiplicity has been violated.
 */
public class MultiplicityException extends FutureTerminatedException
{
	/**
	 *  Create an exception.
	 */
    public MultiplicityException() 
    {
    }

    /**
	 *  Create an exception with message.
	 */
    public MultiplicityException(String message) 
    {
        super(message);
    }

    /**
   	 *  Create an exception with message and cause.
   	 */
    public MultiplicityException(String message, Throwable cause) 
    {
        super(message, cause);
    }

    /**
   	 *  Create an exception with cause.
   	 */
    public MultiplicityException(Throwable cause) 
    {
        super(cause);
    }
}
