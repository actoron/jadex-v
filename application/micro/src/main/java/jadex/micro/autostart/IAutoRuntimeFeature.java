package jadex.micro.autostart;

import jadex.core.IRuntimeFeature;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface IAutoRuntimeFeature	extends IRuntimeFeature
{
	public IFuture<String>	getCompName();
}
