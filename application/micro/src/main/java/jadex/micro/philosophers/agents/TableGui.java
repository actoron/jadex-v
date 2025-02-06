package jadex.micro.philosophers.agents;

import java.util.function.Function;

import jadex.core.IComponentHandle;
import jadex.micro.philosophers.AbstractTableGui;
import jadex.micro.philosophers.PhilosopherState;
import jadex.providedservice.IProvidedServiceFeature;

public class TableGui extends AbstractTableGui
{
	protected IComponentHandle ta;
	
	public TableGui(int n, IComponentHandle ta)
	{
		super(n);
		this.ta = ta;
	}
		
	public PaintData readData(int n) 
	{
	    PhilosopherState[] states = new PhilosopherState[n];
	    Integer[] eatCounts = new Integer[n];
	    Integer[] stickOwners = new Integer[n];
	    
	    if(ta!=null)
	    {
		    for(int i = 0; i < n; i++) 
		    {
		        states[i] = (PhilosopherState) executeOnPhilo(i, ser -> ser.getState());
		        eatCounts[i] = (Integer) executeOnPhilo(i, ser -> ser.getEatCnt());
		        
		        final int fi = i;
		        IComponentHandle p = (IComponentHandle) executeOnTable(ser -> ser.getStickOwner(fi).get());
		        stickOwners[i] = p != null ? (Integer) executeOnPhilo(p, ser -> ser.getNo().get()) : null;
		    }
	    }
	    
	    return new PaintData(states, eatCounts, stickOwners);
	}

	public void notifyPhilosopher(int no, long time)
	{
		executeOnPhilo(no, ser -> {ser.notifyPhilosopher(time); return null;});
	}
	
	public boolean isWaitForClicks()
	{
		return (Boolean)executeOnTable(ser -> ser.isWaitForClicks().get());
	}
	
	public void invertWaitForClicks()
	{
		executeOnTable(ser -> {ser.invertWaitForClicks(); return null;});
	}
	
	public void notifyAllPhilosophers()
	{
		executeOnTable(ser -> {ser.notifyAllPhilosophers(); return null;});
	}
	
	protected Object executeOnTable(Function<ITableService, Object> cmd)
	{
		return ta.scheduleStep(agent ->
		{
			ITableService pas = agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITableService.class);
			return cmd.apply(pas);
			
		}).get();
	}
	
	protected Object executeOnPhilo(int no, Function<IPhilosopherService, Object> cmd)
	{
		return ta.scheduleStep(agent ->
		{
			ITableService tas = agent.getFeature(IProvidedServiceFeature.class).getProvidedService(ITableService.class);
			IComponentHandle pa = tas.getPhilosopher(no).get();
			if(pa==null)
				return null;
			return pa.scheduleStep(pagent ->
			{
				IPhilosopherService pas = pagent.getFeature(IProvidedServiceFeature.class).getProvidedService(IPhilosopherService.class);
				return cmd.apply(pas);
			}).get();
			
		}).get();
	}
	
	protected Object executeOnPhilo(IComponentHandle philo, Function<IPhilosopherService, Object> cmd)
	{
		if(philo==null)
			return null;
    	return philo.scheduleStep(pagent ->
		{
			IPhilosopherService pas = pagent.getFeature(IProvidedServiceFeature.class).getProvidedService(IPhilosopherService.class);
			return cmd.apply(pas);
		}).get();
	}
}
