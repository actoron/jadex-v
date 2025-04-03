package jadex.bdi;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import jadex.common.SUtil;

/**
 *  Wrapper for belief values.
 *  Generates appropriate rule events on changes.
 */
public class Val<T>
{
	T	value;
	Callable<T>	dynamic;
	
	// Fields set on init
	BiConsumer<T, T>	changehandler;
	boolean	updaterate;
	
	/**
	 *  Create belief value with a given value.
	 */
	public Val(T value)
	{
		this.value	= value;
	}
	
	/**
	 *  Create belief value with a dynamic function.
	 */
	public Val(Callable<T> dynamic)
	{
		this.dynamic	= dynamic;
	}
	
	/**
	 *  Called on agent init.
	 *  @param changehandler	The change handler gets called after any change with old and new value.
	 *  @param updaterate	Flag to indicate that the value is externally updated.
	 */
	protected void	init(BiConsumer<T, T> changehandler, boolean updaterate)
	{
		this.changehandler	= changehandler;
		this.updaterate	= updaterate;
	}
	
	/**
	 *  Get the current value.
	 */
	public T get()
	{
		T ret = null;
		try
		{
			ret = dynamic!=null && !updaterate? dynamic.call(): value;
		}
		catch(Exception e)
		{
			//e.printStackTrace();
			throw SUtil.throwUnchecked(e);
		}
		
		//if(ret==null)
		//System.out.println("val get: "+ret);
	
		return ret;
	}

	/**
	 *  Set the value.
	 *  @throws IllegalStateException when dynamic is used.
	 */
	public void	set(T value)
	{
		if(dynamic!=null)
			throw new IllegalStateException("Should not set value on dynamic belief.");
		
		if(changehandler==null)
			throw new IllegalStateException("Wrapper not inited. Missing @Belief/@GoalParameter annotation?");
		
		doSet(value);
	}
	
	/**
	 *  Set the value and throw the event.
	 *  Used e.g. for update rate.
	 */
	protected void doSet(T value)
	{
		T	old	= this.value;
		this.value	= value;
		changehandler.accept(old, value);
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}
}
