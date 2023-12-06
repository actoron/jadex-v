package jadex.core.impl;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentListener;

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
	
	public final Map<String, Set<IComponentListener>> listeners = new HashMap<String, Set<IComponentListener>>();

	protected Map<ComponentIdentifier, IComponent> components = new LinkedHashMap<ComponentIdentifier, IComponent>();
	
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
	
	public void setDebug(boolean debug)
	{
		SUtil.DEBUG = debug;
	}
	
	public void addComponent(IComponent comp)
	{
		//System.out.println("added: "+comp.getId());
		synchronized(components)
		{
			if(components.containsKey(comp.getId()))
				throw new IllegalArgumentException("Component with same CID already exists: "+comp.getId());
			components.put(comp.getId(), comp);
		}
		notifyEventListener(IComponent.COMPONENT_ADDED, comp.getId());
	}
	
	public void removeComponent(ComponentIdentifier cid)
	{
		boolean last;
		synchronized(components)
		{
			components.remove(cid);
			last	= components.isEmpty();
		}
		notifyEventListener(IComponent.COMPONENT_REMOVED, cid);
		if(last)
			notifyEventListener(IComponent.COMPONENT_LASTREMOVED, cid);
//		System.out.println("size: "+components.size()+" "+cid);
	}
	
	/**
	 *  Get a running component.
	 *  @throws IllegalArgumentException when the component does not exist.
	 */
	public IComponent getComponent(ComponentIdentifier cid)
	{
		synchronized(components)
		{
			return components.get(cid);
		}
	}
	
	public void notifyEventListener(String type, ComponentIdentifier cid)
	{
		Set<IComponentListener> mylisteners = null;
		
		synchronized(listeners)
		{
			Set<IComponentListener> ls = listeners.get(type);
			if(ls!=null)
				mylisteners = new HashSet<IComponentListener>(ls);
		}
		
		if(mylisteners!=null)
		{
			if(IComponent.COMPONENT_ADDED.equals(type))
				mylisteners.stream().forEach(lis -> lis.componentAdded(cid));
			else if(IComponent.COMPONENT_REMOVED.equals(type))
				mylisteners.stream().forEach(lis -> lis.componentRemoved(cid));
			else if(IComponent.COMPONENT_LASTREMOVED.equals(type))
				mylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid));
		}
	}
}
