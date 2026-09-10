package jadex.networking.impl;

import jadex.collection.LRU;
import jadex.collection.RwListWrapper;
import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.core.impl.ILifecycle;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.messaging.ICatalogResolver;
import jadex.messaging.IIpcFeature;
import jadex.messaging.IMessageFeature;
import jadex.messaging.INetworkFeature;
import jadex.messaging.impl.ipc.IpcFeature;
import jadex.networking.impl.resolve.CatalogResolver;
import jadex.networking.impl.resolve.Resolver;
import jadex.networking.impl.transport.ITransport;
import jadex.networking.impl.transport.TcpTransport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 *  Service providing network communication across hosts.
 */
public class NetworkFeature implements INetworkFeature, ILifecycle
{
    /** The default TCP port */
    public static final int DEFAULT_TCP_PORT = 5650;

    /** File used to communicate lock status for network configuration */
    private static final File LOCK_FILE = Path.of(System.getProperty("java.io.tmpdir"))
                                              .resolve(IMessageFeature.COM_DIRECTORY_NAME)
											  .resolve("network.lck").toFile();
    // new File(IMessageFeature.COM_DIRECTORY_NAME + File.separator + "network.lck");

    private static final File CFG_FILE = Path.of(System.getProperty("java.io.tmpdir"))
                                              .resolve(IMessageFeature.COM_DIRECTORY_NAME)
											  .resolve("network.cfg").toFile();
    // new File(IMessageFeature.COM_DIRECTORY_NAME + File.separator + "network.cfg");

    /** Marker for the end-of-file for the network configuration. */
    private static final String EOF_MARKER="\n\n\n";

    /** The port used for TCP. */
    private int tcpport = DEFAULT_TCP_PORT;

    /** The host resolver. */
    private Resolver resolver = new Resolver();

    /** The IPv4 address to bind for incoming TCP connections. */
    private String ipv4bindaddress = "0.0.0.0";

    /** The IPv4 address to bind for incoming TCP connections. */
    private String ipv6bindaddress = "::";

    /** Maximum backlog of incoming TCP connections. */
    private int tcpbacklog = 50;

    /** Transports currently in use. */
    private RwListWrapper<ITransport> transports = new RwListWrapper<>(new ArrayList<>());

    private RwMapWrapper<String, ITransport> preferredtransports = new RwMapWrapper<>(new LRU<>(50));

    //private RwMapWrapper<String, ITransport> transports = new RwMapWrapper<>(new HashMap<>());

    /** Lock held on the lock file when process is handling networking. */
    private FileLock lock;

    /**
     *  Creates the network feature.
     */
    public NetworkFeature()
    {
    }

    /**
     *  Starts the feature, enabling servers if applicable.
     */
    public void init()
    {
        IMessageHandler msghndlr= new IMessageHandler()
        {
            public void handleMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] message)
            {
                IpcFeature ipc = (IpcFeature) ComponentManager.get().getFeature(IIpcFeature.class);
                ipc.handleIncomingMessage(origin, receiver, message);
            }
        };
        transports.add(new TcpTransport(resolver, msghndlr, ipv6bindaddress, tcpport, tcpbacklog));
        transports.add(new TcpTransport(resolver, msghndlr, ipv4bindaddress, tcpport, tcpbacklog));

        SUtil.getExecutor().execute(() ->
        {
            try
            {
                boolean nowarn = LOCK_FILE.createNewFile();

                RandomAccessFile nwfile = new RandomAccessFile(LOCK_FILE, "rw");

                System.out.println(ProcessHandle.current().pid() + " attempting networking lock.");

                lock = nwfile.getChannel().lock(0, Long.MAX_VALUE, false);

                System.out.println(ProcessHandle.current().pid() + " networking locked.");

                transports.forEach((transport) ->
                {
                    transport.startServer();
                });

                System.out.println(ProcessHandle.current().pid() + " servers active.");

                try (FileOutputStream fos = new FileOutputStream(CFG_FILE))
                {
                    fos.write((GlobalProcessIdentifier.getSelf() + EOF_MARKER).getBytes(SUtil.UTF8));
                    fos.flush();
                }
            }
            catch (Exception e)
            {
                // Give up, log?
            }
        });
    }

    /**
     *  Called in reverse order of features, when the component terminates.
     */
    public void	cleanup()
    {
    }

    /**
     *  Returns the configured TCP port to use.
     *  @return The TCP port.
     */
    public int getTcpPort()
    {
        return tcpport;
    }

    /**
     *  Sets TCP port to use instead of the default 5650.
     *  Warning: This can make it harder for remote Jadex instance to connect.
     *  @param tcpport The TCP port.
     */
    public void setTcpPort(int tcpport)
    {
        this.tcpport = tcpport;
    }

    /**
     *  Returns the IPv4 address to bind for incoming TCP connections.
     *  @return The IPv4 address to bind for incoming TCP connections.
     */
    public String getIpv4BindAddress()
    {
        return ipv4bindaddress;
    }

    /**
     *  Sets the IPv4 address to bind for incoming TCP connections.
     *  @param ipv4bindaddress The IPv4 address to bind for incoming TCP connections.
     */
    public void setIpv4BindAddress(String ipv4bindaddress)
    {
        this.ipv4bindaddress = ipv4bindaddress;
    }

    /**
     *  Returns the IPv6 address to bind for incoming TCP connections.
     *  @return The IPv6 address to bind for incoming TCP connections.
     */
    public String getIpv6BindAddress()
    {
        return ipv6bindaddress;
    }

    /**
     *  Sets the IPv6 address to bind for incoming TCP connections.
     *  @param ipv6bindaddress The IPv6 address to bind for incoming TCP connections.
     */
    public void setIpv6bindaddress(String ipv6bindaddress)
    {
        this.ipv6bindaddress = ipv6bindaddress;
    }

    /**
     *  Returns the catalog resolver for configuring fixed
     *  host -> IP resolving.
     * 
     *  @return The catalog resolver.
     */
    public ICatalogResolver getCatalogResolver()
    {
        return (ICatalogResolver) resolver.getResolver(CatalogResolver.class);
    }

    /**
     *  Returns the maximum number of incoming TCP connection waiting in the backlog.
     *  @return The maximum number of incoming TCP connection waiting in the backlog.
     */
    public int getTcpBacklog()
    {
        return tcpbacklog;
    }

    /**
	 *  Sends a message to a component using the network.
	 *  
     *  @param origin Origin of the message.
	 *  @param receiver The intended message receiver.
	 *  @param message The message.
	 */
	public IFuture<Void> sendMessage(GlobalProcessIdentifier origin, ComponentIdentifier receiver, byte[] message)
    {
        
        ITransport preferredtransport = preferredtransports.get(receiver.getGlobalProcessIdentifier().host());

        if (preferredtransport == null)
        {
            Future<Void> ret = new Future<>();
            try (IAutoLock l = transports.readLock())
            {
                FutureBarrier<Void> fubar = new FutureBarrier<>();
                for (ITransport transport :transports)
                {
                    IFuture<Void> sendfut = transport.sendMessage(origin, receiver, message);
                    fubar.add(sendfut);
                    sendfut.then((done) ->
                    {
                        ret.setResultIfUndone(null);
                        if (preferredtransports.get(receiver.getGlobalProcessIdentifier().host()) == null)
                        {
                            try (IAutoLock l2 = preferredtransports.writeLock())
                            {
                                if (preferredtransports.get(receiver.getGlobalProcessIdentifier().host()) == null)
                                {
                                    preferredtransports.put(receiver.getGlobalProcessIdentifier().host(), transport);
                                }
                            }
                        }
                    });
                }
                fubar.waitForResultsIgnoreFailures(null).then((results) -> 
                {
                    if (results == null || results.size() == 0)
                    {
                        ret.setException(new UncheckedIOException(new IOException("Unable to connect to host " + receiver)));
                    }
                });
            }
            return ret;
        }
        else
        {
            return preferredtransport.sendMessage(origin, receiver, message);
        }
    }

    public static void main(String[] args)
    {
        try
        {
            System.out.println(ProcessHandle.current().pid()+" has locked, waiting...");
            SUtil.sleep(60000);
            System.out.println(ProcessHandle.current().pid() + " is terminating.");
        }
        catch (Exception e)
        {
        }
    }
}