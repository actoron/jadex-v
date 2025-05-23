package jadex.logger;

import java.util.ResourceBundle;

/**
 *  Logger implementation that uses an internal and an external logger.
 */
public class CombinedLogger implements ISystemLogger
{
	protected final ISystemLogger ilogger;
    protected final ISystemLogger elogger;
    protected boolean system;

    public CombinedLogger(ISystemLogger ilogger, ISystemLogger elogger, boolean system) 
    {
        if(ilogger==null && elogger==null)
        	throw new NullPointerException();

        this.ilogger = ilogger;
        this.elogger = elogger;
        this.system = system;
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
        if(ilogger != null && ilogger.isLoggable(level)) 
            ilogger.log(level, msg);
        if(elogger != null && elogger.isLoggable(level)) 
            elogger.log(level, msg);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) 
    {
        if(ilogger != null && ilogger.isLoggable(level)) 
            ilogger.log(level, bundle, msg, thrown);
        if(elogger != null && elogger.isLoggable(level)) 
            elogger.log(level, bundle, msg, thrown);
    }

    @Override
    public void log(Level level, ResourceBundle bundle, String format, Object... params) 
    {
        if(ilogger != null && ilogger.isLoggable(level))
            ilogger.log(level, bundle, format, params);
        if(elogger != null && elogger.isLoggable(level))
            elogger.log(level, bundle, format, params);
    }

	@Override
	public String toString() 
	{
		return "CombinedLogger [ilogger=" + ilogger + ", elogger=" + elogger + ", system=" + system + "]";
	}
    
   @Override
   public void setLevel(Level level)
   {
	   if(ilogger!=null)
		   ilogger.setLevel(level);
	   if(elogger!=null)
		   elogger.setLevel(level);
   }
}