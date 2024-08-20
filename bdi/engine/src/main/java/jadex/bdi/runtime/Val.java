package jadex.bdi.runtime;

import java.util.concurrent.Callable;

import jadex.bdi.model.MBelief;
import jadex.bdi.runtime.impl.BDIAgentFeature;
import jadex.common.SUtil;
import jadex.micro.impl.MicroAgentFeature;

/**
 *  Wrapper for belief values.
 *  Generates appropriate rule events.
 */
public class Val<T>
{
	T	value;
	Callable<T>	dynamic;
	
	// Fields set via reflection from BDILifecycleAgentFeature
	Object	pojo;
	MBelief	mbel;
	String	param;
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
	 *  Get the current value.
	 */
	public T get()
	{
		try
		{
			return dynamic!=null && !updaterate? dynamic.call(): value;
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}

	/**
	 *  Set the value.
	 *  @throws IllegalStateException when dynamic is used.
	 */
	public void	set(T value)
	{
		if(dynamic!=null)
			throw new IllegalStateException("Should not set value on dynamic belief.");
		
		if(mbel==null && param==null)
			throw new IllegalStateException("Wrapper not inited. Missing @Belief/@GoalParameter annotation?");
		
		// belief
		if(mbel!=null)
		{
			BDIAgentFeature.writeField(value, mbel.getField().getName(), mbel, pojo, MicroAgentFeature.get().getSelf());
		}
		
		// parameter
		else
		{
			BDIAgentFeature.writeParameterField(value, param, pojo, null);
		}
	}
	
	@Override
	public String toString()
	{
		return String.valueOf(get());
	}
}
