package jadex.messaging.impl;

import java.nio.ByteBuffer;

import jadex.core.ComponentIdentifier;


/**
 *  Interface for sending and receiving messages to components outside the current JVM.
 *  
 *  The implementation can be optimized for the local operating system, but
 *  only one implementation should be used per-system to avoid incompatibility.
 */
public interface IIpcService
{
	/**
	 *  Sends a message to a component outside the current JVM.
	 *  
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public void sendMessage(ComponentIdentifier receiver, ByteBuffer message);
}
