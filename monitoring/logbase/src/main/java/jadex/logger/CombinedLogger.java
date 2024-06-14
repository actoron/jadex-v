package jadex.logger;

import java.util.ResourceBundle;

/**
 *  Logger implementation that uses an internal and an external logger.
 */
public class CombinedLogger implements System.Logger 
{
	protected final System.Logger ilogger;
    protected final System.Logger elogger;

    public CombinedLogger(System.Logger ilogger, System.Logger elogger) 
    {
        if(ilogger==null && elogger==null)
        	throw new NullPointerException();

        this.ilogger = ilogger;
        this.elogger = elogger;
    }

    @Override
    public String getName() 
    {
        if(ilogger != null) 
            return ilogger.getName();
        else if (elogger != null)
            return elogger.getName();
        else
            return "CombinedLogger";
    }

    @Override
    public boolean isLoggable(Level level) 
    {
        boolean il = ilogger != null && ilogger.isLoggable(level);
        boolean el = elogger != null && elogger.isLoggable(level);
        return il || el;
    }

    @Override
    public void log(Level level, String msg) 
    {
        if(ilogger != null) 
            ilogger.log(level, msg);
        if(elogger != null) 
            elogger.log(level, msg);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        if(ilogger != null) 
            ilogger.log(level, bundle, msg, thrown);
        if(elogger != null) 
            elogger.log(level, bundle, msg, thrown);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        if(ilogger != null)
            ilogger.log(level, bundle, format, params);
        if(elogger != null)
            elogger.log(level, bundle, format, params);
    }
}