package jadex.micro.philosophers.agents;

import jadex.future.IFuture;
import jadex.micro.philosophers.PhilosopherState;
import jadex.providedservice.annotation.Service;

@Service
public interface IPhilosopherService 
{
	public IFuture<Integer> getNo();
	
	public IFuture<PhilosopherState> getState();
	
	public void notifyPhilosopher(long time);
	
	public IFuture<Integer> getEatCnt();
}
