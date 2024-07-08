package jadex.ipc.impl;

import java.nio.ByteBuffer;

import org.bouncycastle.pqc.jcajce.provider.util.SpecUtil;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.ipc.impl.IpcStreamHandler;

public class Main
{
	public static void main(String[] args)
	{
		
		SUtil.DEBUG = true;
		IpcStreamHandler ipc1 = new IpcStreamHandler();
		ipc1.open("1000000001");
		ipc1.setReceivedMessageHandler((rcv,msg) -> 
		{
			System.out.println("IPC1 received message from " + rcv);
		});
		
		IpcStreamHandler ipc2 = new IpcStreamHandler();
		ipc2.open("1000000002");
		ipc2.setReceivedMessageHandler((rcv,msg) -> 
		{
			System.out.println("IPC2 received message from " + rcv);
		});
		
		ipc1.sendMessage(new ComponentIdentifier("test", new GlobalProcessIdentifier(1000000002, GlobalProcessIdentifier.SELF.host())), ByteBuffer.wrap("This is a test".getBytes(SUtil.UTF8)));
	}
}