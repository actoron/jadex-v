package jadex.networking.impl;

import jadex.collection.RwListWrapper;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.core.impl.ILifecycle;
import jadex.messaging.IIpcFeature;
import jadex.messaging.IMessageFeature;
import jadex.messaging.impl.INetworkTransport;
import jadex.messaging.impl.security.authentication.KeySecret;
import jadex.networking.INetworkFeature;
import jadex.networking.impl.transport.ITransport;
import jadex.networking.impl.transport.TcpTransport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;

/**
 *  Service providing network communication across hosts.
 */
public class NetworkFeature implements INetworkFeature, INetworkTransport, IMessageHandler, ILifecycle
{
    /** The default TCP port */
    public static final int DEFAULT_TCP_PORT = 5650;

    /** File used to communicate lock status for network configuration */
    private static final File LOCK_FILE = new File(IMessageFeature.COM_DIRECTORY_NAME + File.separator + "network.lck");

    private static final File CFG_FILE = new File(IMessageFeature.COM_DIRECTORY_NAME + File.separator + "network.cfg");

    /** Marker for the end-of-file for the network configuration. */
    private static final String EOF_MARKER="\n\n\n";

    /** The port used for TCP. */
    private int tcpport = DEFAULT_TCP_PORT;

    /** The IPv4 address to bind for incoming TCP connections. */
    private String ipv4bindaddress = "0.0.0.0";

    /** The IPv4 address to bind for incoming TCP connections. */
    private String ipv6bindaddress = "::";

    /** Maximum backlog of incoming TCP connections. */
    private int tcpbacklog = 50;

    /** PID of the local process responsible for remote transports. */
    private String nwpid;

    /** Transports currently in use. */
    private RwListWrapper<ITransport> transports = new RwListWrapper<>(new ArrayList<>());
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
        transports.add(new TcpTransport(this, ipv6bindaddress, tcpport, tcpbacklog));
        transports.add(new TcpTransport(this, ipv4bindaddress, tcpport, tcpbacklog));

        SUtil.getExecutor().execute(() ->
        {
            try
            {
                boolean nowarn = LOCK_FILE.createNewFile();

                RandomAccessFile nwfile = new RandomAccessFile(LOCK_FILE, "rw");

                System.out.println(ProcessHandle.current().pid() + " attempting networking lock.");

                lock = nwfile.getChannel().lock(0, Long.MAX_VALUE, false);

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

        String nwhost = null;
        try
        {
            for (int i = 0; i < 10; ++i)
            {
                String cfgstr = new String(SUtil.readFile(CFG_FILE), SUtil.UTF8);
                if (cfgstr.endsWith(EOF_MARKER))
                {
                    nwpid = cfgstr.trim();
                    break;
                }
                SUtil.sleep(100);
            }
        }
        catch (Exception e)
        {
            // Log?
        }
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
     *  Returns the maximum number of incoming TCP connection waiting in the backlog.
     *  @return The maximum number of incoming TCP connection waiting in the backlog.
     */
    public int getTcpBacklog()
    {
        return tcpbacklog;
    }

    /**
     *  Handle incoming message.
     *
     *  @param senderhost Sender of the message from the transport perspective.
     *  @param receiver Receiver of the message from the transport perspective.
     *  @param message The raw message.
     */
    public void handleMessage(String senderhost, ComponentIdentifier receiver, byte[] message)
    {
        if (receiver.getGlobalProcessIdentifier().pid() != null)
        {
            if (GlobalProcessIdentifier.getSelf().host().equals(receiver.getGlobalProcessIdentifier().host()))
            {
                IIpcFeature ipc = ComponentManager.get().getFeature(IIpcFeature.class);
                ipc.sendMessage(receiver, message);
            }
            else
            {
                // Spurious message, report or ignore?
            }
        }
        else
        {
            // Relaying mode

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