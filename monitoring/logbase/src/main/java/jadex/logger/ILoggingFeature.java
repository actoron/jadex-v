package jadex.logger;

import java.util.Collection;

import jadex.core.IRuntimeFeature;

public interface ILoggingFeature extends IRuntimeFeature
{
	/**
	 *  Add a logger creator.
	 *  @param filter The filter if the creator matches.
	 *  @param creator The creator.
	 */
	public void addLoggerCreator(LoggerCreator creator);
	
	/**
	 *  Update a logger creator by exchanging it against the old version.
	 *  @param ocreator The old creator.
	 *  @param ncreator The new creator.
	 */
	public void updateLoggerCreator(LoggerCreator ocreator, LoggerCreator ncreator);
	
	/**
	 *  Get all logger creators.
	 *  @return The logger creators
	 */
	public Collection<LoggerCreator> getLoggerCreators();
	
	/**
	 *  Remove a logger creator.
	 *  @param creator The creator.
	 */
	public void removeLoggerCreator(LoggerCreator creator);
	
	/**
	 *  Set the default logging level for the system logger (Jadex framework logger)
	 *  @param level The level.
	 */
	public void setDefaultSystemLoggingLevel(java.lang.System.Logger.Level level);
	
	/**
	 *  Set the default logging level for the application logger.
	 *  @param level The level.
	 */
	public void setDefaultAppLoggingLevel(java.lang.System.Logger.Level level);

	/**
	 *  Get the default system log level.
	 *  @return The level.
	 */
	public java.lang.System.Logger.Level getDefaultSystemLoggingLevel(); 

	/**
	 *  Get the default app log level.
	 *  @return The level.
	 */
	public java.lang.System.Logger.Level getDefaultAppLogginglevel(); 
}
