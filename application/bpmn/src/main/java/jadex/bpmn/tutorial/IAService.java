package jadex.bpmn.tutorial;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IAService
{
	public IFuture<String> appendHello(String text);
}
