package jadex.registry;

import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.impl.search.ServiceEvent;

public class CoordinatorServiceEvent extends ServiceEvent 
{
    private final long starttime;

    public CoordinatorServiceEvent(IServiceIdentifier id, int type, long starttime) 
    {
        super(id, type);
        this.starttime = starttime;
    }

    public long getStartTime() 
    {
        return starttime;
    }
}