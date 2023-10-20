package jadex.collection;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import jadex.common.IAutoLock;
import jadex.common.RwAutoLock;

/**
 *  Interface for thread-safe data structures using a read-write lock.
 *
 */
public interface IRwDataStructure
{
	/**
	 *  Gets the internal auto lock.
	 *  @return The lock.
	 */
	public RwAutoLock getAutoLock();
	
	/**
	 *  Locks the read lock for resource-based locking.
	 *  
	 *  @return An IAutoLock object for resource-based locking.
	 */
	public default IAutoLock readLock()
	{
		return getAutoLock().readLock();
	}
	
	/**
	 *  Locks the write lock for resource-based locking.
	 *  
	 *  @return An IAutoLock object for resource-based locking.
	 */
	public default IAutoLock writeLock()
	{
		return getAutoLock().writeLock();
	}
	
	/**
	 *  Gets the read lock for manual locking.
	 *  
	 *  @return The read lock.
	 */
	public default Lock getReadLock()
	{
		return getAutoLock().getReadLock();
	}
	
	/**
	 *  Gets the write lock for manual locking.
	 *  
	 *  @return The write lock.
	 */
	public default Lock getWriteLock()
	{
		return getAutoLock().getWriteLock();
	}
	
	/**
	 *  Gets the internal lock.
	 *  @return The lock.
	 */
	public default ReadWriteLock getLock()
	{
		return getAutoLock().getLock();
	}
}
