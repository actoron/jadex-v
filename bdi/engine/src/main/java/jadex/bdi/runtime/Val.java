package jadex.bdi.runtime;

import jadex.bdi.model.MBelief;
import jadex.bdi.runtime.impl.BDIAgentFeature;
import jadex.micro.impl.MicroAgentFeature;

public class Val<T>
{
	T	value;
	Object	pojo;
	MBelief	mbel;
	
	public Val(T value)
	{
		this.value	= value;
	}
	
	public T get()
	{
		return value;
	}
	
	public void	set(T value)
	{
		BDIAgentFeature.writeField(value, mbel.getField().getName(), mbel, pojo, MicroAgentFeature.get().getSelf());
	}
}
