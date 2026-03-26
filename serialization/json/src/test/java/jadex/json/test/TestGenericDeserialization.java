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
 *  Check that objects with generics are property deserialized.
 */
public class TestGenericDeserialization
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
		Type	type	= TestGenericDeserialization.class.getDeclaredField("list").getGenericType();
		List<?>	result	= (List<?>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
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
		Type	type	= TestGenericDeserialization.class.getDeclaredField("nestedList").getGenericType();
		List<?>	result	= (List<?>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
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
		Type	type	= TestGenericDeserialization.class.getDeclaredField("map").getGenericType();
		Map<?,?>	result	= (Map<?,?>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
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
		Type	type	= TestGenericDeserialization.class.getDeclaredField("nestedMap").getGenericType();
		Map<?,?>	result	= (Map<?,?>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
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
		Type	type	= TestGenericDeserialization.class.getDeclaredField("record").getGenericType();
		Record<?>	result	= (Record<?>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value().getClass().getName());
	}
	
	static class Bean<T>
	{
		String	name;
		T		value;
		
		public String getName(){return name;}
		public void setName(String name){this.name = name;}
		public T getValue(){return value;}
		public void setValue(T value){this.value = value;}
	}
	Bean<Data>	bean;
	@Test
	public void testGenericBean() throws Exception
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
		Type	type	= TestGenericDeserialization.class.getDeclaredField("bean").getGenericType();
		Bean<?>	result	= (Bean<?>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.getClass().getName());
	}
	
	Bean<Bean<Data>>	beanbean;
	@Test
	public void testGenericBeanBean() throws Exception
	{
		String	json = """
		{
			"name":"test", 
			"value":
			{
				"name":"test", 
				"value":
				{
					"name":"test", 
					"value":42
				}
			}
		}
		""";
		Type	type	= TestGenericDeserialization.class.getDeclaredField("beanbean").getGenericType();
		@SuppressWarnings("unchecked")
		Bean<Bean<?>>	result	= (Bean<Bean<?>>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.value.getClass().getName());
	}
	
	static class NestedBean<T>
	{
		String	name;
		Bean<T>	value;
		
		public String getName(){return name;}
		public void setName(String name){this.name = name;}
		public Bean<T> getValue(){return value;}
		public void setValue(Bean<T> value){this.value = value;}
	}
	NestedBean<Data>	nestedbean;
	@Test
	public void testGenericNestedBean() throws Exception
	{
		String	json = """
		{
			"name":"test", 
			"value":
			{
				"name":"test", 
				"value":
				{
					"name":"test", 
					"value":42
				}
			}
		}
		""";
		Type	type	= TestGenericDeserialization.class.getDeclaredField("nestedbean").getGenericType();
		NestedBean<?>	result	= (NestedBean<?>) JsonTraverser.objectFromString(json, TestGenericDeserialization.class.getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.value.getClass().getName());
	}
}
