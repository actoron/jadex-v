package jadex.messaging.ipc;

import java.nio.file.Path;
import java.util.Arrays;

import jadex.messaging.IIpcFeature;
import jadex.messaging.impl.ipc.IpcFeature;
import org.junit.jupiter.api.Test;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.GlobalProcessIdentifier;
import jadex.future.Future;
import jadex.messaging.ISecurityFeature.DecodedMessage;
import jadex.messaging.impl.security.authentication.KeySecret;
import jadex.messaging.impl.security.authentication.PasswordSecret;
import jadex.messaging.impl.security.SSecurity;
import jadex.messaging.impl.security.SecurityFeature;

/**
 *  Test for the IPC subsystem.
 */
public class IpcTest
{
	/** Random IPC directory */
	private Path ipcdir;
	
	/**
	 *  Initializes the test.
	 */
	public IpcTest()
	{
		IpcFeature.PERFORM_CLEANUP = false;
		byte[] bytes = new byte[16];
		SUtil.FAST_RANDOM.nextBytes(bytes);
		
		ipcdir = Path.of(System.getProperty("java.io.tmpdir")).resolve("ipc_test_"+SUtil.hex(bytes));
		ipcdir.toFile().mkdir();
		ipcdir.toFile().deleteOnExit();
		System.out.println("IPC Test Path is " + ipcdir);
	}
	
	/**
	 * Self-Test.
	 * 
	 * @param args Arguments.
	 */
	public static final void main(String[] args)
	{
		IpcTest t = new IpcTest();
		try
		{
			t.testBasicIpc();
			t.testSecurity();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 *  Test basic IPC.
	 */
	@Test
	public void testBasicIpc() throws Exception
	{
		Future<Void> done1 = new Future<>();
		ComponentIdentifier cid1 = getIpcComponentTestId("test1");
		IpcFeature ipc1 = getIpcStreamHandler(cid1);
		ipc1.setReceivedMessageHandler((rcvmsg) -> 
		{
			System.out.println(cid1.getLocalName() + " received message from " + rcvmsg.origin() + ": " + new String(rcvmsg.message(), SUtil.UTF8));
			done1.setResultIfUndone(null);
		});
		
		Future<Void> done2 = new Future<>();
		ComponentIdentifier cid2 = getIpcComponentTestId("test2");
		IpcFeature ipc2 = getIpcStreamHandler(cid2);
		ipc2.setReceivedMessageHandler((rcvmsg) -> 
		{
			System.out.println(cid2.getLocalName() + " received message from " + rcvmsg.origin() + ": " + new String(rcvmsg.message(), SUtil.UTF8));
			done2.setResultIfUndone(null);
		});
		
		byte[] hw = "Hello World!".getBytes(SUtil.UTF8);
		ipc2.sendMessage(cid1, hw);
		done1.get(5000);
		
		ipc1.sendMessage(cid2, hw);
		done2.get(5000);
		
		System.out.println("Unencrypted IPC works.");
	}
	
	/**
	 *  Test basic IPC.
	 */
	@Test
	public void testSecurity() throws Exception
	{

		byte[] bmsg = "This is a test".getBytes(SUtil.UTF8);
		
		ComponentIdentifier cid1 = getIpcComponentTestId("test1");
		IpcFeature ipc1 = getIpcStreamHandler(cid1);
		
		ComponentIdentifier cid2 = getIpcComponentTestId("test2");
		IpcFeature ipc2 = getIpcStreamHandler(cid2);
		
		SecurityFeature sec1 = new SecurityFeature(cid1.getGlobalProcessIdentifier(), ipc1);
		SecurityFeature sec2 = new SecurityFeature(cid2.getGlobalProcessIdentifier(), ipc2);
		
		String grp1 = "testgrp1";
		String grp1pw = SUtil.createUniqueId();
		String grp2 = "testgrp2";
		byte[] grp2key = new byte[32];
		SSecurity.getSecureRandom().nextBytes(grp2key);
		
		sec1.addGroup(grp1, new PasswordSecret(grp1pw, false));
		sec1.addGroup(grp2, new KeySecret(grp2key));
		sec2.addGroup(grp1, new PasswordSecret(grp1pw, false));
		sec2.addGroup(grp2, new KeySecret(grp2key));
		
		byte[] emsg = sec1.encryptAndSign(cid2, bmsg);
		
		DecodedMessage decmsg = sec2.decryptAndAuth(cid1.getGlobalProcessIdentifier(), emsg);
		
		if (!decmsg.secinfo().getGroups().contains(grp1) || !decmsg.secinfo().getGroups().contains(grp2))
			throw new RuntimeException("Group authentication failed.");
		
		System.out.println("Done security, authenticated groups: " + Arrays.toString(decmsg.secinfo().getGroups().toArray()));
		System.out.println("Encrypted IPC works.");
	}
	
	private IpcFeature getIpcStreamHandler(ComponentIdentifier cid)
	{
		IpcFeature ipc = new IpcFeature(cid.getGlobalProcessIdentifier());
		ipc.setSocketDirectory(ipcdir);
		ipc.open();
		return ipc;
	}
	
	/**
	 *  Generates a fake component ID with a fake host/pid for testing communication.
	 *  
	 *  @param compname Name of the fake component.
	 *  @return Component Identifier.
	 */
	private ComponentIdentifier getIpcComponentTestId(String compname)
	{
		String pid = 1000000000 + (Math.abs(SSecurity.getSecureRandom().nextInt()) % 10000000) +"";
		ComponentIdentifier ret = new ComponentIdentifier(compname, new GlobalProcessIdentifier(pid, GlobalProcessIdentifier.getSelf().host()));
		return ret;
	}
}

