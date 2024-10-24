package jadex.core.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.ConsoleHandler;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.core.ApplicationContext;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentListener;
import jadex.core.IComponentManager;
import jadex.core.IRuntimeFeature;

/**
 *  Singleton class providing general information for supporting components.
 *  
 *  - Application context
 *  - Exception handling
 *  - Logger
 *  - Managing classloader
 *  - Component id generation 
 */
public class ComponentManager implements IComponentManager
{
	private static volatile ComponentManager instance;
	
	/**
	 *  Returns the component manager instance.
	 *  @return The component manager instance.
	 */
	public static final ComponentManager get()
	{
		if(instance == null)
		{
			synchronized(ComponentManager.class)
			{
				if(instance == null)
					instance = new ComponentManager();
			}
		}
		return instance;
	}
	
	/** Classloader used by components. */
	private ClassLoader classloader = ComponentManager.class.getClassLoader();
	
	/** Cached process ID. */
	private long pid;
	
	/** Cached host name. */
	private String host;
	
	/** The component id number mode. */
	private boolean cidnumbermode;
	
	/** The application context. */
	private ApplicationContext appcontext;
	
	/** The component listeners. */
	public final Map<String, Set<IComponentListener>> listeners = new HashMap<String, Set<IComponentListener>>();

	/** The components. */
	public final Map<ComponentIdentifier, IComponent> components = new LinkedHashMap<ComponentIdentifier, IComponent>();
	
	/** The exception handlers. */
	//protected Map<Object, Map<Object, IExceptionHandler<? extends Exception>>> exceptionhandlers = new HashMap<>();	
	protected Map<Object, Map<Object, HandlerInfo>> exceptionhandlers = new HashMap<>();	
	
	/** The active state of loglibs. */
	//protected Map<String, Boolean> loglibsactive = new HashMap<String, Boolean>();
	
	/** Cache fore runtime features. */
	protected RwMapWrapper<Class<IRuntimeFeature>, IRuntimeFeature> featurecache = new RwMapWrapper<Class<IRuntimeFeature>, IRuntimeFeature>(new HashMap<>());
	
	
	
	/**
	 *  Create a new component manager.
	 */
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
		
		// Add default exception handler
		addExceptionHandler(Exception.class, false, (ex, comp) ->
		{
			System.out.println("Exception in user code of component; component will be terminated: "+comp.getId());
			ex.printStackTrace();
			comp.terminate();
		});
		
		// remove default handler
		//removeExceptionHandler(null, Exception.class);
		
		// Set the root logger to warning.
		// Otherwise without logbase feature internal logs get printed
		// With logbase feature the parent logger is ignored anyway.
		configureRootLogger(java.util.logging.Level.WARNING);
	}
	
	/**
	 *  Set the level of the root logger and its handlers.
	 *  @param level The level to set.
	 */
	public static void configureRootLogger(java.util.logging.Level level) 
	{
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
        logger.setLevel(level);

        for(var handler : logger.getHandlers()) 
        {
            if(handler instanceof ConsoleHandler) 
            {
                handler.setLevel(level);
            }
        }
    }
	
	/**
	 *  Get the feature instance for the given type.
	 *  
	 *  @param featuretype Requested runtime feature type.
	 *  @return The feature or null if not found or available.
	 */
	public <T extends IRuntimeFeature> T getFeature(Class<T> featuretype)
	{
		IRuntimeFeature feature = featurecache.get(featuretype);
		if (feature == null)
		{
			try (IAutoLock l = featurecache.writeLock())
			{
				feature = featurecache.get(featuretype);
				if (feature == null)
				{
					RuntimeFeatureProvider<IRuntimeFeature> prov = null;
					@SuppressWarnings("rawtypes")
					Iterator<RuntimeFeatureProvider> it = ServiceLoader.load(RuntimeFeatureProvider.class, classloader).iterator();
					for (;it.hasNext();)
					{
						@SuppressWarnings("unchecked")
						RuntimeFeatureProvider<IRuntimeFeature> next = it.next();
						if (next.getFeatureType().equals(featuretype))
						{
							prov = next;
							break;
						}
					}
					if (prov != null)
					{
						Set<Class<? extends IRuntimeFeature>> preds = prov.getDependencies();
						for (Class<?> pred : preds)
						{
							@SuppressWarnings("unchecked")
							Class<IRuntimeFeature> cpred = (Class<IRuntimeFeature>) pred;
							getFeature(cpred);
						}
						feature = prov.createFeatureInstance();
						@SuppressWarnings("unchecked")
						Class<IRuntimeFeature> basefeaturetype = (Class<IRuntimeFeature>) featuretype;
						featurecache.put(basefeaturetype, feature);
					}
				}
			}
		}
		@SuppressWarnings("unchecked")
		T ret = (T) feature;
		return ret;
	}
	
	/**
	 *  Returns the process identifier of the Java process.
	 *  
	 *  @return Process identifier of the Java process.
	 */
	public long pid()
	{
		return pid;
	}
	
	/**
	 *  Returns the name of the host on which the Java process is running.
	 *  
	 *  @return Name of the host on which the Java process is running.
	 */
	public String host()
	{
		return host;
	}
	
	/**
	 *  Sets the class loader used by components.
	 *  
	 *  @param classloader The class loader that components should use.
	 */
	public void setClassLoader(ClassLoader classloader)
	{
		this.classloader = classloader;
	}
	
	/**
	 *  Gets the class loader used by components.
	 *  
	 *  @return The class loader that components should use.
	 */
	public ClassLoader getClassLoader()
	{
		return classloader;
	}
	
	/**
	 *  Are component ids numbers or strings.
	 *  @return True, if number mode.
	 */
	public boolean isComponentIdNumberMode() 
	{
		return cidnumbermode;
	}
	
	/**
	 *  Configure if numbers instead words should be used
	 *  as automatically generated component names.
	 *  
	 *  @param cidnumbermode True, if automatically generated names should be numbers.
	 */
	public void setComponentIdNumberMode(boolean cidnumbermode) 
	{
		this.cidnumbermode = cidnumbermode;
	}
	
	/**
	 *  Turns on debug messages globally.
	 *  
	 *  @param debug If true, debug messages are emitted globally.
	 */
	public void setDebug(boolean debug)
	{
		SUtil.DEBUG = debug;
	}
	
	/**
	 *  Add a component.
	 *  @param comp The component.
	 */
	public void addComponent(IComponent comp)
	{
		//System.out.println("added: "+comp.getId());
		if(getLogger().isLoggable(Level.INFO))
			getLogger().log(Level.INFO, "Component created: "+comp.getId());
		
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
	
	/**
	 *  Remove a component.
	 *  @param cid The component id.
	 */
	public void removeComponent(ComponentIdentifier cid)
	{
		if(getLogger().isLoggable(Level.INFO))
			getLogger().log(Level.INFO, "Component removed: "+cid);
		//System.out.println("Component removed: "+cid);
		
		//System.out.println("removing: "+cid);
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
		//System.out.println("size: "+components.size()+" "+cid);
	}

	static Logger getLogger()
	{
		return System.getLogger(IComponent.class.getName());
//		System.out.println("CM get logger "+logger);
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
		synchronized(components)
		{
			return components.size();
		}
	}
	
	/**
	 *  Print number of current components.
	 */
	public void printNumberOfComponents()
	{
		System.out.println("Running components: "+getNumberOfComponents());
	}
	
	/**
	 *  Print components.
	 */
	public void printComponents()
	{
		synchronized(components)
		{
			System.out.println("Running components: "+components);
		}
	}
	
	/**
	 *  Set an application context for the components.
	 *  @param appcontext The context.
	 */
	public synchronized void setApplicationContext(ApplicationContext appcontext)
	{
		// todo: add group on security
		this.appcontext = appcontext;
	}
	
	/**
	 *  Get the application context.
	 *  @return The context.
	 */
	public synchronized ApplicationContext getApplicationContext()
	{
		return appcontext;
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
	
	/**
	 *  Add an exception handler.
	 *  @param cid The component id.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public synchronized void addExceptionHandler(ComponentIdentifier cid, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers==null)
		{
			handlers = new HashMap<>();
			exceptionhandlers.put(clazz, handlers);
		}
		handlers.put(cid, new HandlerInfo(handler, exactmatch));
	}
	
	/**
	 *  Add an exception handler.
	 *  @param type The component pojo type.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public synchronized void addExceptionHandler(Class<?> type, Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers==null)
		{
			handlers = new HashMap<>();
			exceptionhandlers.put(clazz, handlers);
		}
		handlers.put(type, new HandlerInfo(handler, exactmatch));
	}
	
	/**
	 *  Add an exception handler for all.
	 *  @param clazz The exception class to match.
	 *  @param exactmatch How clazz should be interpreted.
	 *  @param handler The handler.
	 */
	public synchronized void addExceptionHandler(Class<? extends Exception> clazz, boolean exactmatch, BiConsumer<? extends Exception, IComponent> handler)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers==null)
		{
			handlers = new HashMap<>();
			exceptionhandlers.put(clazz, handlers);
		}
		handlers.put(null, new HandlerInfo(handler, exactmatch));
	}
	
	/**
	 *  Remove an exception handler.
	 *  @param key The key.
	 *  @param clazz The exception class.
	 */
	public synchronized void removeExceptionHandler(Object key, Class<? extends Exception> clazz)
	{
		Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
		if(handlers!=null)
		{
			handlers.remove(key);
			if(handlers.isEmpty())
				exceptionhandlers.remove(clazz);
		}
	}
	
	public synchronized <E extends Exception> BiConsumer<? extends Exception, IComponent> getExceptionHandler(E exception, Component component)
	{
		BiConsumer<? extends Exception, IComponent> ret = null;
		HandlerInfo info;
		Class<?> clazz = exception.getClass();
		boolean exact = true;
		
		while(ret==null)
		{
			// search by exception type
			Map<Object, HandlerInfo> handlers = exceptionhandlers.get(clazz);
			if(handlers!=null)
			{
				// try get individual handler by cid
				info = handlers.get(component.getId());
				if(info!=null && (!info.exact() || exact))
					ret = info.handler(); 
				if(ret==null)
				{
					// try getting by pojo type
					info = component.getPojo()!=null? handlers.get(component.getPojo().getClass()): null;
					if(info!=null && (!info.exact() || exact))
						ret = info.handler(); 
					if(ret==null)
					{
						// try getting by engine type
						info = handlers.get(component.getClass());
						if(info!=null && (!info.exact() || exact))
							ret = info.handler(); 
						if(ret==null)
						{
							// try getting generic handler
							info = handlers.get(null);
							if(info!=null && (!info.exact() || exact))
								ret = info.handler(); 
						}
					}
				}
			}
			
			if(ret==null && clazz!=null)
			{
				clazz = clazz.getSuperclass();
				exact = false;
				if(clazz==null)
					break;
				if(Object.class.equals(clazz))
					clazz = null;
			}
			else
			{
				break;
			}
		}
		
		return ret;
	}
	
	protected record HandlerInfo(BiConsumer<? extends Exception, IComponent> handler, boolean exact) 
	{
	};
	
	
	
	
}
