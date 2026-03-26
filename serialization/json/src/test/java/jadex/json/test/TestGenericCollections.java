package jadex.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadex.transformation.jsonserializer.JsonTraverser;

/**
 *  Check that generic collections are property deserialized.
 */
public class TestGenericCollections
{
	static class Data
	{
		String	name;
		int		value;
	}
	
	List<Data>	list;
	@Test
	public void testGenericList() throws Exception
	{
		String	json = """
		[
			{
				"name":"test", 
				"value":42
			}
		]
		""";
		Type	type	= TestGenericCollections.class.getDeclaredField("list").getGenericType();
		List<?>	result	= (List<?>) JsonTraverser.objectFromString(json, TestGenericCollections.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.get(0).getClass().getName());
	}
	
	List<List<Data>>	nestedList;
	@Test
	public void testGenericNestedList() throws Exception
	{
		String	json = """
		[
			[
				{
					"name":"test", 
					"value":42
				}
			]
		]
		""";
		Type	type	= TestGenericCollections.class.getDeclaredField("nestedList").getGenericType();
		List<?>	result	= (List<?>) JsonTraverser.objectFromString(json, TestGenericCollections.class.getClassLoader(), type);
		assertEquals(ArrayList.class.getName(), result.get(0).getClass().getName());
		List<?>	result2 = (List<?>) result.get(0);
		assertEquals(Data.class.getName(), result2.get(0).getClass().getName());
	}
	
	Map<String, Data>	map;
	@Test
	public void testGenericMap() throws Exception
	{
		String	json = """
		{
			"test":
			{
				"name":"test", 
				"value":42
			}
		}
		""";
		Type	type	= TestGenericCollections.class.getDeclaredField("map").getGenericType();
		Map<?,?>	result	= (Map<?,?>) JsonTraverser.objectFromString(json, TestGenericCollections.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.get("test").getClass().getName());
	}
	
	Map<String, Map<String, Data>>	nestedMap;
	@Test
	public void testGenericNestedMap() throws Exception
	{
		String	json = """
		{
			"test":
			{
				"test2":
				{
					"name":"test", 
					"value":42
				}
			}
		}
		""";
		Type	type	= TestGenericCollections.class.getDeclaredField("nestedMap").getGenericType();
		Map<?,?>	result	= (Map<?,?>) JsonTraverser.objectFromString(json, TestGenericCollections.class.getClassLoader(), type);
		assertEquals(LinkedHashMap.class.getName(), result.get("test").getClass().getName());
		Map<?,?>	result2 = (Map<?,?>) result.get("test");
		assertEquals(Data.class.getName(), result2.get("test2").getClass().getName());
	}
	
	static record Record<T>(String name, T value){}
	Record<Data>	record;
	@Test
	public void testGenericRecord() throws Exception
	{
		String	json = """
		{
			"name":"test", 
			"value":
			{
				"name":"test", 
				"value":42
			}
		}
		""";
		Type	type	= TestGenericCollections.class.getDeclaredField("record").getGenericType();
		Record<?>	result	= (Record<?>) JsonTraverser.objectFromString(json, TestGenericCollections.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value().getClass().getName());
	}
}
