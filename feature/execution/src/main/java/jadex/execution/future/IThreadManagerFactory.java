package jadex.execution.future;

/**
 *  Provide access to managed threads (e.g. component threads or swing thread).
 */
public interface IThreadManagerFactory
{
	public interface IThreadManager
	{
		/**
		 *  Schedule a runnable on the thread manager thread.
		 */
		public void	scheduleStep(Runnable runnable);
	}
	
	/**
	 *  Get the thread manager, if any, for the current thread.
	 */
	public IThreadManager	getThreadManger();
}
