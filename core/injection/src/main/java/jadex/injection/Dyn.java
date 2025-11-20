package jadex.injection;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;

/**
 *  Wrapper for a dynamic value.
 *  Generates appropriate events on changes.
 *  Supports dynamic values that are updated on every access or periodically.
 */
public class Dyn<T>	extends AbstractDynVal<T>
{
	/** The dynamic value provider. */
	final Callable<T>	dynamic;

	/** The update rate in millis, if > 0. */
	long	updaterate;
	
	/** Counter to be incremented whenever a new update rate is set. */
	int	modcount;
	
	/**
	 *  Create an observable value with a dynamic function.
	 */
	public Dyn(Callable<T> dynamic)
	{
		this.dynamic	= dynamic;
	}
	
	/**
	 *  Set the update rate.
	 *  Starts periodic updates when > 0, i.e. the first update happens immediately and then every updaterate millis.
	 *  When 0 or less, no periodic updates are done and the value is evaluated on every get() call (default).
	 *  @param updaterate	The update rate in millis, or 0 for no periodic updates.
	 */
	public Dyn<T>	setUpdateRate(long updaterate)
	{
		return doSetUpdateRate(updaterate, false);
	}
	
	/**
	 *  Set the update rate.
	 *  Starts periodic updates when > 0, i.e. the first update happens immediately and then every updaterate millis.
	 *  When 0 or less, no periodic updates are done and the value is evaluated on every get() call (default).
	 *  @param updaterate	The update rate in millis, or 0 for no periodic updates.
	 *  @param initing	When initing, no event is generated on first (immediate) update. (hack!?)
	 */
	Dyn<T>	doSetUpdateRate(long updaterate, boolean initing)
	{
		this.updaterate	= updaterate;
		// Increment the modcount to "cancel" old timers.
		int fmodcount	= ++modcount;
		
		// When already inited -> start periodic updates.
		if(updaterate>0 && comp!=null)
		{
			IExecutionFeature	exe	= comp.getFeature(IExecutionFeature.class);
			new Consumer<Void>()
			{
				boolean _initing	= initing;
				@Override
				public void accept(Void t)
				{
					// Only update if the update rate has not changed in the meantime.
					if(fmodcount==modcount)
					{
						try
						{
							doSet(dynamic.call(), _initing);
							_initing	= false;
							exe.waitForDelay(updaterate).then(this);
						}
						catch(Exception e)
						{
							SUtil.throwUnchecked(e);
						}
					}
				}
			}
			// Must happen after injections but before on start
			.accept(null);
		}
		
		return this;
	}
	
	/**
	 *  Set the observation mode for inner values.
	 *  Default is COLLECTION_AND_BEAN.
	 */
	public Dyn<T> setObservationMode(ObservationMode mode)
	{
		super.setObservationMode(mode);
		return this;
	}
	
	/**
	 *  Called on component init.
	 */
	@Override
	void	init(IComponent comp, String name)
	{
		super.init(comp, name);
		
		// Set update rate to start periodic updates.
		doSetUpdateRate(updaterate, true);
	}
	
	/**
	 *  Get the current value.
	 *  Gets the dynamic value on every call when no update rate is set.
	 *  When an update rate is set, the last updated value is returned.
	 */
	@Override
	public T get()
	{
		T ret = null;
		try
		{
			ret = updaterate<=0 ? dynamic.call() : value;
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
		
		return ret;
	}
}
