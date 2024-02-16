package jadex.communication.impl;

import java.nio.ByteBuffer;
import java.util.ServiceLoader;

import jadex.core.ComponentIdentifier;
import jadex.messaging.impl.IIpcService;
import jadex.messaging.impl.IpcProvider;

/**
 *  Implementation of the IpcProvider.
 */
public class IpcProviderImpl extends IpcProvider
{
	/**
	 *  Acquires the interprocess communication service (IpcService)
	 *  
	 *  @return The interprocess communication service (IpcService)
	 */
	public IIpcService getIpcService()
	{
		return IpcStreamHandler.get();
	}
}
