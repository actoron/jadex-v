package jadex.logger;

import java.lang.System.Logger;

/**
 *  Extended interface to allow setLevel().
 */
public interface ISystemLogger	extends Logger
{
	/**
	 *  Set the logging level.
	 */
	public void	setLevel(Level level);
}
