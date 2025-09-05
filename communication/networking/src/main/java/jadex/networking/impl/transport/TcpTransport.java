package jadex.networking.impl.transport;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SBinConv;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.messaging.impl.SIOHelper;
import jadex.networking.impl.IMessageHandler;
import jadex.networking.impl.resolve.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class TcpTransport implements ITransport
{
    /** The bind address for incoming connections. */
    private InetAddress bindaddress;

    /** Port for listening. */
    private int port;

    /** Length of backlog queue. */
    private int backlog;

    /** The message handler for incoming messages. */
    private IMessageHandler messagehandler;

    /** Currently open TCP connections. */
    private RwMapWrapper<String, Socket> connections = new RwMapWrapper<>(new HashMap<>());

    /**
     *  Creates a new TCP transport.
     *  @param messagehandler The message handler for incoming messages.
     *  @param ip The IP address to bind.
     *  @param port Port to bind.
     *  @param backlog Number of incoming connection in backlog.
     */
    public TcpTransport(IMessageHandler messagehandler, String ip, int port, int backlog)
    {
        this.messagehandler = messagehandler;
        this.port = port;
        this.backlog = backlog;

        try
        {
            if (ip != null)
            {
                bindaddress = (InetAddress) Arrays.stream(InetAddress.getAllByName(ip)).findAny().orElse(null);
            }
        }
        catch (UnknownHostException e)
        {
            throw SUtil.throwUnchecked(e);
        }
    }

    /**
     *  Starts the transport server for incoming connections.
     */
    public void startServer()
    {
        SUtil.getExecutor().execute(getServerHandler(port, backlog));
    }

    /**
     *  Sends a message to a remote host.
     *
     *  @param receiver Message receiver.
     *  @param rawmessage The raw message.
     *  @return Null, when sent.
     */
    public IFuture<Void> sendMessage(ComponentIdentifier receiver, byte[] rawmessage)
    {
        Socket conn = connections.get(receiver.getGlobalProcessIdentifier().host());

        if (conn == null)
        {
            boolean v6 = bindaddress instanceof Inet6Address;
            Set<IEndpoint> endpoints = MultiResolver.get().resolve(receiver.getGlobalProcessIdentifier().host());
            if (endpoints != null)
            {
                endpoints = endpoints.stream().filter((ep) -> v6 ? ep instanceof TcpV6Endpoint : ep instanceof TcpV4Endpoint).collect(Collectors.toSet());

                byte[] hostbytes = GlobalProcessIdentifier.getSelf().host().getBytes(SUtil.UTF8);
                for (IEndpoint ep : endpoints)
                {
                    ITcpEndpoint tep = (ITcpEndpoint) ep;
                    try
                    {
                        conn = new Socket(tep.getAddress(), tep.getPort());
                        SIOHelper.writeChunk(conn.getOutputStream(), hostbytes);

                    }
                    catch (IOException e)
                    {
                        SUtil.close(conn);
                        conn = null;
                    }
                    if (conn != null)
                        break;
                }

                if (conn != null)
                {
                    try (IAutoLock l = connections.writeLock())
                    {
                        Socket existingconn = connections.get(receiver.getGlobalProcessIdentifier().host());

                        if (existingconn == null)
                        {
                            connections.put(receiver.getGlobalProcessIdentifier().host(), conn);
                        }
                        else
                        {
                            // Other Thread has already established the connection, dropping new one.
                            SUtil.close(conn);
                            conn = existingconn;
                        }
                    }
                }
            }
        }

        try
        {
            SIOHelper.writeChunk(conn.getOutputStream(), receiver.toString().getBytes(SUtil.UTF8));
            SIOHelper.writeChunk(conn.getOutputStream(), rawmessage);
            return IFuture.DONE;
        }
        catch (IOException e)
        {
            try (IAutoLock l = connections.writeLock())
            {
                // Implement retry if new connection already exists?
                if (conn == connections.get(receiver.getGlobalProcessIdentifier().host()))
                    connections.remove(receiver.getGlobalProcessIdentifier().host());
            }
            SUtil.close(conn);
        }

        return new Future<>(new IOException("TCP connection to " + receiver.getGlobalProcessIdentifier().host() + " failed."));
    }

    /**
     *  Gets the priority of the transport.
     *  @return Priority of the transport, higher means higher priority.
     */
    public int getPriority()
    {
        return bindaddress instanceof Inet6Address ? 60 : 40;
    }

    /**
     *  Sends a message via a socket.
     *  @param socket The socket.
     *  @param msg The message.
     * @throws IOException
     */
    private void sendMessage(Socket socket, byte[] msg) throws IOException
    {
        synchronized (socket)
        {
            OutputStream os = socket.getOutputStream();
            os.write(SBinConv.intToBytes(msg.length));
            os.write(msg);
            os.flush();
        }
    }

    private Runnable getServerHandler(int port, int backlog)
    {
        return () ->
        {
            try(ServerSocket serversocket = new ServerSocket(port, backlog, bindaddress))
            {
                while (!serversocket.isClosed())
                {
                    Socket incoming = serversocket.accept();

                    SUtil.getExecutor().execute(() ->
                    {
                        try
                        {
                            InputStream is = incoming.getInputStream();
                            byte[] sizebuf = new byte[4];
                            String remotehost = new String(SIOHelper.readChunk(is, sizebuf), SUtil.UTF8);

                            try (IAutoLock l = connections.writeLock())
                            {
                                Socket existing = connections.get(remotehost);
                                SUtil.close(existing);
                                connections.put(remotehost, incoming);
                            }

                            while (incoming.isConnected())
                            {
                                GlobalProcessIdentifier receiver = GlobalProcessIdentifier.fromString(new String(SIOHelper.readChunk(is, sizebuf), SUtil.UTF8));
                                byte[] rawmessage = SIOHelper.readChunk(is, sizebuf);
                                messagehandler.handleMessage(remotehost, receiver, rawmessage);
                            }
                        }
                        catch (IOException e)
                        {
                        }
                        finally
                        {
                            SUtil.close(incoming);
                        }
                    });
                }
            }
            catch (IOException e)
            {
            }
        };
    }
}
