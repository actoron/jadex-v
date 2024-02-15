package jadex.messaging.impl;

/**
 *  Provider for the IPC service to allow access as part
 *  of the Java Service Interface.
 */
public abstract class IpcProvider
{
	/**
	 *  Acquires the interprocess communication service (IpcService)
	 *  
	 *  @return The interprocess communication service (IpcService)
	 */
	public abstract IIpcService getIpcService();
}
