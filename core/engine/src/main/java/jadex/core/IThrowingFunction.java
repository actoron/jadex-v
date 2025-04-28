package jadex.core;

import jadex.future.IFuture;

@FunctionalInterface
public interface IThrowingFunction<T, R> 
{
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 */
	public R apply(T t) throws Exception;
	
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
