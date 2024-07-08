package jadex.ipc;

import java.nio.ByteBuffer;
import java.util.Scanner;

import jadex.common.SUtil;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.ipc.impl.IpcStreamHandler;

public class Test
{
	public static long a = System.currentTimeMillis();
	
	public static void main(String[] args) throws Exception
	{
		/*try
		{
			
			System.out.println("exec: " + SUtil.getExecutor() + " " + SUtil.isVirtualExecutor());
			System.out.println(System.getProperty("java.version"));
			Path socketdirpath = Path.of(System.getProperty("java.io.tmpdir")).resolve("jadexsockets");
			System.out.println("createdir " + socketdirpath.toFile().mkdir());
			
			String pidstr = String.valueOf(ProcessHandle.current().pid());
			
			Path socketpath = socketdirpath.resolve(pidstr);
			
			UnixDomainSocketAddress socketaddress = UnixDomainSocketAddress.of(socketpath);
			
			ServerSocketChannel channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
			
			channel.bind(socketaddress);
			
			SocketChannel conn = channel.accept();
			
			//DatagramChannel conn = DatagramChannel.open(StandardProtocolFamily.UNIX);
			//conn.bind(socketaddress);
			
			byte[] buf = new byte[20];
			conn.read(ByteBuffer.wrap(buf));
			String s = new String(buf, SUtil.UTF8);
			
			System.out.println(s);
			
			conn.close();
			
			//channel.close();
			
			Files.deleteIfExists(socketpath);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}*/
		
		/*IpcStreamHandler sh = IpcStreamHandler.get();
		System.out.println("PID: " + ComponentManager.get().pid());
		Scanner sc = new Scanner(System.in);
	    System.out.println("Enter a value :");
	    String str = sc.nextLine();
	    long rpid = Long.valueOf(str);
	    sh.sendMessage(new GlobalProcessIdentifier(rpid, ComponentManager.get().host()), ByteBuffer.wrap("Hello there".getBytes(SUtil.UTF8)));
	    System.out.println("User input: " + str);
	    
	    SUtil.sleep(10000);*/
		
	}
}
