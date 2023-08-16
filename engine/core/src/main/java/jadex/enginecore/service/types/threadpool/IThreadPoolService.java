package jadex.enginecore.service.types.threadpool;

import jadex.concurrent.IThreadPool;
import jadex.enginecore.service.annotation.Service;

/**
 *  Interface for threadpool service.
 */
@Service(system=true)
public interface IThreadPoolService extends IThreadPool
{
}
