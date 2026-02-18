package jadex.core;

import java.lang.reflect.AnnotatedType;
import java.util.concurrent.Callable;

/**
 *  Interface to allow specifying alternative return type information,
 *  e.g. for generic invocation handlers delegating to concrete methods.
 */
public interface ICallable<T> extends Callable<T>, IStep
{
	/**
     * 	Get the return type to use for scheduling this callable.
     */
    public AnnotatedType	getReturnType();
}
