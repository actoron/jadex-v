package jadex.micro.philosophers.thread;

import jadex.micro.philosophers.AbstractTableGui;
import jadex.micro.philosophers.PhilosopherState;

public class TableGui extends AbstractTableGui
{
	protected Table t;
	
	public TableGui(int n, Table t)
	{
		super(n);
		this.t = t;
	}
		
	public PaintData readData(int n) 
	{
	    PhilosopherState[] states = new PhilosopherState[n];
	    Integer[] eatCounts = new Integer[n];
	    Integer[] stickOwners = new Integer[n];
	    
	    if(t!=null)
	    {
		    for(int i = 0; i < n; i++) 
		    {
		        states[i] = t.getPhilosopher(i).getState();
		        eatCounts[i] = t.getPhilosopher(i).getEatCnt();
		        stickOwners[i] = t.getStickOwner(i)!=null? t.getStickOwner(i).getNo(): null;
		    }
	    }
	    
	    return new PaintData(states, eatCounts, stickOwners);
	}

	public void notifyPhilosopher(int no, long time)
	{
		t.getPhilosopher(no).notifyPhilosopher(0);
	}
	
	public boolean isWaitForClicks()
	{
		return t.isWaitForClicks();
	}
	
	public void invertWaitForClicks()
	{
		t.invertWaitForClicks();
	}
	
	public void notifyAllPhilosophers()
	{
		t.notifyAllPhilosophers();
	}
}
