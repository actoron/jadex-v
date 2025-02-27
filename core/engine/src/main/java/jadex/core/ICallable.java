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
    	// necessary for Java compiler, wtf :-(
    	return (Class<? extends IFuture<?>>)(Class<?>)IFuture.class;
    }
}
