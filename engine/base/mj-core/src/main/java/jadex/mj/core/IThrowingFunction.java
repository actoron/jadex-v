package jadex.mj.core;

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
}
