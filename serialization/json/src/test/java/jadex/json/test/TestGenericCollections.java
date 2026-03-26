package jadex.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.transformation.jsonserializer.JsonTraverser;

/**
 *  Check that generic collections are property deserialized.
 */
public class TestGenericCollections
{
	List<Data>	forthetype;
	
	static class Data
	{
		String	name;
		int		value;
	}
	
	@Test
	public void testGenericCollections() throws Exception
	{
		String	json = "[{\"name\":\"test\", \"value\":42}]";
		Type	type	= TestGenericCollections.class.getDeclaredField("forthetype").getGenericType();
		List<?>	result	= (List<?>) JsonTraverser.objectFromString(json, TestGenericCollections.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.get(0).getClass().getName());
	}
}
