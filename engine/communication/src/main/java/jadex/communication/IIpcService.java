package jadex.communication;

import java.nio.ByteBuffer;

import jadex.communication.impl.IpcStreamHandler;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;


/**
 *  Interface for sending and receiving messages to components outside the current JVM.
 *  
 *  The implementation can be optimized for the local operating system, but
 *  only one implementation should be used per-system to avoid incompatibility.
 */
public interface IIpcService
{
	/**
	 *  Gets the singleton instance of the handler.
	 *  @return Singleton instance of the handler.
	 */
	public static IIpcService get()
	{
		return IpcStreamHandler.get();
	}
	
	/**
	 *  Sends a message to a component outside the current JVM.
	 *  
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public void sendMessage(ComponentIdentifier receiver, ByteBuffer message);
}
