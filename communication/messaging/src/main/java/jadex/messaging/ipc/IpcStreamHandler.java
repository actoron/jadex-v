package jadex.messaging.ipc;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentManager;
import jadex.core.IExternalAccess;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.IIpcFeature;
import jadex.messaging.IMessageFeature;
import jadex.messaging.impl.MessageFeature;
import jadex.serialization.SerializationServices;

/**
 *  Class implementing IPC communication between JVMs on the same host
 *  using named pipes / FIFOs.
 */
public class IpcStreamHandler implements IIpcFeature
{
	/** Name of the IPC/Socket directory. */
	private static final String IPC_DIRECTORY_NAME = "jadexipc";
	
	/** Directory used for IPC. */
	private Path socketdir;
	
	/** Path to the current socket */
	private Path socketpath;
	
	/** The server channel for incoming connections. */
	private ServerSocketChannel serverchannel = null;
	
	/** The local gpid */
	private GlobalProcessIdentifier gpid;
	
	/** Currently established connections. */
	private RwMapWrapper<Long, LinkedBlockingQueue<ScheduledMessage>> connections;
	
	/** Expire unused connections after this timeout */
	private long connectiontimeout = 900000;
	
	/** Singleton instance used for communication. */
	private volatile static IpcStreamHandler singleton;
	
	/**
	 *  Gets the singleton instance of the handler.
	 *  @return Singleton instance of the handler.
	 */
	/*public static final IpcStreamHandler get()
	{
		if (singleton == null)
		{
			synchronized (IpcStreamHandler.class)
			{
				if (singleton == null)
				{
					singleton = new IpcStreamHandler(GlobalProcessIdentifier.SELF);
					singleton.open();
				}
			}
		}
		return singleton;
	}*/
	
	/** Handler dealing with received messages. */
	private Consumer<byte[]> secmsghandler;
	
	/** Handler dealing with received messages. */
	private Consumer<ReceivedMessage> rcbmsghandler = (rmsg) ->
	{
		IExternalAccess exta = ComponentManager.get().getComponent(rmsg.receiver()).getExternalAccess();
		exta.scheduleStep((comp) -> 
		{
			((MessageFeature) comp.getFeature(IMessageFeature.class)).externalMessageArrived(rmsg.origin(), rmsg.message);
		});
	};
	
	/**
	 *  Creates a new UnixSocketStreamHandler.
	 */
	public IpcStreamHandler(GlobalProcessIdentifier gpid)
	{
		socketdir = Path.of(System.getProperty("java.io.tmpdir")).resolve(IPC_DIRECTORY_NAME);
		
		Runtime.getRuntime().addShutdownHook(new Thread(() -> 
		{
			close();
		}));
		
		this.gpid = gpid;
		connections = new RwMapWrapper<>(new HashMap<>());
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
	public void sendMessage(ComponentIdentifier receiver, byte[] message)
	{
		if (gpid.host().equals(receiver.getGlobalProcessIdentifier().host()))
		{
			ScheduledMessage smsg = new ScheduledMessage(receiver, message);
			LinkedBlockingQueue<ScheduledMessage> connection = null;
			try (IAutoLock l = connections.readLock())
			{
				connection = connections.get(receiver.getGlobalProcessIdentifier().pid());
				if (connection != null)
				{
					connection.add(smsg);
					return;
				}
				
			}
			
			connection = connect(receiver.getGlobalProcessIdentifier().pid());
			connection.add(smsg);
		}
		else
		{
			// TODO: Remote
		}
	}
	
	/**
	 *  Sets the directory used for the domain socket IPC.
	 *  
	 *  @param dir The file system directory used for IPC.
	 */
	public void setSocketDirectory(Path dir)
	{
		close();
		socketdir = dir;
		open();
	}
	
	/**
	 *  Sets the message handle for received security messages, overwriting the default.
	 *  @param rcvmsghandler The new message handler.
	 */
	public void setSecurityMessageHandler(Consumer<byte[]> secmsghandler)
	{
		this.secmsghandler = secmsghandler;
	}
	
	/**
	 *  Sets the message handle for received messages, overwriting the default.
	 *  @param rcvmsghandler The new message handler.
	 */
	public void setReceivedMessageHandler(Consumer<ReceivedMessage> rcvmsghandler)
	{
		this.rcbmsghandler = rcvmsghandler;
	}
	
	/**
	 *  Opens a socket allowing incoming connections.
	 */
	public void open()
	{
		socketpath = socketdir.resolve(""+gpid.pid());
		try
		{
			if (socketpath.toFile().exists())
				socketpath.toFile().delete();
			socketpath.toFile().deleteOnExit();
			
			UnixDomainSocketAddress socketaddress = UnixDomainSocketAddress.of(socketpath);
			serverchannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
			serverchannel.configureBlocking(true);
			serverchannel.bind(socketaddress);
		}
		catch(IOException e)
		{
			throw new UncheckedIOException(e);
		}
		
		SUtil.getExecutor().execute(() -> {
			while (true)
			{
				try
				{
					SocketChannel conn = serverchannel.accept();
					handleNewConnection(conn);
				}
				catch (IOException e)
				{
					// No use re-throwing exception, cannot be handled properly.
					// Likely reason for exceptions: JVM is terminating.
					return;
				}
			}
		});
	}
	
	/**
	 *  Closes the IPC.
	 */
	public void close()
	{
		if (serverchannel != null)
		{
			try
			{
				serverchannel.close();
			}
			catch (Exception e) {};
			socketpath.toFile().delete();
		}
	}
	
	/**
	 *  Connects to a remote pid
	 *  @param remotepid The remot pid.
	 *  @return Queue for sending messages.
	 */
	private LinkedBlockingQueue<ScheduledMessage> connect(long remotepid)
	{
		LinkedBlockingQueue<ScheduledMessage> queue = null;
		
		SocketChannel channel = null;
		try
		{
			channel = SocketChannel.open(StandardProtocolFamily.UNIX);
			Path remotesocketpath = socketdir.resolve(String.valueOf(remotepid));
			UnixDomainSocketAddress remotesocketaddress = UnixDomainSocketAddress.of(remotesocketpath);
			channel.connect(remotesocketaddress);
		}
		catch (IOException e)
		{
			channel = null;
			e.printStackTrace();
		}
		
		try (IAutoLock l = connections.writeLock())
		{
			queue = connections.get(remotepid);
			if (queue != null)
			{
				if (channel != null)
					SUtil.close(channel);
				
				return queue;
			}
			
			if (channel == null)
				throw new UncheckedIOException(new IOException("Failed to establish connection to " + remotepid + "."));
			
			SocketChannel fchannel = channel;
			LinkedBlockingQueue<ScheduledMessage> fqueue = new LinkedBlockingQueue<>();
			queue = fqueue;
			SUtil.getExecutor().execute(() -> {
				OutputStream os = Channels.newOutputStream(fchannel);
				ClassLoader cl = IComponentManager.get().getClassLoader();
				
				try
				{
					String gpidstr = gpid.toString();
					byte[] gpidbytes = gpidstr.getBytes(SUtil.UTF8);
					os.write(SUtil.intToBytes(gpidbytes.length));
					os.write(gpidbytes);
					
					while(true)
					{
						ScheduledMessage smsg = fqueue.poll(connectiontimeout, TimeUnit.MILLISECONDS);
						if (smsg != null)
						{
							SerializationServices.get().encode(os, cl, smsg.receiver());
							os.write(SUtil.intToBytes(smsg.message.length));
							os.write(smsg.message);
							os.flush();
						}
						else
						{
							throw new EOFException();
						}
					}
				}
				catch (Exception e)
				{
					connections.remove(remotepid, fqueue);
					if (!fqueue.isEmpty())
					{
						LinkedBlockingQueue<ScheduledMessage> newqueue = connect(remotepid);
						do
						{
							ScheduledMessage smsg = fqueue.poll();
							newqueue.add(smsg);
						}
						while (!fqueue.isEmpty());
					}
					
					SUtil.close(os);
					return;
				}
			});
			connections.put(remotepid, fqueue);
		}
		
		return queue;
	}
	
	/**
	 *  Deals with a new incoming connection.
	 *  @param channel The new channel that has been opened.
	 */
	private void handleNewConnection(SocketChannel channel)
	{
		SUtil.getExecutor().execute(() ->
		{
			InputStream is = Channels.newInputStream(channel);
			ClassLoader cl = IComponentManager.get().getClassLoader();
			SerializationServices serserv = SerializationServices.get();
			int readbytes = 0;
			byte[] sizebuf = new byte[4];
			try
			{
				readbytes = 0;
				while (readbytes < sizebuf.length)
					readbytes += is.read(sizebuf, readbytes, sizebuf.length - readbytes);
				
				GlobalProcessIdentifier origin = null;
				{
					readbytes = 0;
					byte[] msg = new byte[SUtil.bytesToInt(sizebuf)];
					while (readbytes < msg.length)
						readbytes += is.read(msg, readbytes, msg.length - readbytes);
					
					String originstr = new String(msg, SUtil.UTF8);
					origin = GlobalProcessIdentifier.fromString(originstr);
				}
				
				while (channel.isConnected())
				{
					Object o = serserv.decode(is, cl);
					ComponentIdentifier receiver = (ComponentIdentifier) o;
					
					readbytes = 0;
					while (readbytes < sizebuf.length)
						readbytes += is.read(sizebuf, readbytes, sizebuf.length - readbytes);
					
					readbytes = 0;
					byte[] msg = new byte[SUtil.bytesToInt(sizebuf)];
					while (readbytes < msg.length)
						readbytes += is.read(msg, readbytes, msg.length - readbytes);
					
					if (receiver.getLocalName() != null)
					{
						rcbmsghandler.accept(new ReceivedMessage(origin, receiver, msg));
					}
					else if (secmsghandler != null)
					{
						secmsghandler.accept(msg);
					}
				}
				SUtil.close(is);
			}
			catch (Exception e)
			{
				SUtil.close(is);
			}
		});
	}
	
	/**
	 *  A message that has been scheduled for transfer.
	 */
	private static record ScheduledMessage(ComponentIdentifier receiver, byte[] message) {};
	
	/**
	 *  A message that has been received.
	 */
	public static record ReceivedMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] message) {};
}
