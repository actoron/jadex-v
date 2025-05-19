package jadex.core.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.ConsoleHandler;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentListener;
import jadex.core.IComponentManager;
import jadex.core.IRuntimeFeature;
import jadex.errorhandling.IErrorHandlingFeature;

/**
 *  Singleton class providing general information for supporting components.
 *  
 *  - Managing features
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
	
	// todo: avoid making these public!
	
	/** The component listeners. */
	final Map<String, Set<IComponentListener>> listeners = new HashMap<String, Set<IComponentListener>>();

	/** The components. */
	private final Map<ComponentIdentifier, IComponent> components = new LinkedHashMap<ComponentIdentifier, IComponent>();
	
	/** The number of components per appid. */
	private final Map<String, Integer> appcompcnt = new HashMap<>();

	
	/** Cache fore runtime features. */
	protected RwMapWrapper<Class<IRuntimeFeature>, IRuntimeFeature> featurecache = new RwMapWrapper<Class<IRuntimeFeature>, IRuntimeFeature>(new HashMap<>());
	
	public void addComponentListener(IComponentListener listener, String... types)
	{
		synchronized(listeners)
		{	
			//System.out.println("adding comp listener: "+Arrays.toString(types));
			for(String type: types)
			{
				Set<IComponentListener> ls = ComponentManager.get().listeners.get(type);
				if(ls==null)
				{
					ls = new HashSet<IComponentListener>();
					ComponentManager.get().listeners.put(type, ls);
				}
				ls.add(listener);
			}
		}
	}
	
	public void removeComponentListener(IComponentListener listener, String... types)
	{
		synchronized(listeners)
		{
			for(String type: types)
			{
				Set<IComponentListener> ls = ComponentManager.get().listeners.get(type);
				if(ls!=null)
				{
					ls.remove(listener);
					if(ls.isEmpty())
						ComponentManager.get().listeners.remove(type);
				}
			}
		}
	}
	
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
		getFeature(IErrorHandlingFeature.class).addExceptionHandler(Exception.class, false, (ex, comp) ->
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
	 * /
	public void setDebug(boolean debug)
	{
		SUtil.DEBUG = debug;
	}*/
	
	// Hack. remember first component for fetching informative name.
	IComponent	first	= null;
	
	/**
	 *  Get the component/pojo toString()/classname of the first started component.
	 *  @return null if no component has been started yet. 
	 */
	public String	getInferredApplicationName()
	{
		String	ret	= null;
		IComponent	comp	= getCurrentComponent();
		comp	= comp!=null ? comp : first;
		
		// Has application
		if(comp!=null && comp.getApplication()!=null)
		{
			ret	= comp.getApplication().getName();
		}
		
		// Has pojo
		else if(comp!=null && comp.getPojo()!=null)
		{
			try
			{
				// Check for overridden toString() (raises exception if not found)
				comp.getPojo().getClass().getDeclaredMethod("toString");
				ret	= comp.getPojo().toString();
			}
			catch(Exception e)
			{
				// If no toString() use class name
				ret	= comp.getPojo().getClass().getName();
				// Strip lambda  address(!?)
				if(ret!=null && ret.indexOf('/')!=-1)
				{
					ret	= ret.substring(0, ret.indexOf('/'));
				}
			}			
		}
		
		// Has component w/o pojo
		else if (comp!=null)
		{
			// TODO: can we derive a more useful app name?
			ret	= comp.getClass().getName();
		}
		
		return ret;
	}
	
	static boolean	HANDLES_INITED	= false;
	static ThreadLocal<Object> LOCAL;
	static MethodHandle	GET_COMPONENT;
	
	/**
	 * Get the current component.
	 * @return	null, if not running inside a component.
	 */
	public IComponent getCurrentComponent()
	{
		IComponent ret	= null;
		
		// Hack!!! use reflection to find current component via execution feature, if any
		if(!HANDLES_INITED)
		{
			try
			{
				Class<?>	cexe	= Class.forName("jadex.execution.impl.ExecutionFeature");
				Field	flocal	= cexe.getField("LOCAL");
				MethodHandle	getlocal	= MethodHandles.lookup().unreflectGetter(flocal);
				LOCAL	= (ThreadLocal<Object>)getlocal.invoke();
				GET_COMPONENT	= MethodHandles.lookup().unreflect(cexe.getMethod("getComponent"));
				HANDLES_INITED	= true;
			}
			catch(Throwable e)
			{
				// If no exe feature in classpath -> fail and never try again.
			}
		}
		
		if(LOCAL!=null)
		{
			try
			{
				Object	exefeature	= LOCAL.get();
				ret	= exefeature!=null ? (IComponent)GET_COMPONENT.invoke(exefeature) : null;
			}
			catch(Throwable e)
			{
			}
		}

		return ret;
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
			
			// Hack. remember first component for fetching informative name.
			if(first==null)
			{
				first	= comp;
			}
			
			components.put(comp.getId(), comp);
			if(comp.getAppId()!=null)
				incrementComponentCount(comp.getAppId());
		}
		notifyEventListener(COMPONENT_ADDED, comp.getId(), null);
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
		boolean lastapp = false;
		String appid = null;
		synchronized(components)
		{
			IComponent comp = components.remove(cid);
			if(comp==null)
				throw new RuntimeException("Unknown component id: "+cid);
			last = components.isEmpty();
			appid = comp.getAppId();
			if(appid!=null)
			{
				decrementComponentCount(appid);
				lastapp = getNumberOfComponents(appid)==0;
			}
		}
		notifyEventListener(COMPONENT_REMOVED, cid, null);
		if(lastapp)
			notifyEventListener(COMPONENT_LASTREMOVEDAPP, cid, null);
		if(last)
			notifyEventListener(COMPONENT_LASTREMOVED, cid, appid);
		//System.out.println("size: "+components.size()+" "+cid);
	}

	// Caching for small speedup (detected in PlainComponentBenchmark)
	private Logger logger	= null;
	private Logger getLogger()
	{
		if(logger==null)
			logger	= System.getLogger(IComponent.class.getName());
		return logger;
//		System.out.println("CM get logger "+logger);
	}

	/**
	 *  Convenience method that returns access to the logging subsystem used by Jadex.
	 *
	 *  @param requestingClass The class on whose behalf logging access is requested.
	 *  @return A logger.
	 */
	public Logger getLogger(Class<?> requestingClass)
	{
		return System.getLogger(requestingClass.getName());
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
	 *  Get the number of current components per app.
	 *  @param appid The app id.
	 *  @return The number of components in this app.
	 */
	public int getNumberOfComponents(String appid)
	{
		synchronized(components)
		{
			return appcompcnt.getOrDefault(appid, 0);
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
	 * /
	public synchronized void setApplicationContext(ApplicationContext appcontext)
	{
		// todo: add group on security
		this.appcontext = appcontext;
	}*/
	
	/**
	 *  Get the application context.
	 *  @return The context.
	 * /
	public synchronized ApplicationContext getApplicationContext()
	{
		return appcontext;
	}*/
	
	public void notifyEventListener(String type, ComponentIdentifier cid, String appid)
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
			if(Component.isExecutable())
			{
				Set<IComponentListener> fmylisteners	= mylisteners;
				Runnable	notify	= () ->
				{
					if(COMPONENT_ADDED.equals(type))
						fmylisteners.stream().forEach(lis -> lis.componentAdded(cid));
					else if(COMPONENT_REMOVED.equals(type))
						fmylisteners.stream().forEach(lis -> lis.componentRemoved(cid));
					else if(COMPONENT_LASTREMOVED.equals(type))
						fmylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid));
					else if(COMPONENT_LASTREMOVEDAPP.equals(type))
						fmylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid, appid));
				};

				getGlobalRunner().getComponentHandle().scheduleStep(notify);
			}
			else
			{
				try
				{
					if(COMPONENT_ADDED.equals(type))
						mylisteners.stream().forEach(lis -> lis.componentAdded(cid));
					else if(COMPONENT_REMOVED.equals(type))
						mylisteners.stream().forEach(lis -> lis.componentRemoved(cid));
					else if(COMPONENT_LASTREMOVED.equals(type))
						mylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid));
					else if(COMPONENT_LASTREMOVEDAPP.equals(type))
						mylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid, appid));
				}
				catch(Exception e)
				{
					getLogger(Object.class).log(Level.INFO, "Exception in event notification: "+SUtil.getExceptionStacktrace(e));
				}
			}
		}
	}
	
	public void incrementComponentCount(String appid) 
	{
		synchronized (components) 
		{
			appcompcnt.put(appid, appcompcnt.getOrDefault(appid, 0) + 1);
			//System.out.println("inc: "+appid+" "+appcompcnt);
		}
	}

	public void decrementComponentCount(String appid) 
	{
		synchronized (components) 
		{
			int count = appcompcnt.getOrDefault(appid, 0);
			if (count <= 1) 
			{
				appcompcnt.remove(appid);
			} 
			else 
			{
				appcompcnt.put(appid, count - 1);
		    }
			//System.out.println("dec: "+appid+" "+appcompcnt);
		}
	}
	
	public void runWithComponentsLock(Runnable run)
	{
		synchronized (components) 
		{
			run.run();
		}
	}
	
	public void runWithListenersLock(Runnable run)
	{
		synchronized (listeners) 
		{
			run.run();
		}
	}
	
	protected volatile IComponent	globalrunner;
	/**
	 *  Get or create a component to run global steps on, e.g. component listener notifications.
	 */
	public IComponent	getGlobalRunner()
	{
		if(globalrunner==null)
		{
			synchronized(this)
			{
				if(globalrunner==null)
				{
					try
					{
						globalrunner	= SUtil.getExecutor().submit(() -> new Component(null, new ComponentIdentifier(Component.GLOBALRUNNER_ID))
						{
							public void handleException(Exception exception)
							{
								globalrunner.getLogger().log(Level.INFO, "Exception on global runner: "+SUtil.getExceptionStacktrace(exception));
							}
						}).get();
					}
					catch(Exception e)
					{
						SUtil.throwUnchecked(e);
					}
				}
			}
		}
		return globalrunner;
	}
}
