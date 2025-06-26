package jadex.logger;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jadex.core.IComponent;

public class LoggingFeature implements ILoggingFeature
{
	/** The logger configurators. */
	protected List<LoggerCreator> loggercreators = new ArrayList<>();
	
	protected java.lang.System.Logger.Level defsystemlevel = java.lang.System.Logger.Level.WARNING;
	protected java.lang.System.Logger.Level defapplevel = java.lang.System.Logger.Level.INFO;
	
	public LoggingFeature() 
	{
	}
	
	/**
	 *  Add a logger configurator.
	 *  @param filter The filter if the configurator matches.
	 *  @param creator The creator.
	 */
	public synchronized void addLoggerCreator(LoggerCreator creator)
	{
		// remove existing fallback configurator
		if(creator.filter()==null)
			loggercreators.removeIf(lc -> lc.filter()==null && lc.system()==creator.system());
		
		loggercreators.add(creator);
	}
	
	/**
	 *  Update a logger creator by exchanging it against it old version.
	 *  @param ocreator The old creator.
	 *  @param ncreator The new creator.
	 */
	public synchronized void updateLoggerCreator(LoggerCreator ocreator, LoggerCreator ncreator)
	{
		if(ncreator==null)
			throw new NullPointerException("new creator must not null");

		/*if(ocreator!=null)
			removeLoggerCreator(ocreator);
		addLoggerCreator(ncreator);*/
		
		// remove existing fallback configurator
		if(ocreator==null && ncreator.filter()==null)
			loggercreators.removeIf(lc -> lc.filter()==null && lc.system()==ncreator.system());

		loggercreators.remove(ocreator);
		loggercreators.add(ncreator);
	}
	
	/**
	 *  Remove a logger creator.
	 *  @param creator The creator.
	 */
	public synchronized void removeLoggerCreator(LoggerCreator creator)
	{
		loggercreators.remove(creator);
	}
	
	/**
	 *  Get all logger configurators.
	 *  @return The logger configurators
	 */
	public synchronized Collection<LoggerCreator> getLoggerCreators()
	{
		return loggercreators;
	}
	
	@Override
	public synchronized void setSystemLoggingLevel(Level level)
	{
		defsystemlevel = level;
		((ISystemLogger)System.getLogger(IComponent.class.getName())).setLevel(level);
	}
	
	@Override
	public synchronized void setAppLoggingLevel(Level level)
	{
		defapplevel = level;
		((ISystemLogger)System.getLogger("application")).setLevel(level);
	}

	@Override
	public Level getSystemLoggingLevel() 
	{
		return defsystemlevel;
	}

	@Override
	public Level getAppLogginglevel() 
	{
		return defapplevel;
	}
	
}
