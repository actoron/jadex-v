package jadex.bdi;

/**
 *  An exception that indicates an aborted goal i.e. neither failed nor succeeded.
 *  Note: This exception will not be logged by the logger.
 */
@SuppressWarnings("serial")
public class GoalDroppedException	extends GoalFailureException
{
}
