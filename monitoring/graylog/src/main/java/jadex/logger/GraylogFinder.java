package jadex.logger;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jadex.core.impl.ComponentManager;
import jadex.core.impl.ComponentManager.LoggerConfigurator;
import jadex.core.impl.SystemPackageScanner;

/**
 *  Used to create logger.
 */
public class GraylogFinder extends LoggerFinder
{
	protected Map<LoggerConfigurator, Logger> loggers = new HashMap<>();
	
	protected Set<String> systempackages = null;
	
	public synchronized Logger getLogger(String name, Module module) 
	{
        if(name == null)
            throw new NullPointerException();
        
        Logger logger = null;

        Collection<LoggerConfigurator> confs = ComponentManager.get().getLoggerConfigurators();
        LoggerConfigurator selected = null;

        boolean isSystem = isSystemLogger(name);

        for(LoggerConfigurator conf : confs) 
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
                logger = new GraylogLogger(name);
                selected.configurator().accept(((GraylogLogger)logger).getLoggerImplementation());
                loggers.put(selected, logger);
            }
        } 
        else 
        {
            System.out.println("No logger configurator found for: " + name);
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
		}
		
		return systempackages.contains(name);
	}
}
