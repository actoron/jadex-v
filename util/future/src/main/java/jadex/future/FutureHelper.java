package jadex.future;

import java.util.List;

import jadex.common.ICommand;
import jadex.common.Tuple3;

/**
 *  Helper class to access future notification stack
 */
public abstract class FutureHelper
{
	/**
	 *  Process all collected listener notifications for the current thread, i.e. temporarily disable stack compaction.
	 */
	public static void	notifyStackedListeners()
	{
		// Check if scheduled because Future.NOTIFYING.remove() is expensive
		boolean	scheduled	= Future.NOTIFYING.get()!=null;
		if(!scheduled)
		{
			synchronized(Future.NOTIFICATIONS)
			{
				List<Tuple3<Future<?>, IResultListener<?>, ICommand<IResultListener<?>>>>	queue	= Future.NOTIFICATIONS.get(Thread.currentThread());
				scheduled	= queue!=null && !queue.isEmpty();
			}
		}

		if(scheduled)
		{
			// TODO: resetting notification state breaks some BDI agents (e.g. MaintainGoalContext.agent.xml)
	//		boolean	noti	= Future.NOTIFYING.get()!=null;
			Future.NOTIFYING.remove();	// force new loop even when in outer notification loop
			Future.startScheduledNotifications();
	//		if(noti)
	//			Future.NOTIFYING.set(true);
		}
	}
}