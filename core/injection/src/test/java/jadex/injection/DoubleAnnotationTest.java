package jadex.injection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.IThrowingConsumer;
import jadex.future.Future;
import jadex.injection.annotation.DynamicValue;
import jadex.injection.annotation.ProvideResult;

/**
 *  Test that a dynamic value can be annotated
 *  with different kinds at the same time.
 */
public class DoubleAnnotationTest
{
	long	TIMEOUT	= 10000;

	@DynamicValue
	@ProvideResult
	Val<String>	val	= new Val<String>("");
	
	@Test
	void testDoubleAnnotation()
	{
		IComponentHandle	handle	= IComponentManager.get()
			.create(this).get(TIMEOUT);
		
		Future<String>	result	= new Future<>();		
		handle.subscribeToResults().next(event ->
			result.setResult((String) event.value()));
		
		Future<String>	value	= new Future<>();
		handle.scheduleStep((IThrowingConsumer<IComponent>)	comp ->
			comp.getFeature(IInjectionFeature.class).addListener("val", event ->
				value.setResult((String) event.value()))).get(TIMEOUT);
		
		handle.scheduleStep((IThrowingConsumer<IComponent>)	comp -> val.set("test")).get(TIMEOUT);
		
		assertEquals("test", value.get(TIMEOUT));
		assertEquals("test", result.get(TIMEOUT));
	}
}
