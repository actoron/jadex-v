package jadex.enginecore.service.types.cron;

import jadex.enginecore.service.annotation.CheckNotNull;
import jadex.enginecore.service.annotation.Service;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;

/**
 *  Interface for adding and removing cron jobs.
 */
@Service(system=true)
public interface ICronService
{
	/**
	 *  Add a schedule job.
	 *  @param job The cron job.
	 */
//	@Timeout(Timeout.NONE)
	public <T> ISubscriptionIntermediateFuture<T> addJob(@CheckNotNull CronJob<T> job);
	
	/**
	 *  Remove a schedule job.
	 *  @param jobid The jobid.
	 */
	public IFuture<Void> removeJob(String jobid);
	
	/**
	 *  Test if a job is scheduled with an id.
	 *  @param jobid The jobid.
	 */
	public IFuture<Boolean> containsJob(String jobid);

}
