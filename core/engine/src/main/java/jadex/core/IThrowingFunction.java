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
        return (Class<? extends IFuture<?>>)IFuture.class;
    }
}
