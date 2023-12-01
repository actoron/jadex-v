package jadex.communication;

import java.nio.channels.SocketChannel;
import java.util.HashMap;

import jadex.collection.LeaseTimeMap;
import jadex.collection.RwMapWrapper;
import jadex.core.ComponentIdentifier;

public class UnixSocketStreamHandler implements IMessageSender
{
	private RwMapWrapper<Long, SocketChannel> connectioncache;
	
	//private RwMapWrapper<ComponentIdentifier, IComponent>
	
	private volatile static UnixSocketStreamHandler singleton;
	
	/**
	 *  Gets the singleton instance of the handler.
	 *  @return Singleton instance of the handler.
	 */
	public static final UnixSocketStreamHandler get()
	{
		if (singleton == null)
		{
			synchronized (UnixSocketStreamHandler.class)
			{
				if (singleton == null)
				{
					singleton = new UnixSocketStreamHandler();
				}
			}
		}
		return singleton;
	}
	
	/**
	 *  Creates a new UnixSocketStreamHandler.
	 */
	private UnixSocketStreamHandler()
	{
		 connectioncache = new RwMapWrapper<>(new LeaseTimeMap<>(900000, null, false, false, false));
	}
	
	/**
	 *  Sends a message to a component outside the current JVM.
	 *  
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public void sendMessage(ComponentIdentifier receiver, byte[] message)
	{
		
	}
	
}
