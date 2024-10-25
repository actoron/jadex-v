package jadex.logger;

import java.lang.System.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public record LoggerCreator(String name, Function<String, Boolean> filter, Function<String, Logger> icreator, Function<String, Logger> ecreator, boolean system) 
{
    private static final ConcurrentHashMap<Function<String, Boolean>, Integer> ids = new ConcurrentHashMap<>();
    private static final AtomicInteger nextid = new AtomicInteger(1);

    public LoggerCreator(String name, Function<String, Boolean> filter, Function<String, Logger> icreator, Function<String, Logger> ecreator, boolean system) 
    {
        this.name = name;
        this.filter = filter;
        this.icreator = icreator;
        this.ecreator = ecreator;
        this.system = system;
    }

    public LoggerCreator(Function<String, Logger> icreator, Function<String, Logger> ecreator, boolean system) 
    {
        this(null, null, icreator, ecreator, system);
    }

    public LoggerCreator(Function<String, Logger> icreator, Function<String, Logger> ecreator) 
    {
        this(null, null, icreator, ecreator, false);
    }

    public String getLoggerName() 
    {
        String ret = name;
        if(ret == null) 
        {
            ret = system ? "system" : "application";
            if(filter != null) 
                ret += "_" + getFilterId(filter);
        }
        return ret;
    }

    private static int getFilterId(Function<String, Boolean> filter) 
    {
        return ids.computeIfAbsent(filter, key -> nextid.getAndIncrement());
    }
}