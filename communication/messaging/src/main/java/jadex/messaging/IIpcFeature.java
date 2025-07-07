package jadex.messaging;

import java.nio.file.Path;

import jadex.core.ComponentIdentifier;
import jadex.core.IRuntimeFeature;

/**
 *  Runtime feature implementing IPC communication on the same host.
 */
public interface IIpcFeature extends IRuntimeFeature
{
	/** Flag if the IpcFeature should attempt a clean up
	 *  of the IPC directory on startup.
	 */
	public static boolean PERFORM_CLEANUP = true;

	/**
	 *  Sends a message to a component outside the current JVM.
	 *  
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public void sendMessage(ComponentIdentifier receiver, byte[] message);
	
	/**
	 *  Sets the directory used for the domain socket IPC.
	 *  Note: This interrupts communication, ideally set this
	 *  efore starting components.
	 *  
	 *  @param dir The file system directory used for IPC.
	 */
	public void setSocketDirectory(Path dir);
}
