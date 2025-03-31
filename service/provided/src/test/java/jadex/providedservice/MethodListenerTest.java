package jadex.providedservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

public class MethodListenerTest
{
	public static final long	TIMEOUT	= 10000;
	
	//-------- test interfaces/classes --------
	
	@Service
	@FunctionalInterface
	public interface IMyLambdaService
	{
		public IFuture<ComponentIdentifier>	myMethod();
	}

	//-------- tests --------
	
	@Test
	public void	testMethodListener() throws Exception
	{
		IComponentHandle	handle	= IComponentManager.get().create(
			(IMyLambdaService)() -> new Future<>(IComponentManager.get().getCurrentComponent().getId())
		).get(TIMEOUT);
		
		IMyLambdaService	service	= ProvidedServiceTest.searchService(handle, IMyLambdaService.class);
		Method	m	= IMyLambdaService.class.getMethod("myMethod");
		
		Future<IMyLambdaService>	fut1	= new Future<>();
		Future<IMyLambdaService>	fut2	= new Future<>();
		handle.scheduleStep(comp ->
		{
			comp.getFeature(IProvidedServiceFeature.class).addMethodInvocationListener(((IService)service).getServiceId(), null,
				new IMethodInvocationListener()
			{
				@Override
				public void methodCallStarted(Object proxy, Method method, Object[] args, Object callid) 
				{
					assertEquals(m, method);
					fut1.setResult((IMyLambdaService) proxy);
				}
					
				@Override
				public void methodCallFinished(Object proxy, Method method, Object[] args, Object callid)
				{
					assertEquals(m, method);
					fut2.setResult((IMyLambdaService)proxy);
				}
			});
			return null;
		}).get(TIMEOUT);
		
		service.myMethod().get();
		
		assertSame(service, fut1.get(TIMEOUT));
		assertSame(service, fut2.get(TIMEOUT));
	}
}
