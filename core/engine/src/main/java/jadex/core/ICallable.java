package jadex.core;

import java.util.concurrent.Callable;

import jadex.future.IFuture;

public interface ICallable<T> extends Callable<T>
{
	/**
     * Provides the return type of the future.
     *
     * @return the class type of the future result
     */
    default Class<? extends IFuture<?>> getFutureReturnType() 
    {
    	@SuppressWarnings("unchecked")
    	// Double cast necessary for Java compiler, wtf :-(
		Class<? extends IFuture<?>>	ret	= (Class<? extends IFuture<?>>)(Class<?>)IFuture.class;
    	return  ret;
    }
}
