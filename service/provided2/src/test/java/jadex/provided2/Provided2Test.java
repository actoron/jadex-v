package jadex.provided2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.future.Future;
import jadex.injection.annotation.OnEnd;
import jadex.injection.annotation.OnStart;
import jadex.provided2.annotation.Service;

/**
 *  Test automatic provided service registration.
 */
public class Provided2Test
{
	public static final long	TIMEOUT	= 10000;
	
	//-------- test interfaces --------
	
	@Service
	public interface IMyService {}
	
	@Service
	@FunctionalInterface
	public interface IMyLambdaService
	{
		public String	myMethod(IComponent comp);
	}
	
	//-------- test methods --------
	
	@Test
	public void	testPojoService()
	{
		Future<Void>	start	= new Future<>();
		Future<Void>	end	= new Future<>();
		
		IComponentHandle	comp = IComponentManager.get().create(new IMyService()
		{
			@OnStart
			void onStart()
			{
				start.setResult(null);
			}
			
			@OnEnd
			void onEnd()
			{
				end.setResult(null);
			}
		}).get(TIMEOUT);
		
		assertTrue(start.isDone());
		assertFalse(end.isDone());
		
		// TODO: test service registration
		
		comp.terminate().get(TIMEOUT);
		assertTrue(end.isDone());
	}

	@Test
	public void	testFieldService()
	{
		Future<Void>	compstart	= new Future<>();
		Future<Void>	compend	= new Future<>();
		Future<Void>	servicestart	= new Future<>();
		Future<Void>	serviceend	= new Future<>();
		
		IComponentHandle	comp = IComponentManager.get().create(new IMyService()
		{
			@SuppressWarnings("unused")
			IMyService	myservice	= new IMyService()
			{
				@OnStart
				void onStart()
				{
					assertFalse(compstart.isDone());
					servicestart.setResult(null);
				}
				
				@OnEnd
				void onEnd()
				{
					assertFalse(compend.isDone());
					serviceend.setResult(null);
				}
			};
			
			@OnStart
			void onStart()
			{
				assertTrue(servicestart.isDone());
				compstart.setResult(null);
			}
			
			@OnEnd
			void onEnd()
			{
				assertTrue(serviceend.isDone());
				compend.setResult(null);
			}
		}).get(TIMEOUT);
		
		assertTrue(servicestart.isDone());
		assertTrue(compstart.isDone());
		assertFalse(serviceend.isDone());
		assertFalse(compend.isDone());
		
		// TODO: test service registration
		
		comp.terminate().get(TIMEOUT);
		assertTrue(serviceend.isDone());
		assertTrue(compend.isDone());
	}

	@Test
	public void	testLambdaService()
	{
		IComponentManager.get().create((IMyLambdaService)comp -> comp.getId().toString()).get();
	}
}
