package jadex.bt;

import java.lang.System.Logger.Level;
import java.util.concurrent.Callable;

import jadex.bt.impl.BTAgentFeature;
import jadex.common.SUtil;
import jadex.execution.IExecutionFeature;
import jadex.micro.impl.MicroAgentFeature;

/**
 *  Wrapper for belief values.
 *  Generates appropriate rule events.
 */
public class Val<T>
{
	protected T value;
	protected Callable<T> dynamic;
	protected long updaterate;
	
	protected Object pojo;
	protected String name;
	
	/**
	 *  Create belief value with a given value.
	 */
	public Val(T value)
	{
		this.value = value;
	}
	
	/**
	 * Create belief value with a dynamic function.
	 */
	public Val(Callable<T> dynamic, long updaterate)
	{
		this.dynamic = dynamic;
		this.updaterate = updaterate;
	}
	
	private void ensureInitialized() 
    {
        if (this.name == null) 
            autoInit();
    }
	
	public void init(Object pojo, String name)
	{
		if(this.name!=null)
			return;
		
		this.pojo = pojo;
		this.name = name;
		if(dynamic!=null)
		{
			writeField();
			if(updaterate>0)
				performUpdates();
		}
		else if(value!=null)
		{
			set(value); // reset the value to generate initial event
		}
	}
	
	private void autoInit() 
	{
	    if (this.name == null) 
	    {
	        this.pojo = IExecutionFeature.get().getComponent().getPojo();
	        Class<?> clazz = pojo.getClass();

	        while (clazz != null) 
	        {
	            for (var field : clazz.getDeclaredFields()) 
	            {
	                field.setAccessible(true);
	                try 
	                {
	                    if (field.get(pojo) == this) 
	                    {
	                    	init(pojo, field.getName());
	                        return;
	                    }
	                } 
	                catch (IllegalAccessException e) 
	                {
	                    throw new RuntimeException("Field access error", e);
	                }
	            }
	            clazz = clazz.getSuperclass(); 
	        }

	        throw new IllegalStateException("Could not determine field name for Val");
	    }
	}
	
	private void performUpdates()
	{
		IExecutionFeature.get().waitForDelay(updaterate).then(Void ->
		{
			writeField();
			IExecutionFeature.get().scheduleStep(() -> performUpdates());
		}).printOnEx();
	}
	
	private void writeField()
	{
		try
		{
			BTAgentFeature.writeField(dynamic.call(), name, pojo, MicroAgentFeature.get().getSelf());
		}
		catch(Exception e)
		{
			SUtil.rethrowAsUnchecked(e);
		}
	}
	
	private void writeField(T value)
	{
		try
		{
			BTAgentFeature.writeField(value, name, pojo, MicroAgentFeature.get().getSelf());
		}
		catch(Exception e)
		{
			SUtil.rethrowAsUnchecked(e);
		}
	}
	
	/**
	 *  Get the current value.
	 */
	public T get()
	{
		ensureInitialized();
		
		try
		{
			return dynamic!=null && updaterate==0? dynamic.call(): value;
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
		ensureInitialized();
		
		//System.out.println("setting val: "+value);
		
		//if(value instanceof Val)
		//	System.out.println("val set: "+value);
		
		if(dynamic!=null)
			throw new IllegalStateException("Should not set value on dynamic belief.");
		
		if(name==null)
		{
			//System.out.println("val not inited, no event: "+value);
	  		System.getLogger(this.getClass().getName()).log(Level.INFO, "val not inited, no event: "+value);

			this.value = value;
			return;
			//throw new IllegalStateException("Wrapper not inited. Missing @Belief/@GoalParameter annotation.");
		}
		
		writeField(value);
	}
	
	@Override
	public String toString()
	{
		return "Val: "+String.valueOf(get());
	}
}
