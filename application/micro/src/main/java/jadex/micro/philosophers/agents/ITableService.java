package jadex.micro.philosophers.agents;

import jadex.core.IExternalAccess;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ITableService 
{
	public void addPhilosopher(int no);
	
	public IFuture<IExternalAccess> getPhilosopher(int no);
	
	public IFuture<IExternalAccess> getStickOwner(int no);
	
	public void notifyAllPhilosophers();
	
	public IFuture<Void> getLeftStick(int no);
	
	public IFuture<Void> getRightStick(int no);
	
	public void releaseLeftStick(int no);
	
	public void releaseRightStick(int no);
	
	public IFuture<Boolean> isWaitForClicks();
	
	public void invertWaitForClicks();
}
