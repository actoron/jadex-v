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
import jadex.future.Future;
import jadex.future.IFuture;

/**
 *  Singleton class providing general information for supporting components.
 *  
 *  - Managing features
 *  - Managing classloader
 *  - Component id generation 
 */
public class ComponentManager implements IComponentManager
{
	//private static Set<Class<? extends IRuntimeFeature>> knownfeatures = new HashSet<>();

	// todo: make configurable!
	/*static
	{
		long start = System.currentTimeMillis();

		String javacp = System.getProperty("java.class.path");
	    String sep = System.getProperty("path.separator");
	    /*List<String> fcp = Arrays.stream(javacp.split(sep))
	    	.filter(path -> !path.contains("/java") && !path.contains("jdk") && !path.contains("jre"))
	    	.collect(Collectors.toList());
	    String fcps = String.join(sep, fcp);* /

	    List<String> fcp = Arrays.stream(javacp.split(sep))
		    .filter(path ->
		    {
		        String lower = path.toLowerCase();
		        return !lower.contains("/java") &&
		               !lower.contains("jdk") &&
		               !lower.contains("jre") &&
		               !lower.contains("rt.jar") &&
		               !lower.contains("tools.jar") &&
		               !lower.contains("sun") &&
		               !lower.contains("com.sun") &&
		               !lower.contains("oracle") &&
		               !lower.contains("ext") &&
		               !lower.contains("modules");
		    })
		    .collect(Collectors.toList());
	    String fcps = String.join(sep, fcp);

	    System.out.println("javacp: "+javacp);
	    System.out.println("fcp: "+fcps);

	    //ClassLoader cl = ComponentManager.class.getClassLoader();
	    //List<URL> urls = SUtil.getClasspathURLs(cl, true);
	    //System.out.println("urls: "+urls);

	    try (ScanResult res = new ClassGraph()
	    	.enableClassInfo()
	    	.overrideClasspath(fcps)
	        //.overrideClasspath(urls.toArray(new URL[0]))
	        .scan())
	    {
	    	for (ClassInfo ci : res.getClassesImplementing(IRuntimeFeature.class.getName()))
            {
                if (!ci.isInterface() && !ci.isAbstract())
                {
                    Class<?> cls = ci.loadClass();
                    if (IRuntimeFeature.class.isAssignableFrom(cls))
                    {
                        knownfeatures.add((Class<? extends IRuntimeFeature>) cls);
                    }
                }
            }
	    }

		long end = System.currentTimeMillis();

		System.out.println("found feature types: "+knownfeatures);
		System.out.println("needed: "+(end-start));
	}*/
	
	private static volatile ComponentManager instance;
	
	/**
	 *  Returns the component manager instance.
	 *  @return The component manager instance.
	 */
	public static final ComponentManager get()
	{
		return get(null);
	}

	/**
	 *  Returns the component manager instance.
	 *
	 *  @param pid Sets a custom local process identifier, can only be done once.
	 *  @return The component manager instance.
	 */
	public static final ComponentManager get(String pid)
	{
		if(instance == null)
		{
			synchronized(ComponentManager.class)
			{
				if(instance == null)
				{
					if (pid == null)
						pid = "" + ProcessHandle.current().pid();
					instance = new ComponentManager(pid);
					instance.init();
				}
			}
		}
		return instance;
	}
	
	/** Classloader used by components. */
	private ClassLoader classloader = ComponentManager.class.getClassLoader();
	
	/** Cached process ID. */
	private String pid;
	
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
	protected RwMapWrapper<Class<? extends IRuntimeFeature>, Future<? extends IRuntimeFeature>> featurecache = new RwMapWrapper<>(new HashMap<>());
	
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
	 *
	 *  @param pid The local process ID.
	 */
	private ComponentManager(String pid)
	{
		this.pid = pid;
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
		
		//knownfeatures.stream().forEach(type -> featurecache.put((Class)type, new Future()));
		
		// remove default handler
		//removeExceptionHandler(null, Exception.class);
		
		// Set the root logger to warning.
		// Otherwise without logbase feature internal logs get printed
		// With logbase feature the parent logger is ignored anyway.
		configureRootLogger(java.util.logging.Level.WARNING);
	}
	
	private void init()
	{
		// Add default exception handler
		IErrorHandlingFeature ehf = getFeature(IErrorHandlingFeature.class);
		if(ehf!=null)
		{
			ehf.addExceptionHandler(Exception.class, false, (ex, comp) ->
			{
				System.out.println("Exception in user code of component; component will be terminated: "+comp.getId());
				ex.printStackTrace();
				comp.terminate();
			});
		}

		//System.out.println("before initFeatures");
		IFuture<Void> ifut = initFeatures();
		//ifut.then(Void -> System.out.println("init features completed"));
		ifut.printOnEx();
		//System.out.println("after initFeatures");
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
		return awaitFeature(featuretype).get();
	}

	/**
	 *  Get the feature instance for the given type.
	 *
	 *  @param featuretype Requested runtime feature type.
	 *  @return The feature or null if not found or available.
	 */
	public <T extends IRuntimeFeature> IFuture<T> awaitFeature(Class<T> featuretype)
	{
		//System.out.println("awaitFeature: "+featuretype+" "+featurecache.keySet()+" "+this);

		Future<T> ret;

		try (IAutoLock l = featurecache.writeLock())
		{
			@SuppressWarnings("unchecked")
			Future<T> existing = (Future<T>)featurecache.get(featuretype);
			if(existing != null)
				return existing;

			ret = new Future<>();
			featurecache.put(featuretype, ret);
			//System.out.println("awaitFeature2: "+featuretype+" "+featurecache.keySet());
		}

		/*Timer t = new Timer();
		t.schedule(new TimerTask() {

			@Override
			public void run()
			{
				if(!ret.isDone())
				{
					System.out.println("Feature could not be resolved: "+featuretype);
					ret.setExceptionIfUndone(new TimeoutException("Feature could not be resolved: "+featuretype));
				}
			}
		}, 5000);*/

		createFeature(featuretype).then(feature ->
		{
			if (feature != null)
			{
				//System.out.println("await feature, created feature: "+feature+" "+featurecache.keySet());
				ret.setResult(feature);
			}
			else
			{
				ret.setException(new RuntimeException("Feature was declared but could not be created: " + featuretype));
			}
		}).catchEx(ret);

		return ret;
	}

	/**
	 *  Create a runtime feature based on providers.
	 *  @param featuretype The type.
	 *  @return The feature or null if no provider was found.
	 */
	protected <T extends IRuntimeFeature> IFuture<T> createFeature(Class<T> featuretype)
	{
		//System.out.println("createFeature: "+featuretype);
		Future<T> ret = new Future<>();
		RuntimeFeatureProvider<IRuntimeFeature> prov = null;

		@SuppressWarnings("rawtypes")
		Iterator<RuntimeFeatureProvider> it = ServiceLoader.load(RuntimeFeatureProvider.class, classloader).iterator();
		for (; it.hasNext(); )
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
			Future<Void> alldeps = new Future<>();

			Set<Class<? extends IRuntimeFeature>> preds = prov.getDependencies();

			Iterator<Class<? extends IRuntimeFeature>> itpred = preds.iterator();
			Runnable[] chain = new Runnable[1];

			chain[0] = () ->
			{
				if(itpred.hasNext())
				{
					Class<? extends IRuntimeFeature> dep = itpred.next();
					awaitFeature(dep).then(f -> chain[0].run()).catchEx(alldeps);
				}
				else
				{
					alldeps.setResult(null);
				}
			};

			chain[0].run();

			final RuntimeFeatureProvider<IRuntimeFeature> fprov = prov;
			alldeps.then(v ->
			{
				@SuppressWarnings("unchecked")
				T instance = (T)fprov.createFeatureInstance();
				ret.setResult(instance);
			}).catchEx(ret);
		}
		else
		{
			ret.setResult(null);
		}
		return ret;
	}

	/**
	 *  Init the non-lazy features with providers.
	 * /
	public IFuture<Void> initFeatures()
	{
		Future<Void> ret = new Future<>();

		@SuppressWarnings("rawtypes")
		Iterator<RuntimeFeatureProvider> it = ServiceLoader.load(RuntimeFeatureProvider.class, classloader).iterator();
		for (;it.hasNext();)
		{
			RuntimeFeatureProvider<IRuntimeFeature> prov = it.next();

			if(!prov.isLazyFeature())
			{
				Class<IRuntimeFeature> featuretype = prov.getFeatureType();
				createFeature(featuretype).then(feature ->
				{
					if(feature!=null)
					{
						try (IAutoLock l = featurecache.writeLock())
						{
							featurecache.put(featuretype, feature);
						}
					}
					else // should not occur as there is a provider
					{
						ret.setException(new RuntimeException("Could not create feature: "+featuretype));
						//featurecache.put(featuretype, ret);
					}
				}).catchEx(ret);

				/*Set<Class<? extends IRuntimeFeature>> preds = prov.getDependencies();
				for (Class<?> pred : preds)
				{
					@SuppressWarnings("unchecked")
					Class<IRuntimeFeature> cpred = (Class<IRuntimeFeature>) pred;
					getFeature(cpred);
				}
				IRuntimeFeature feature = prov.createFeatureInstance();
				try (IAutoLock l = featurecache.writeLock())
				{
					featurecache.put(prov.getFeatureType(), feature);
				}* /
			}
		}

		return ret;
	}*/

	public IFuture<Void> initFeatures()
	{
	    Future<Void> chain = new Future<>();
		@SuppressWarnings("rawtypes")
		Iterator it = ServiceLoader.load(RuntimeFeatureProvider.class, classloader).iterator();
		@SuppressWarnings("unchecked")
		Iterator<RuntimeFeatureProvider<IRuntimeFeature>>it1 = it;
	    initNextFeature(it1, chain);
	    return chain;
	}

	private void initNextFeature(Iterator<RuntimeFeatureProvider<IRuntimeFeature>> it, Future<Void> chain)
	{
	    if (!it.hasNext())
	    {
	        chain.setResult(null);
	        return;
	    }

	    RuntimeFeatureProvider<IRuntimeFeature> prov = it.next();
	    //System.out.println("init next: "+prov.getFeatureType());
	    awaitFeature(prov.getFeatureType()).then(res ->
	    {
	    	initNextFeature(it, chain);
	    }).catchEx(chain);
	}

	/**
	 *  Add a runtime feature.
	 *  @param feature The feature
	 */
	public void addFeature(IRuntimeFeature feature)
	{
	     Class<? extends IRuntimeFeature> type = resolveFeatureInterface(feature.getClass());
	     addFeature(type, feature);
	}

	public void addFeature(Class<? extends IRuntimeFeature> type, IRuntimeFeature feature)
	{
		Future<IRuntimeFeature> feafut = null;

		try (IAutoLock l = featurecache.writeLock())
		{
			Object cached = featurecache.get(type);

			if (cached instanceof Future)
			{
				@SuppressWarnings("unchecked")
				Future<IRuntimeFeature> fut = (Future<IRuntimeFeature>) cached;

				if (fut.isDone())
					throw new RuntimeException("Feature already resolved: " + type);

				feafut = fut;
			}
			else if (cached != null)
			{
				throw new RuntimeException("Feature already resolved with non-future: " + type);
			}
			else
			{
				Future<IRuntimeFeature> fut = new Future<>();
				featurecache.put(type, fut);
				feafut = fut;
			}
		}

		// lock-free notification
		if (feafut != null)
			feafut.setResult(feature);
	}

	/**
	 *  Test if a feature is present.
	 *
	 *  @param featuretype Requested runtime feature type.
	 *  @return True, if the feature is present, i.e. created.
	 * /
	public boolean isFeatureKnown(Class<?> featuretype)
	{
		//return featurecache.get(featuretype)!=null;
		return knownfeatures.contains(featuretype);
	}*/

	public boolean isFeatureResolved(Class<?> type)
	{
		Object val = featurecache.get(type);
		return val != null && (!(val instanceof Future) || ((Future<?>)val).isDone());
	}


    private Class<? extends IRuntimeFeature> resolveFeatureInterface(Class<?> implClass)
    {
        Set<Class<? extends IRuntimeFeature>> interfaces = new HashSet<>();

        for (Class<?> iface : implClass.getInterfaces())
        {
            collectRuntimeFeatureInterfaces(iface, interfaces);
        }

        Class<?> superclass = implClass.getSuperclass();
        while (superclass != null)
        {
            for (Class<?> iface : superclass.getInterfaces())
                collectRuntimeFeatureInterfaces(iface, interfaces);
            superclass = superclass.getSuperclass();
        }

        Set<Class<? extends IRuntimeFeature>> mostspec = getMostSpecificInterfaces(interfaces);

        if (mostspec.size() == 1)
        {
            return mostspec.iterator().next();
        }
        else if (mostspec.isEmpty())
        {
            throw new IllegalArgumentException("No interface extending IRuntimeFeature found for class " + implClass.getName());
        }
        else
        {
            throw new IllegalArgumentException("Multiple unrelated interfaces extending IRuntimeFeature found: " + mostspec);
        }
    }

    @SuppressWarnings("unchecked")
    private void collectRuntimeFeatureInterfaces(Class<?> iface, Set<Class<? extends IRuntimeFeature>> result)
    {
        if (!iface.isInterface()) return;

        if (IRuntimeFeature.class.isAssignableFrom(iface))
            result.add((Class<? extends IRuntimeFeature>) iface);

        for (Class<?> parent : iface.getInterfaces())
            collectRuntimeFeatureInterfaces(parent, result);
    }

    private Set<Class<? extends IRuntimeFeature>> getMostSpecificInterfaces(Set<Class<? extends IRuntimeFeature>> interfaces)
    {
        Set<Class<? extends IRuntimeFeature>> result = new HashSet<>(interfaces);

        for (Class<? extends IRuntimeFeature> a : interfaces)
        {
            for (Class<? extends IRuntimeFeature> b : interfaces)
            {
                if (a != b && a.isAssignableFrom(b))
                {
                    result.remove(a); // a is more generic than b -> remove
                }
            }
        }

        return result;
    }

    /**
	 *  Remove a runtime feature.
	 *  @param type The feature type.
	 */
	public void removeFeature(Class<IRuntimeFeature> type)
	{
		featurecache.remove(type);
	}

	/**
	 *  Get the feature instance for the given type.
	 *
	 *  @param featuretype Requested runtime feature type.
	 *  @return The feature or null if not found or available.
	 * /
	public <T extends IRuntimeFeature> T getFeature(Class<T> featuretype)
	{
//		System.out.println("getFeature: "+featuretype);

		Object feature = featurecache.get(featuretype);
		if(feature instanceof IFuture)
		{
			IFuture<T> f = (IFuture<T>)feature;
			if(f.isDone())
				feature = f.get();
			else
				throw new RuntimeException("Feature still unresolved: "+featuretype);
		}
		else if (feature == null)
		{
			try (IAutoLock l = featurecache.writeLock())
			{
				feature = featurecache.get(featuretype);
				if (feature == null)
				{
					feature = createFeature(featuretype);
					if(feature!=null)
					{
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
	}*/
	
	/**
	 *  Returns the process identifier of the Java process.
	 *  
	 *  @return Process identifier of the Java process.
	 */
	public String pid()
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
