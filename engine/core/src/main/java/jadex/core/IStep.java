package jadex.core;

import java.util.concurrent.Callable;

/**
 *  Marker interface for component steps to easily find
 *  helper classes for magic lambda casts, e.g.
 *  scheduleAsyncStep((<marker-step>) comp -> ...).
 *  
 *  Note that also {@link Runnable} and {@link Callable} are supported step types.
 */
public interface IStep
{
}
