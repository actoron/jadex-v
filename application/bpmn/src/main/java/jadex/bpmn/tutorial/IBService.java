package jadex.bpmn.tutorial;

import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IBService
{
	public IFuture<Integer> add(int a, int b);
	
	public IFuture<Integer> sub(int a, int b);
	
	public IIntermediateFuture<Integer> count();
}
