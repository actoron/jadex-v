package jadex.core;

/**
 *  A functional interface for operations that accepts one argument and may throw an exception.
 *  This is similar to {@link java.util.function.Consumer}, but allows checked exceptions.
 *  
 *  Can be used as component step receiving the current component.
 */
@FunctionalInterface
public interface IThrowingConsumer<T>	extends IStep
{
	/**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t) throws Exception;
}
