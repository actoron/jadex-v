package jadex.messaging.impl.ipc;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.Set;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SBinConv;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentManager;
import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.IIpcFeature;
import jadex.messaging.IMessageFeature;
import jadex.messaging.impl.MessageFeature;
import jadex.messaging.impl.SIOHelper;
import jadex.serialization.SerializationServices;

/**
 *  Class implementing IPC communication between JVMs on the same host
 *  using named pipes / FIFOs.
 */
public class IpcFeature implements IIpcFeature
{
	/** Flag if the IpcFeature should attempt a clean up
	 *  of the IPC directory on startup.
	 */
	public static boolean PERFORM_CLEANUP = true;

	/** Subdirectory used for IPC FIFOs */
	private static final String IPC_SUBDIR = "ipc";

	/** Directory used for IPC. */
	private Path socketdir;
	
	/** Path to the current socket */
	private Path socketpath;
	
	/** The server channel for incoming connections. */
	private ServerSocketChannel serverchannel = null;
	
	/** The local gpid */
	private GlobalProcessIdentifier gpid;
	
	/** Currently established connections. */
	private RwMapWrapper<String, LinkedBlockingQueue<ScheduledMessage>> connections;
	
	/** Expire unused connections after this timeout */
	private long connectiontimeout = 900000;
	
	/** Handler dealing with received messages. */
	private Consumer<byte[]> secmsghandler;

	private FileLock fifolock;
	
	/** Handler dealing with received messages. */
	private Consumer<ReceivedMessage> rcbmsghandler = (rmsg) ->
	{	
		try
		{
			IComponentHandle exta = ComponentManager.get().getComponentHandle(rmsg.receiver());
			exta.scheduleStep((comp) -> 
			{
				((MessageFeature) comp.getFeature(IMessageFeature.class)).externalMessageArrived(rmsg.origin(), rmsg.message);
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	};
	
	/**
	 *  Creates a new UnixSocketStreamHandler.
	 */
	public IpcFeature(GlobalProcessIdentifier gpid)
	{
		socketdir = Path.of(System.getProperty("java.io.tmpdir")).resolve(IMessageFeature.COM_DIRECTORY_NAME)
																 .resolve(IPC_SUBDIR);

		this.gpid = gpid;
		connections = new RwMapWrapper<>(new HashMap<>());
		socketdir.toFile().mkdirs();
		
		File dir = socketdir.toFile();
		if (!dir.isDirectory() || !dir.canRead() || !dir.canWrite())
			throw new UncheckedIOException(new IOException("Cannot access communcation directory: " + dir.getAbsolutePath()));
	}
	
	/**
	 *  Sends a message to a component outside the current JVM.
	 *  
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public void sendMessage(ComponentIdentifier receiver, byte[] message)
	{
		//System.out.println("IPC sending message: "+message+" to "+receiver);

		if (gpid.host().equals(receiver.getGlobalProcessIdentifier().host()))
		{
			ScheduledMessage smsg = new ScheduledMessage(receiver, message);
			LinkedBlockingQueue<ScheduledMessage> connection = null;
			try (IAutoLock l = connections.readLock())
			{
				connection = connections.get(receiver.getGlobalProcessIdentifier().pid());
				if (connection != null)
				{
					//System.out.println("IPC sending to1 "+receiver.getGlobalProcessIdentifier().pid());
					connection.add(smsg);
					return;
				}
				
			}
			
			connection = connect(receiver.getGlobalProcessIdentifier().pid());
			//System.out.println("IPC sending to2 "+receiver.getGlobalProcessIdentifier().pid());
			connection.add(smsg);
		}
		else
		{
			throw new UnsupportedOperationException("Remote messaging not supported yet.");
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
		socketdir.toFile().mkdirs();


		File fdir = socketdir.toFile();
		if (!fdir.isDirectory() || !fdir.canRead() || !fdir.canWrite())
			throw new UncheckedIOException(new IOException("Cannot access communcation directory: " + fdir.getAbsolutePath()));
		open();
	}
	
	/**
	 *  Sets the message handle for received security messages, overwriting the default.
	 *  @param secmsghandler The new message handler.
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
		if (IpcFeature.PERFORM_CLEANUP)
		{
			File[] files = socketdir.toFile().listFiles();
			Set<String> allpids = ProcessHandle.allProcesses().map(ProcessHandle::pid).map(String::valueOf).collect(Collectors.toSet());
			if (files != null)
			{
				Arrays.stream(files).filter( f -> f.getName().matches("[0-9]+")).forEach( f ->
				{
					if (!allpids.contains(f.getName()))
					{
						boolean tmp = f.delete();
					}
				});
			}
		}

		socketpath = socketdir.resolve(""+gpid.pid());
		try
		{
			if (socketpath.toFile().exists())
			{
				boolean tmp = socketpath.toFile().delete();
			}
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
	private LinkedBlockingQueue<ScheduledMessage> connect(String remotepid)
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
					os.write(SBinConv.intToBytes(gpidbytes.length));
					os.write(gpidbytes);
					
					while(true)
					{
						//System.out.println("IPC sending to "+remotepid);
						ScheduledMessage smsg = fqueue.poll(connectiontimeout, TimeUnit.MILLISECONDS);
						if (smsg != null)
						{
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							SerializationServices.get().encode(baos, cl, smsg.receiver());
							//byte[] encrec = ((Security) ComponentManager.get().getFeature(ISecurityFeatur
							SIOHelper.writeChunk(os, baos.toByteArray());
							SIOHelper.writeChunk(os, smsg.message());
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
					e.printStackTrace();
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
				while (readbytes < sizebuf.length)
					readbytes += is.read(sizebuf, readbytes, sizebuf.length - readbytes);
				
				GlobalProcessIdentifier origin = null;
				{
					readbytes = 0;
					byte[] msg = new byte[SBinConv.bytesToInt(sizebuf)];
					while (readbytes < msg.length)
						readbytes += is.read(msg, readbytes, msg.length - readbytes);
					
					String originstr = new String(msg, SUtil.UTF8);
					origin = GlobalProcessIdentifier.fromString(originstr);
				}
				
				while (channel.isConnected())
				{
					byte[] recbytes = SIOHelper.readChunk(is, sizebuf);
					//recbytes = ((Security) ComponentManager.get().getFeature(ISecurityFeature.class)).decryptAndAuth(origin, recbytes).message();

					Object o = serserv.decode(new ByteArrayInputStream(recbytes), cl);
					ComponentIdentifier receiver = (ComponentIdentifier) o;

					byte[] msg = SIOHelper.readChunk(is, sizebuf);
					
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
				e.printStackTrace();
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
