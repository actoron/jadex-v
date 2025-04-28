package jadex.remoteservices;

import jadex.core.ComponentIdentifier;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

@Service
public interface ITestService
{
    public IFuture<ComponentIdentifier> getComponentName();
}
