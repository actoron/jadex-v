package jadex.logger;

public interface ILoggerProvider 
{
	public default boolean replacesLoggerProvider(ILoggerProvider provider)
	{
		return false;
	}
}
