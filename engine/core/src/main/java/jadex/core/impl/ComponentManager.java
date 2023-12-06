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
		if(instance == null)
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
	
	/** The component id number mode. */
	private boolean cidnumbermode;
	
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
	
	public boolean isComponentIdNumberMode() 
	{
		return cidnumbermode;
	}

	public void setComponentIdNumberMode(boolean cidnumbermode) 
	{
		this.cidnumbermode = cidnumbermode;
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
			{
				ComponentManager.get().printComponents();
				throw new IllegalArgumentException("Component with same CID already exists: "+comp.getId()+" "+ComponentManager.get().getNumberOfComponents());
			}
			components.put(comp.getId(), comp);
		}
		notifyEventListener(IComponent.COMPONENT_ADDED, comp.getId());
	}
	
	public void removeComponent(ComponentIdentifier cid)
	{
		boolean last;
		synchronized(components)
		{
			if(components.remove(cid)==null)
				throw new RuntimeException("Unknown component id: "+cid);
			last = components.isEmpty();
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
	
	/**
	 *  Get the number of current components.
	 */
	public int getNumberOfComponents()
	{
		return components.size();
	}
	
	/**
	 *  Print number of current components.
	 */
	public void printNumberOfComponents()
	{
		System.out.println("Running components: "+components.size());
	}
	
	/**
	 *  Print components.
	 */
	public void printComponents()
	{
		System.out.println("Running components: "+components);
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
