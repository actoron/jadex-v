package jadex.core.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.ConsoleHandler;

import jadex.collection.RwMapWrapper;
import jadex.common.IAutoLock;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
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
			boolean init = false;
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
	
	/** The component listeners. */
	private final Map<String, Set<IComponentListener>> listeners = new HashMap<String, Set<IComponentListener>>();

	/** The components. */
	private final Map<ComponentIdentifier, IComponent> components = new LinkedHashMap<ComponentIdentifier, IComponent>();
	
	/** The daemon components. */
	private final Map<ComponentIdentifier, IComponent> daemons = new LinkedHashMap<ComponentIdentifier, IComponent>();
	
	/** The components per app id. */
	private final Map<String, Set<ComponentIdentifier>> appcomps = new HashMap<>();
	
	/** Global counter for components in creation. */
	private volatile int	creationcnt = 0;

	/** App counters for components in creation. */
	private final Map<String, Integer> appcreationcnt = new HashMap<>();

	/** Cache for runtime features in configuration mode (before the start of the first component). */
	protected volatile Map<Class<? extends IRuntimeFeature>, Future<? extends IRuntimeFeature>> featureconfigurationcache = new HashMap<>();
	
	/** Cache for runtime features. */
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

		Future<T> ret = null;

		// We need to hold the cache reference for synchonize later
		boolean noconf = true;
		Map<Class<? extends IRuntimeFeature>, Future<? extends IRuntimeFeature>> concache = featureconfigurationcache;
		if (concache != null)
		{
			synchronized (concache)
			{
				if (featureconfigurationcache != null)
				{
					noconf = false;

					Future<T> existing = (Future<T>) featureconfigurationcache.get(featuretype);
					if (existing != null)
						return existing;

					ret = new Future<>();
					featureconfigurationcache.put(featuretype, ret);
				}
			}
		}

		if (noconf)
		{
			try (IAutoLock l = featurecache.readLock())
			{
				Future<T> existing = (Future<T>) featurecache.get(featuretype);
				if (existing != null)
					return existing;
			}

			try (IAutoLock l = featurecache.writeLock())
			{
				@SuppressWarnings("unchecked")
				Future<T> existing = (Future<T>) featurecache.get(featuretype);
				if (existing != null)
					return existing;

				ret = new Future<>();
				featurecache.put(featuretype, ret);
				//System.out.println("awaitFeature2: "+featuretype+" "+featurecache.keySet());
			}
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

		Future<T> fret = ret;
		boolean fnoconf = noconf;
		createFeature(featuretype).then(feature ->
		{
			if (feature != null)
			{
				//System.out.println("await feature, created feature: "+feature+" "+featurecache.keySet());

				if (fnoconf && feature instanceof ILifecycle)
					((ILifecycle) feature).init();

				fret.setResult(feature);
			}
			else
			{
				fret.setException(new RuntimeException("Feature was declared but could not be created: " + featuretype));
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
	 *  @param type Requested runtime feature type.
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
			}
			catch(Throwable e)
			{
				// If no exe feature in classpath -> fail and never try again.
			}
			HANDLES_INITED	= true;
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
	 *  Create a component based on a pojo.
	 *  @param pojo The pojo.
	 *  @param localname The component local name or null for auto-generation.
	 *  @param app The application context.
	 *  @return The external access of the running component.
	 */
	public IFuture<IComponentHandle> create(Object pojo, String localname, Application app)
	{
		if (!(pojo instanceof IDaemonComponent))
			initializeFeatures();

		ComponentIdentifier cid = localname==null? null: new ComponentIdentifier(localname);
		if(pojo==null)
		{
			// Plain component for null pojo
			//return Component.createComponent(Component.class, () -> new Component(pojo, cid, app));
			return Component.createComponent(new Component(pojo, cid, app));
		}
		else
		{
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(pojo.getClass());
			if(creator!=null)
			{
				return creator.create(pojo, cid, app);
			}
			else
			{
				return new Future<>(new RuntimeException("Could not create component: "+pojo));
			}
		}
	}
	
	/**
	 *  Usage of components as functions that terminate after execution.
	 *  Create a component based on a function.
	 *  @param pojo The pojo.
	 *  @param localname The component id or null for auto-generationm.
	 *  @param app The application context.
	 *  @return The execution result.
	 */
	public <T> IFuture<T> run(Object pojo, String localname, Application app)
	{
		if (!(pojo instanceof IDaemonComponent))
			initializeFeatures();

		if(pojo==null)
		{
			return new Future<>(new UnsupportedOperationException("No null pojo allowed for run()."));
		}
		else
		{
			IComponentLifecycleManager	creator	= SComponentFeatureProvider.getCreator(pojo.getClass());
			if(creator!=null)
			{
				return creator.run(pojo, localname==null ? null : new ComponentIdentifier(localname), app);
			}
			else
			{
				return new Future<>(new RuntimeException("Could not create component: "+pojo));
			}
		}
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
		
		if(comp.getPojo() instanceof IDaemonComponent)
		{
			// Daemon component
			synchronized(daemons)
			{
				IComponent	old	= daemons.put(comp.getId(), comp);
				if(old!=null)
				{
					daemons.put(comp.getId(), old); // restore
					throw new IllegalArgumentException("Daemon component with same CID already exists: "+comp.getId()+" "+ComponentManager.get().getNumberOfComponents());
				}
			}
			//System.out.println("added daemon: "+comp.getId());
		}
		else
		{
			synchronized(components)
			{
				IComponent	old	= components.put(comp.getId(), comp);
				if(old!=null)
				{
					components.put(comp.getId(), old); // restore
	//				ComponentManager.get().printComponents();
					throw new IllegalArgumentException("Component with same CID already exists: "+comp.getId()+" "+ComponentManager.get().getNumberOfComponents());
				}
				
				// Hack. remember first component for fetching informative name.
				if(first==null)
				{
					first	= comp;
				}
				
				// Add component to application, if any.
				String appid = comp.getAppId();
				if(appid!=null)
				{
					Set<ComponentIdentifier> appcompset = appcomps.get(appid);
					if(appcompset==null)
					{
						appcompset = new HashSet<ComponentIdentifier>();
						appcomps.put(appid, appcompset);
					}
					appcompset.add(comp.getId());
				}
			}
		}
		
		// TODO: Added event for daemon components?
		notifyEventListener(COMPONENT_ADDED, comp.getId());
	}
	
	/**
	 *  Remove a component.
	 *  @param comp The component.
	 */
	public void removeComponent(IComponent comp)
	{
		ComponentIdentifier cid = comp.getId();
		if(getLogger().isLoggable(Level.INFO))
			getLogger().log(Level.INFO, "Component removed: "+cid);
		//System.out.println("Component removed: "+cid);
		
		//System.out.println("removing: "+cid);
		if(comp.getPojo() instanceof IDaemonComponent)
		{
			// Daemon component
			synchronized(daemons)
			{
				IComponent old = daemons.remove(comp.getId());
				if(old==null)
				{
					throw new RuntimeException("Unknown daemon component id: "+cid);
				}
			}
		}
		else
		{
			boolean last;
			boolean lastapp = false;
			String appid = null;
			synchronized(components)
			{
				IComponent old = components.remove(cid);
				if(old==null)
				{
					throw new RuntimeException("Unknown component id: "+cid);
				}
				last = creationcnt==0 && components.isEmpty();
				
				appid = comp.getAppId();
				if(appid!=null)
				{
					Set<ComponentIdentifier> appcompset = appcomps.get(appid);
					if(appcompset==null)
						throw new RuntimeException("Unknown app id: "+appid);
					appcompset.remove(cid);
					if(appcompset.isEmpty())
					{
						appcomps.remove(appid);
						lastapp = appcreationcnt.getOrDefault(appid, 0) <= 1;
					}
				}
			}
			if(lastapp)
				notifyEventListener(COMPONENT_LASTREMOVEDAPP, cid);
			if(last)
				notifyEventListener(COMPONENT_LASTREMOVED, cid);
		}
		//System.out.println("size: "+components.size()+" "+cid);

		notifyEventListener(COMPONENT_REMOVED, comp.getId());
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
	 *  @return The component with the given id or null if not found.
	 */
	public IComponent getComponent(ComponentIdentifier cid)
	{
		IComponent comp = null;
		synchronized(components)
		{
			comp	= components.get(cid);
		}
		if(comp==null)
		{
			synchronized(daemons)
			{
				comp	= daemons.get(cid);
			}
		}
		return comp;
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
			return appcomps.getOrDefault(appid, Collections.emptySet()).size();
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
	
	@Override
	public Set<ComponentIdentifier> getAllComponents()
	{
		synchronized(components)
		{
			return new LinkedHashSet<ComponentIdentifier>(components.keySet());
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
			if(Component.isExecutable())
			{
				Set<IComponentListener> fmylisteners	= mylisteners;
				Runnable	notify	= () ->
				{
					if(COMPONENT_ADDED.equals(type))
						fmylisteners.stream().forEach(lis -> lis.componentAdded(cid));
					else if(COMPONENT_REMOVED.equals(type))
						fmylisteners.stream().forEach(lis -> lis.componentRemoved(cid));
					else if(COMPONENT_LASTREMOVED.equals(type)
						|| COMPONENT_LASTREMOVEDAPP.equals(type))
						fmylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid));
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
					else if(COMPONENT_LASTREMOVED.equals(type)
						|| COMPONENT_LASTREMOVEDAPP.equals(type))
						mylisteners.stream().forEach(lis -> lis.lastComponentRemoved(cid));
				}
				catch(Exception e)
				{
					getLogger(Object.class).log(Level.INFO, "Exception in event notification: "+SUtil.getExceptionStacktrace(e));
				}
			}
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
					Component comp = new Component(new IDaemonComponent(){}, new ComponentIdentifier("__globalrunner__"), null)
					{
						public void handleException(Exception exception)
						{
							globalrunner.getLogger().log(Level.INFO, "Exception on global runner: "+SUtil.getExceptionStacktrace(exception));
						}
					};
					comp.init();
					globalrunner = comp;
				}
			}
		}
		return globalrunner;
	}
	
	@Override
	public void waitForLastComponentTerminated()
	{
		doWaitForLastComponentTerminated(null);
	}
	
	/**
	 *  Wait for last global or application component to be terminated.
	 * 	@param app The application to wait for, or null for global.
	 */
	public void	doWaitForLastComponentTerminated(Application app)
	{
		// Use reentrant lock/condition instead of synchronized/wait/notify to avoid pinning when using virtual threads.
		ReentrantLock lock	= new ReentrantLock();
		Condition	wait	= lock.newCondition();

	    try 
	    { 
	    	lock.lock();
	    	boolean dowait;
		    //synchronized(ComponentManager.get().components) 
		    synchronized(components)
		    {
		    	dowait = app==null
		    		? creationcnt!=0 || getNumberOfComponents()!=0
		    		: appcreationcnt.getOrDefault(app.getId(), 0)!=0 || getNumberOfComponents(app.getId())!=0;
		        if(dowait) 
		        {
		        	String eventtype = app==null ? IComponentManager.COMPONENT_LASTREMOVED : IComponentManager.COMPONENT_LASTREMOVEDAPP;
			        IComponentManager.get().addComponentListener(new IComponentListener() 
			        {
			            @Override
			            public void lastComponentRemoved(ComponentIdentifier cid) 
			            {
			        	    try 
			        	    { 
			        	    	lock.lock();
			        	    	IComponentManager.get().removeComponentListener(this, eventtype);
			                    wait.signal();
			                }
			        	    finally
			        	    {
			        			lock.unlock();
			        		}
			            }
			        }, eventtype);
		        }
		    }
		    
		    if(dowait)
		    {
		    	try 
			    {
			    	wait.await();
			    } 
			    catch(InterruptedException e) 
			    {
			        e.printStackTrace();
			    }
		    }
	    }
	    finally
	    {
			lock.unlock();
		}
	}

	public void increaseCreating(Application app)
	{
		synchronized(components) 
		{
			creationcnt++;
			if(app!=null)
			{
				appcreationcnt.put(app.getId(), appcreationcnt.getOrDefault(app.getId(), 0) + 1);
			}
		}
	}
	
	public void decreaseCreating(ComponentIdentifier cid, Application app)
	{
		boolean last = false;
		boolean lastapp = false;
		
		synchronized(components) 
		{
			creationcnt--;			
			last = creationcnt==0 && getNumberOfComponents()==0;
			
			if(app!=null)
			{
				int cnt = appcreationcnt.getOrDefault(app.getId(), 0);
				if(cnt <= 1) 
				{
					appcreationcnt.remove(app.getId());
					lastapp = getNumberOfComponents(app.getId())==0;
				} 
				else 
				{
					appcreationcnt.put(app.getId(), cnt - 1);
				}
			}
		}
		
		if(last)
		{
			notifyEventListener(IComponentManager.COMPONENT_LASTREMOVED, cid);
		}
		if(lastapp)
		{
			notifyEventListener(IComponentManager.COMPONENT_LASTREMOVEDAPP, cid);
		}
	}

	/**
	 *  Get all components of the given application.
	 *  @return The set of all component ids belonging to the application.
	 */
	public Set<ComponentIdentifier> getAllComponents(Application application)
	{
		synchronized(components)
		{
			Set<ComponentIdentifier> ret = appcomps.get(application.getId());
			if(ret==null)
			{
				return Collections.emptySet();
			}
			else
			{
				return new LinkedHashSet<ComponentIdentifier>(ret);
			}
		}
	}

	/**
	 *  Initializes configured runtime features before first component starts.
	 */
	public void initializeFeatures()
	{
		// We need to hold the cache reference for synchonize later
		Map<Class<? extends IRuntimeFeature>, Future<? extends IRuntimeFeature>> concache = featureconfigurationcache;
		if (concache != null)
		{
			synchronized (concache)
			{
				if (featureconfigurationcache != null)
				{
					ArrayList<ILifecycle> initlist = new ArrayList<>();
					for (Map.Entry<Class<? extends IRuntimeFeature>, Future<? extends IRuntimeFeature>> entry : concache.entrySet())
					{
						if (entry.getValue().get() instanceof ILifecycle)
							initlist.add((ILifecycle) entry.getValue().get());

						featurecache.put(entry.getKey(), entry.getValue());
					}
					featureconfigurationcache = null;
					for (ILifecycle lfeat : initlist)
						lfeat.init();
				}
			}
		}
	}
}
