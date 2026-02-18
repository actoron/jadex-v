package jadex.core;

import java.lang.reflect.AnnotatedType;

/**
 *  Functional interface similar to {@link java.util.function.Function} but allowing to throw checked exceptions.
 *  
 *  Can be used as component step receiving the current component.
 */
@FunctionalInterface
public interface IThrowingFunction<T, R>	extends IStep
{
	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 */
	public R apply(T t) throws Exception;
	
	/**
	 *  Method to allow specifying alternative return type information,
	 *  e.g. for generic invocation handlers delegating to concrete methods.
	 */
    public default AnnotatedType	getReturnType()
    {
    	return null;
    }
}
