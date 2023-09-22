package jadex.mj.core;

@FunctionalInterface
public interface IThrowingConsumer<T> 
{
	/**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t) throws Exception;
}
