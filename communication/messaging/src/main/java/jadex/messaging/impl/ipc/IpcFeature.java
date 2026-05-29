package jadex.messaging.impl.ipc;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.messaging.IIpcFeature;
import jadex.messaging.IMessageFeature;
import jadex.messaging.INetworkFeature;
import jadex.messaging.impl.MessageFeature;
import jadex.messaging.impl.SIOHelper;

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
	private RwMapWrapper<String, LinkedBlockingQueue<TransportMessage>> connections;
	
	/** Expire unused connections after this timeout */
	private long connectiontimeout = 900000;
	
	/** Handler dealing with received messages. */
	private Consumer<byte[]> secmsghandler;

	/** PID of the process handling network traffic. */
	private String networkprocess;
	
	/** Handler dealing with received messages. */
	private Consumer<TransportMessage> rcbmsghandler = (rmsg) ->
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
			forwardMessage(gpid, receiver, message);
		}
		else
		{
			String nwp = getNetworkPid();

			if (nwp != null)
			{
				forwardMessage(gpid, receiver, message, nwp);
			}
			else
				throw new UnsupportedOperationException("Remote messaging not available.");
			

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
	public void setReceivedMessageHandler(Consumer<TransportMessage> rcvmsghandler)
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
	private LinkedBlockingQueue<TransportMessage> connect(String remotepid)
	{
		LinkedBlockingQueue<TransportMessage> queue = null;
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
			LinkedBlockingQueue<TransportMessage> fqueue = new LinkedBlockingQueue<>();
			queue = fqueue;
			SUtil.getExecutor().execute(() -> {
				OutputStream os = Channels.newOutputStream(fchannel);
				
				try
				{	
					while(true)
					{
						//System.out.println("IPC sending to "+remotepid);
						TransportMessage smsg = fqueue.poll(connectiontimeout, TimeUnit.MILLISECONDS);
						if (smsg != null)
						{
							byte[] strbuf = smsg.origin().toString().getBytes(SUtil.UTF8);
							SIOHelper.writeChunk(os, strbuf);
							strbuf = smsg.receiver().toString().getBytes(SUtil.UTF8);
							SIOHelper.writeChunk(os, strbuf);
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
						LinkedBlockingQueue<TransportMessage> newqueue = connect(remotepid);
						do
						{
							TransportMessage smsg = fqueue.poll();
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
	 *  Handles incoming messages.
	 * 
	 * @param origin Message origin.
	 * @param receiver The message receiver.
	 * @param msg The message.
	 */
	public void handleIncomingMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] msg)
	{
		if (gpid.host().equals(receiver.getGlobalProcessIdentifier().host()))
		{
			if (gpid.pid().equals(receiver.getGlobalProcessIdentifier().pid()))
			{
				if (receiver.getLocalName() != null)
				{
					rcbmsghandler.accept(new TransportMessage(origin, receiver, msg));
				}
				else if (secmsghandler != null)
				{
					secmsghandler.accept(msg);
				}
			}
			else
				forwardMessage(origin, receiver, msg);
		}
		else
		{
			if (GlobalProcessIdentifier.getSelf().pid().equals(getNetworkPid()))
			{
				INetworkFeature nwfeat = ComponentManager.get().getFeature(INetworkFeature.class);
				nwfeat.sendMessage(origin, receiver, msg);
			}
			else
				ComponentManager.get().getLogger(this.getClass()).log(Level.WARNING, "IPC received message for foreign host: " + receiver + ", discarding...");
		}
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
			byte[] sizebuf = new byte[4];
			try
			{
				while (channel.isConnected())
				{
					GlobalProcessIdentifier origin = GlobalProcessIdentifier.fromString(new String(SIOHelper.readChunk(is, sizebuf), SUtil.UTF8));
					ComponentIdentifier receiver = ComponentIdentifier.fromString(new String(SIOHelper.readChunk(is, sizebuf), SUtil.UTF8));
					byte[] msg = SIOHelper.readChunk(is, sizebuf);
					handleIncomingMessage(origin, receiver, msg);
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
	 *  Gets the current network PID.
	 *  
	 *  @return PID of the process handling network traffic.
	 */
	private String getNetworkPid()
	{
		if (networkprocess == null)
		{
			synchronized(this)
			{
				if (networkprocess == null)
				{
					GlobalProcessIdentifier nwpid = readNetworkProcess();
					if (nwpid != null)
						networkprocess = nwpid.pid();
					else
						networkprocess = null;
				}
			}
		}

		return networkprocess;
	}

	/**
     *  Reads the current process handling network traffic from the config file.
     * @return
     */
    public GlobalProcessIdentifier readNetworkProcess()
    {
        GlobalProcessIdentifier ret = null;
		File conffile = Path.of(System.getProperty("java.io.tmpdir"))
                                      .resolve(IMessageFeature.COM_DIRECTORY_NAME)
									  .resolve("network.cfg").toFile();
        try (FileInputStream fis = new FileInputStream(conffile))
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            ret = GlobalProcessIdentifier.fromString(br.readLine());
        }
        catch(Exception e)
        {
			e.printStackTrace();
        }
        return ret;
    }

	/**
	 *  Forwards a message to a different process.
	 *  Will forward to receiver directly.
	 * 
	 *  @param origin Origin of the message.
	 *  @param receiver Receiver of the message.
	 *  @param message The message.
	 */
	private void forwardMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] message)
	{
		forwardMessage(origin, receiver, message, receiver.getGlobalProcessIdentifier().pid());
	}

	/**
	 *  Forwards a message to a different process.
	 * 
	 *  @param origin Origin of the message.
	 *  @param receiver Receiver of the message.
	 *  @param message The message.
	 *  @param targetpid PID of the process to forward the message to.
	 */
	private void forwardMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] message, String targetpid) {
		TransportMessage smsg = new TransportMessage(origin, receiver, message);
		LinkedBlockingQueue<TransportMessage> connection = null;
		try (IAutoLock l = connections.readLock())
		{
			connection = connections.get(targetpid);
			if (connection != null)
			{
				//System.out.println("IPC sending with existing conn to "+receiver.getGlobalProcessIdentifier().pid());
				connection.add(smsg);
				return;
			}
		}
		
		//System.out.println("IPC sending to with new conn "+receiver.getGlobalProcessIdentifier().pid() + " from " + gpid);
		connection = connect(targetpid);
		connection.add(smsg);
	}
	
	/**
	 *  A message that is being transported.
	 */
	public static record TransportMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] message) {};
}
