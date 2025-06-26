package jadex.featuretest;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.featuretest.impl.TestFeature2NewProvider.SubComponent;
import jadex.future.Future;

public class CreationTest
{
	public static long	TIMEOUT	= 10000;
	
	@Test
	public void	testBasicCreation()
	{
		IComponentHandle	handle	= IComponentManager.get().create(17).get(TIMEOUT);
		Future<Class<?>>	type	= (Future<Class<?>>)handle.scheduleStep((IThrowingFunction<IComponent, Class<?>>)comp -> comp.getClass());
		assertSame(Component.class, type.get(TIMEOUT));
	}

	@Test
	public void	testSubtypeCreation()
	{
		IComponentHandle	handle	= IComponentManager.get().create(17.4).get(TIMEOUT);
		Future<Class<?>>	type	= (Future<Class<?>>)handle.scheduleStep((IThrowingFunction<IComponent, Class<?>>)comp -> comp.getClass());
		assertSame(SubComponent.class, type.get(TIMEOUT));
	}

	@Test
	public void	testFailedCreation()
	{
		assertThrows(RuntimeException.class, () -> IComponentManager.get().create("17.4").get(TIMEOUT));
	}
}
