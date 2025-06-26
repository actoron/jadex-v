package jadex.remoteservices.impl;

import jadex.future.IFuture;
import jadex.remoteservices.impl.remotecommands.RemoteReference;

import java.lang.reflect.Method;


/**
 *  Feature for securely sending and handling remote execution commands.
 *  Internal methods, e.g., for platform-specific commands.
 */
public interface IInternalRemoteExecutionFeature
{
	/**
	 *  Invoke a method on a remote object.
	 *  @param ref	The target reference.
	 *  @param method	The method to be executed.
	 *  @param args	The arguments.
	 *  @return	The result(s) of the method invocation, if any. Connects any futures involved.
	 */
	public <T> IFuture<T> executeRemoteMethod(RemoteReference ref, Method method, Object[] args);
}
