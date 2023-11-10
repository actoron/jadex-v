package jadex.core.impl;

import java.net.InetAddress;

import jadex.common.SUtil;

/**
 *  Singleton class providing general information for supporting components.
 */
public class ComponentManager
{
	private static volatile ComponentManager instance;
	
	public static final ComponentManager get()
	{
		if (instance == null)
		{
			synchronized(ComponentManager.class)
			{
				if (instance == null)
				{
					instance = new ComponentManager();
				}
			}
		}
		return instance;
	}
	
	/** Cached process ID. */
	private long pid;
	
	/** Cached host name. */
	private String host;
	
	private ComponentManager()
	{
		pid = ProcessHandle.current().pid();
		host = SUtil.createPlainRandomId("unknown", 12);
		try
		{
			// Probably needs something more clever like obtaining the main IP address.
			InetAddress localhost = InetAddress.getLocalHost();
			host = localhost.getHostName();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public long pid()
	{
		return pid;
	}
	
	public String host()
	{
		return host;
	}
}
