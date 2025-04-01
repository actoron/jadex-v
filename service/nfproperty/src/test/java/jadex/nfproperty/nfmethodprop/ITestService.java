package jadex.nfproperty.nfmethodprop;

import jadex.future.IFuture;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;
import jadex.nfproperty.sensor.service.ExecutionTimeProperty;
import jadex.nfproperty.sensor.service.WaitqueueProperty;
import jadex.providedservice.annotation.Service;

@NFProperties(
{	
	@NFProperty(ExecutionTimeProperty.class),
	@NFProperty(WaitqueueProperty.class)
})
@Service
public interface ITestService
{
	@NFProperties({@NFProperty(WaitqueueProperty.class), @NFProperty(ExecutionTimeProperty.class)})
	public IFuture<Void> methodA(long wait);
	
	@NFProperties({@NFProperty(ExecutionTimeProperty.class), @NFProperty(WaitqueueProperty.class)})
	public IFuture<Void> methodB(long wait);
}
