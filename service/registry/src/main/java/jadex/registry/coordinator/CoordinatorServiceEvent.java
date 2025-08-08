package jadex.registry.coordinator;

import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.ServiceEvent;

public class CoordinatorServiceEvent extends ServiceEvent 
{
    private long starttime;

    public CoordinatorServiceEvent(IServiceIdentifier id, int type, long starttime) 
    {
        super(id, type);
        this.starttime = starttime;
    }

    public long getStartTime() 
    {
        return starttime;
    }

	public void setStartTime(long starttime) 
	{
		this.starttime = starttime;
	}
}