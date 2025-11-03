package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;

import jadex.core.IComponentManager;
import jadex.injection.annotation.DynamicValue;
import jadex.injection.impl.InjectionModel;

/**
 *  Test various cases for initializing a Dyn
 *  and check that the dependency is properly detected.
 */
public class DynBytecodeAnalysisTest
{
	static class DynPojo
	{
		// Base dependency
		@DynamicValue
		Val<String>	val	= new Val<String>("initial");
		Val<String>	getVal(){return val;}
		
		// Test method reference
//		@DynamicValue
//		Dyn<String>	dyn1	= new Dyn<>(val::get);
//		@DynamicValue
//		Dyn<String>	dyn2	= new Dyn<>(getVal()::get);

		// Test lambda
		@DynamicValue
		Dyn<String>	dyn3	= new Dyn<>(() -> val.get());
		@DynamicValue
		Dyn<String>	dyn4	= new Dyn<>(() -> getVal().get());
		
		// Test anonymous class
		@DynamicValue
		Dyn<String>	dyn5	= new Dyn<>(new Callable<>()
			{public String call() throws Exception {return val.get();}});
		@DynamicValue
		Dyn<String>	dyn6	= new Dyn<>(new Callable<>()
			{public String call() throws Exception {return getVal().get();}});
		
//		@DynamicValue
//		Dyn<String>	dyn7	= new Dyn<>(createInnerCallable());
//		Callable<String>	createInnerCallable() {return val::get;}
		
//		@DynamicValue
//		Dyn<String>	dyn8	= new Dyn<>(createOuterCallable(this));
	}
	static Callable<String>	createOuterCallable(DynPojo pojo) {return pojo.val::get;}
	
	@Test
	public void	testDependencies()
	{
		// Initialize feature providers
		IComponentManager.get().create(new DynPojo()).get(10000);
		
		for(int i=1; i<=8; i++)
		{
			doTestDependency(i);
		}
	}
	
	void	doTestDependency(int i)
	{
		String	name	= "dyn"+i;
		try
		{
			InjectionModel	model	= InjectionModel.getStatic(Collections.singletonList(DynPojo.class), null, null);
			Field	f	= DynPojo.class.getDeclaredField(name);
			Set<String>	deps	= model.findDependentFields(f);
			assertEquals(Collections.singleton("val"), deps, name);
		}
		catch(NoSuchFieldException e)
		{
			// Ignore -> allow outcommenting fields
//			e.printStackTrace();
		}
	}
}
