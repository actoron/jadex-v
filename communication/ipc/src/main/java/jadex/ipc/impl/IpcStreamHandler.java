package jadex.ipc.impl;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jadex.collection.LeaseTimeMap;
import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.ComponentManager;
import jadex.ipc.impl.security.Security;
import jadex.ipc.impl.security.random.SecureThreadedRandom;
import jadex.messaging.impl.IIpcService;

public class IpcStreamHandler implements IIpcService
{
	/** Name of the IPC/Socket directory. */
	private static final String IPC_DIRECTORY_NAME = "jadexipc";
	
	/** Directory used for IPC. */
	private static Path socketdir;
	static
	{
		socketdir = Path.of(System.getProperty("java.io.tmpdir")).resolve(IPC_DIRECTORY_NAME);
		System.out.println(System.getProperty("java.io.tmpdir"));
	}
	
	/**
	 *  Sets the directory for the domain socket IPC.
	 *  Must be called before any component is started.
	 *  
	 *  @param dir The file system directory used for IPC.
	 */
	public static final void setSocketDirectory(Path dir)
	{
		if (singleton != null) {
			throw new IllegalStateException("Communication is already running. Set socket directory before communication start.");
		}
			
		socketdir = dir;
	}
	
	/** If a connection was requested, timeout for awaiting that connection. */
	private static final int CONNECTION_WAIT_TIMEOUT = 3000;
	
	/** Currently established connections. */
	private RwMapWrapper<Long, SocketChannel> connectioncache;
	
	/** Latch that is triggered (released) if a new connection was established. */
	private CountDownLatch connectionlatch = new CountDownLatch(1);
	
	/**
	 *  Does some global initialization when the service is requested the first time.
	 *  Replaces the SUtil default secure random with one that has higher performace.
	 */
	static
	{
		SUtil.SECURE_RANDOM = new SecureThreadedRandom();
	}
	
	/** Singleton instance used for communication. */
	private volatile static IpcStreamHandler singleton;
	
	/**
	 *  Gets the singleton instance of the handler.
	 *  @return Singleton instance of the handler.
	 */
	public static final IpcStreamHandler get()
	{
		if (singleton == null)
		{
			synchronized (IpcStreamHandler.class)
			{
				if (singleton == null)
				{
					singleton = new IpcStreamHandler();
					singleton.open();
				}
			}
		}
		return singleton;
	}
	
	/**
	 *  Creates a new UnixSocketStreamHandler.
	 */
	public IpcStreamHandler()
	{
		LeaseTimeMap<Long, SocketChannel> ltm = new LeaseTimeMap<>(900000, true);
		ltm.setTouchOnRead(true);
		ltm.setTouchOnWrite(true);
		ltm.setRawRemoveCommand((map, entry) ->
		{
			try (IAutoLock l = connectioncache.writeLock())
			{
				//System.out.println("Lease time expired for " + entry.getFirstEntity());
				SocketChannel removedchan = map.remove(entry.getFirstEntity());
				SUtil.close(removedchan);
			}
		});
		
		connectioncache = new RwMapWrapper<>(ltm);
		socketdir.toFile().mkdirs();
		
		File dir = socketdir.toFile();
		if (!dir.isDirectory() || !dir.canRead() || !dir.canWrite())
			throw new UncheckedIOException(new IOException("Cannot access socket directory: " + dir.getAbsolutePath()));
	}
	
	/**
	 *  Sends a message to a component outside the current JVM.
	 *  
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public void sendMessage(ComponentIdentifier receiver, ByteBuffer message)
	{
		if (ComponentManager.get().host().equals(receiver.getGlobalProcessIdentifier().host()))
		{
			SocketChannel connection = null;
			
			long receiverpid = receiver.getGlobalProcessIdentifier().pid();
			connection = connectioncache.get(receiverpid);
			if (connection == null)
				connection = connect(receiverpid);
			
			try {
				if (connection != null)
				{
					ByteBuffer sizebuf = ByteBuffer.allocate(4);
					if (receiver.getLocalName() != null)
					{
						byte[] namearr = receiver.getLocalName().getBytes(SUtil.UTF8);
						namearr = Security.get().encryptAndSign(receiver.getGlobalProcessIdentifier(), namearr);
						sizebuf.putInt(namearr.length);
						sizebuf.rewind();
						ByteBuffer namebuf = ByteBuffer.wrap(namearr);
						connection.write(sizebuf);
						connection.write(namebuf);
					}
					else
					{
						sizebuf.putInt(0);
						sizebuf.rewind();
						connection.write(sizebuf);
					}
					
					sizebuf.rewind();
					sizebuf.putInt(message.remaining());
					sizebuf.rewind();
					connection.write(sizebuf);
					connection.write(message);
				}
				else
					throw new IOException("Could not connect to " + receiver.getGlobalProcessIdentifier());
			}
			catch (IOException e)
			{
				throw SUtil.throwUnchecked(e);
			}
			
		}
		else
		{
			// TODO: Remote
		}
	}
	
	/**
	 *  Opens a socket allowing incoming connections.
	 */
	protected void open()
	{
		String pidstr = String.valueOf(ComponentManager.get().pid());
		open(pidstr);
	}
	
	/**
	 *  Opens a socket allowing incoming connections.
	 *  
	 *  @param pidstr PID string to use.
	 */
	protected void open(String pidstr)
	{
		SUtil.getExecutor().execute(() -> {
			try
			{
				Path socketpath = socketdir.resolve(pidstr);
				
				UnixDomainSocketAddress socketaddress = UnixDomainSocketAddress.of(socketpath);
				ServerSocketChannel serverchannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
				serverchannel.configureBlocking(true);
				serverchannel.bind(socketaddress);
				
				while (true)
				{
					try
					{
						System.out.println("Waiting for new connections");
						SocketChannel conn = serverchannel.accept();
						System.out.println("New connection, handling...");
						handleNewConnection(conn);
					}
					catch (IOException e)
					{
						throw SUtil.throwUnchecked(e);
					}
				}
			}
			catch(IOException e)
			{
				throw new UncheckedIOException(e);
			}
		});
	}
	
	private SocketChannel connect(long remotepid)
	{
		SocketChannel channel = null;
		if (isConnector(remotepid))
		{
			try
			{
				channel = SocketChannel.open(StandardProtocolFamily.UNIX);
				Path remotesocketpath = socketdir.resolve(String.valueOf(remotepid));
				UnixDomainSocketAddress remotesocketaddress = UnixDomainSocketAddress.of(remotesocketpath);
				channel.connect(remotesocketaddress);
				ByteBuffer numbuf = ByteBuffer.allocate(8);
				numbuf.putLong(ComponentManager.get().pid());
				numbuf.rewind();
				channel.write(numbuf);
				addConnection(remotepid, channel);
			}
			catch (IOException e)
			{
				channel = null;
				SUtil.throwUnchecked(e);
			}
		}
		else
		{
			try
			{
				SocketChannel tmpchannel = SocketChannel.open(StandardProtocolFamily.UNIX);
				Path remotesocketpath = socketdir.resolve(String.valueOf(remotepid));
				UnixDomainSocketAddress remotesocketaddress = UnixDomainSocketAddress.of(remotesocketpath);
				tmpchannel.connect(remotesocketaddress);
				ByteBuffer numbuf = ByteBuffer.allocate(8);
				numbuf.putLong(ComponentManager.get().pid());
				numbuf.rewind();
				
				// Triggered the other side to connect to us, waiting for connection...
				CountDownLatch cl = connectionlatch;
				tmpchannel.write(numbuf);
				SUtil.close(tmpchannel);
				
				long timeout = CONNECTION_WAIT_TIMEOUT;
				long timestamp = System.currentTimeMillis();
				try
				{
					while (channel == null && timeout > 0)
					{
						cl.await(timeout, TimeUnit.MILLISECONDS);
						timeout -= (System.currentTimeMillis() - timestamp);
						channel = connectioncache.get(remotepid);
					}
				}
				catch (InterruptedException e)
				{
				}
			}
			catch (IOException e)
			{
				SUtil.throwUnchecked(e);
			}
		}
		
		if (channel == null)
			throw new UncheckedIOException(new IOException("Failed to establish connection to " + remotepid + "."));
		
		return channel;
	}
	
	private void handleNewConnection(SocketChannel channel)
	{
		SUtil.getExecutor().execute(() ->
		{
			try
			{
				long pid = ComponentManager.get().pid();
				channel.configureBlocking(true);
				long remotepid = 0;
				SocketChannel remotechannel = null;
				ByteBuffer numbuf = ByteBuffer.allocate(8);
				
				// Read remote PID
				while (numbuf.hasRemaining())
					channel.read(numbuf);
				numbuf.rewind();
				remotepid = numbuf.getLong();
				
				if (isConnector(remotepid))
				{
					// Close requesting connection...
					SUtil.close(channel);
					
					// Establish new connection
					SocketChannel newchannel = connect(remotepid);
					remotechannel = newchannel;
				}
				else
				{
					remotechannel = channel;
					addConnection(remotepid, remotechannel);
				}
				
				if (remotechannel != null)
				{
					handleMessages(remotepid, remotechannel);
				}
			}
			catch (IOException e)
			{
				SUtil.close(channel);
			}
		});
	}
	
	private void handleMessages(long remotepid, SocketChannel channel)
	{
		SUtil.getExecutor().execute(() -> {
			System.out.println("handling messages");
			ByteBuffer numbuf = ByteBuffer.allocate(4);
			ByteBuffer buf = null;
			try
			{
				byte[] localname = readChunk(channel);
				byte[] message = readChunk(channel);
				
				if (localname == null)
				{
					Security.get().handleMessage(message);
				}
				else
				{
					// TODO: Component handover
				}
			}
			catch (Exception e)
			{
				SUtil.close(channel);
			}
		});
	}
	
	private void addConnection(long remoteip, SocketChannel channel)
	{
		try (IAutoLock l = connectioncache.writeLock())
		{
			SocketChannel old = connectioncache.get(remoteip);
			SUtil.close(old);
			connectioncache.put(remoteip, channel);
		}
		
		// Signal a new connection via latch...
		connectionlatch.countDown();
		connectionlatch = new CountDownLatch(1);
	}
	
	/**
	 *  Reads a chunk from the channel that is prefixed with its size.
	 * 
	 *  @param channel The channel.
	 *  @return The bytes read or null if size was zero.
	 *  @throws Exception If an error occurred.
	 */
	private byte[] readChunk(SocketChannel channel) throws Exception
	{
		ByteBuffer numbuf = ByteBuffer.allocate(4);
		ByteBuffer buf = null;
		boolean notdone = true;
		
		while (notdone)
		{
			if (buf == null)
			{
				System.out.println("Reading msg size");
				int ds = channel.read(numbuf);
				if (ds < 0)
					throw new EOFException("Connection closed");
				System.out.println("Got msg size" + ds + " " + numbuf.hasRemaining());
				if (!numbuf.hasRemaining())
				{
					numbuf.rewind();
					int size = numbuf.getInt();
					numbuf.rewind();
					
					if (size > 0)
						buf = ByteBuffer.wrap(new byte[size]);
					else
						notdone = false;
				}
			}
			else
			{
				channel.read(buf);
				if (!buf.hasRemaining())
					notdone = false;
			}
		}
		
		return buf.array();
	}
	
	/**
	 *  Tests if the process should directly connect to the remote process (true)
	 *  or if it should ask to for the remote process to connect back.
	 *  
	 *  @param remotepid Remote pid;
	 *  @return True, if the process should directly connect to the remote process.
	 */
	private boolean isConnector(long remotepid)
	{
		if (remotepid > ComponentManager.get().pid())
			return true;
		return false;
	}
}
