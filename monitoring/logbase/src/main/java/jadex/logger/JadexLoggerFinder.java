package jadex.logger;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;

import jadex.core.IComponentManager;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.ComponentManager.LoggerCreator;

/**
 *  Used to create logger.
 */
public class JadexLoggerFinder extends LoggerFinder
{
	protected Map<LoggerCreator, Logger> loggers = new HashMap<>();
	protected Map<String, Logger> loggersbyname = new HashMap<>();
	
	protected Set<String> systempackages = null;
	
	public JadexLoggerFinder()
	{
		Collection<IInternalLoggerProvider> iproviders = new ArrayList<IInternalLoggerProvider>(); 
		ServiceLoader.load(IInternalLoggerProvider.class).forEach(prov ->
		{
			iproviders.add(prov);
		});
		IInternalLoggerProvider iprov = (IInternalLoggerProvider)getLoggerProvider(iproviders);
		
		Collection<IExternalLoggerProvider> eproviders = new ArrayList<IExternalLoggerProvider>(); 
		ServiceLoader.load(IExternalLoggerProvider.class).forEach(prov ->
		{
			eproviders.add(prov);
		});
		IExternalLoggerProvider eprov = (IExternalLoggerProvider)getLoggerProvider(eproviders);
		
		if(iprov!=null || eprov!=null)
		{
			LoggerCreator sysc = IComponentManager.get().getLoggerCreators().stream().filter(c -> c.system() && c.filter()==null).findFirst().orElse(null);
			
			LoggerCreator appc = IComponentManager.get().getLoggerCreators().stream().filter(c -> !c.system() && c.filter()==null).findFirst().orElse(null);
			
			java.lang.System.Logger.Level syslevel = iprov!=null && !iprov.isConfigured()? java.lang.System.Logger.Level.WARNING: null; 
			java.lang.System.Logger.Level applevel = iprov!=null && !iprov.isConfigured()? java.lang.System.Logger.Level.ALL: null; 
			
//			System.out.println("syslevel: "+syslevel);
//			System.out.println("applevel: "+applevel);
			
			LoggerCreator nsysc = new LoggerCreator( 
				sysc!=null && sysc.icreator()!=null? sysc.icreator(): iprov!=null? name -> iprov.getLogger(name, syslevel): null, 
				sysc!=null && sysc.ecreator()!=null? sysc.ecreator(): eprov!=null? name -> eprov.getLogger(name): null, 
				true);
			
			LoggerCreator nappc = new LoggerCreator(
				appc!=null && appc.icreator()!=null? appc.icreator(): iprov!=null? name -> iprov.getLogger(name, applevel): null, 
				appc!=null && appc.ecreator()!=null? appc.ecreator(): eprov!=null? name -> eprov.getLogger(name): null, 
				false);
			
			if(sysc==null)
				IComponentManager.get().addLoggerCreator(nsysc);
			else
				IComponentManager.get().updateLoggerCreator(sysc, nsysc);
			
			if(appc==null)
				IComponentManager.get().addLoggerCreator(nappc);
			else
				IComponentManager.get().updateLoggerCreator(appc, nappc);
		}
	}
	
	protected ILoggerProvider getLoggerProvider(Collection<? extends ILoggerProvider> providers)
	{
		ILoggerProvider ret = null;
		for(ILoggerProvider provider: providers)
		{
			if(ret==null)
			{
				ret = provider;
				continue;
			}
			
			if(provider.replacesLoggerProvider(ret))
			{
				// Both want to replace each other -> fail
				if(ret.replacesLoggerProvider(provider))
				{
					throw new IllegalStateException("Cyclic replacement of logger providers: "+provider+", "+ret);
				}
				// new provider wants to replace existing -> replace
				else
				{
					ret = provider;
				}
			}
			// existing provider wants to replace new -> nop
			//else if(ret.get(provider.getFeatureType()).replacesFeatureProvider(provider))
			
			// no provider wants to replace the other -> fail
			else if(!ret.replacesLoggerProvider(provider))
			{
				System.out.println("Multiple logger providers without ordering: "+providers);
				//throw new IllegalStateException("Two providers with no replacement: "+provider+", "+ret);
			}
		}
		return ret;
	}
	
	public synchronized Logger getLogger(String name, Module module) 
	{
        if(name == null)
            throw new NullPointerException();
        
        // shortcut access by name (must be the name that was generated by the loggercreator)
        Logger logger = loggersbyname.get(name);
        
        if(logger==null)
        {
	        Collection<LoggerCreator> confs = ComponentManager.get().getLoggerCreators();
	        LoggerCreator selected = null;
	
	        boolean isSystem = isSystemLogger(name);
	
	        for(LoggerCreator conf : confs) 
	        {
	            if(conf.system() == isSystem) 
	            {
	                // Default logger for system or application
	                if(conf.filter() == null) 
	                {
	                    selected = conf;
	                } 
	                else if(conf.filter().apply(name)) 
	                {
	                    selected = conf;
	                    break;
	                }
	            }
	        }
	
	        if(selected != null) 
	        {
	            logger = loggers.get(selected);
	            if(logger == null) 
	            {
	            	Logger ilogger = selected.icreator()!=null? selected.icreator().apply(selected.getLoggerName()): null;
	            	Logger elogger = selected.ecreator()!=null? selected.ecreator().apply(selected.getLoggerName()): null;
	            	logger = new CombinedLogger(ilogger, elogger);
	                loggers.put(selected, logger);
	                loggersbyname.put(selected.getLoggerName(), logger);
	            }
	        } 
	        else 
	        {
	            System.out.println("No logger creator found for: " + name);
	        }
	        
//	        System.out.println("Created logger: "+name+" "+isSystem);
        }
        
        return logger;
    }
	
	/**
	 *  Check if the name belongs to a system class (i.e. package).
	 *  If the name does no represent a class name, false is returned.
	 *  @param name The logger name.
	 *  @return True, if is system logger name.
	 */
	protected boolean isSystemLogger(String name)
	{
		boolean ret = false;
		
		String packname = name;
		int idx = name.lastIndexOf(".");
		if(idx>0)
			packname = name.substring(0, idx);
		
		ret = isSystemPackage(packname);
		
		return ret;
	}
	
	protected synchronized boolean isSystemPackage(String name)
	{
		if(systempackages==null)
		{
			//long start = System.currentTimeMillis();
			systempackages = SystemPackageScanner.getSystemPackages();
			//systempackages.forEach(System.out::println);
	        //long end = System.currentTimeMillis();
	        //System.out.println("Needed: "+(end-start));
			
//			System.out.println("Scanner found system packages: "+systempackages);
		}
		
		return systempackages.contains(name);
	}
}