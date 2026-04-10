package jadex.json.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadex.common.SReflect.ParameterizedTypeImpl;
import jadex.common.transformation.annotations.IncludeFields;
import jadex.transformation.jsonserializer.JsonTraverser;

/**
 *  Check that objects with generics are property deserialized.
 */
public class TestGenericDeserialization
{
	@IncludeFields
	static class Data
	{
		public String	name;
		public int		value;
	}
	
	@Test
	public void testClassCastException()
	{
		String	json = """
		[
			42
		]
		""";
		Type	type	= new ParameterizedTypeImpl(List.class, Data.class);
		assertThrows(ClassCastException.class, () -> 
			JsonTraverser.objectFromString(json, getClass().getClassLoader(), type));
		
		String	json2 = """
		{
			"value":42
		}
		""";
		Type	type2	= new ParameterizedTypeImpl(Map.class, String.class, Data.class);
		assertThrows(ClassCastException.class, () -> 
			JsonTraverser.objectFromString(json2, getClass().getClassLoader(), type2));
	}
	
	@Test
	public void testSimple() throws Exception
	{
		String	json = """
		{
			"name":"test", 
			"value":42
		}
		""";
		Data	result = (Data) JsonTraverser.objectFromString(json, getClass().getClassLoader(), Data.class);
		assertEquals("test", result.name);
		assertEquals(42, result.value);
	}
	
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
		Type	type	= new ParameterizedTypeImpl(List.class, Data.class);
		List<?>	result	= (List<?>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.get(0).getClass().getName());
	}
	
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
		Type	type	= new ParameterizedTypeImpl(List.class, new ParameterizedTypeImpl(List.class, Data.class));
		List<?>	result	= (List<?>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(ArrayList.class.getName(), result.get(0).getClass().getName());
		List<?>	result2 = (List<?>) result.get(0);
		assertEquals(Data.class.getName(), result2.get(0).getClass().getName());
	}
	
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
		Type	type	= new ParameterizedTypeImpl(Map.class, String.class, Data.class);
		Map<?,?>	result	= (Map<?,?>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.get("test").getClass().getName());
	}
	
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
		Type	type	= new ParameterizedTypeImpl(Map.class, String.class, new ParameterizedTypeImpl(Map.class, String.class, Data.class));
		Map<?,?>	result	= (Map<?,?>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(LinkedHashMap.class.getName(), result.get("test").getClass().getName());
		Map<?,?>	result2 = (Map<?,?>) result.get("test");
		assertEquals(Data.class.getName(), result2.get("test2").getClass().getName());
	}
	
	static record MyRecord<T>(String name, T value){}
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
		Type	type	= new ParameterizedTypeImpl(MyRecord.class, Data.class);
		MyRecord<?>	result	= (MyRecord<?>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value().getClass().getName());
	}
	
	static class MyBean<T>
	{
		String	name;
		T		value;
		
		public String getName(){return name;}
		public void setName(String name){this.name = name;}
		public T getValue(){return value;}
		public void setValue(T value){this.value = value;}
	}
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
		Type	type	= new ParameterizedTypeImpl(MyBean.class, Data.class);
		MyBean<?>	result	= (MyBean<?>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.getClass().getName());
	}
	
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
		Type	type	= new ParameterizedTypeImpl(MyBean.class, new ParameterizedTypeImpl(MyBean.class, Data.class));
		@SuppressWarnings("unchecked")
		MyBean<MyBean<?>>	result	= (MyBean<MyBean<?>>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.value.getClass().getName());
	}
	
	static class NestedBean<E>
	{
		String	name;
		MyBean<E>	value;
		
		public String getName(){return name;}
		public void setName(String name){this.name = name;}
		public MyBean<E> getValue(){return value;}
		public void setValue(MyBean<E> value){this.value = value;}
	}
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
		Type	type	= new ParameterizedTypeImpl(NestedBean.class, Data.class);
		NestedBean<?>	result	= (NestedBean<?>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.value.getClass().getName());
	}
	
	@Test
	public void testGenericNestedBeanBean() throws Exception
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
					"value":
					{
						"name":"test", 
						"value":42
					}
				}
			}
		}
		""";
		Type	type	= new ParameterizedTypeImpl(NestedBean.class, new ParameterizedTypeImpl(MyBean.class, Data.class));
		@SuppressWarnings("unchecked")
		NestedBean<MyBean<Data>>	result	= (NestedBean<MyBean<Data>>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.value.value.getClass().getName());
	}
	
	// Test recursively resolving nested parameterized value type. 
	@IncludeFields
	class NestedBeanBean<E>
	{
		public String	name;
		public Map<String, MyBean<E>>	value;
	}
	@Test
	public void testGenericNestedBeanBeanMap() throws Exception
	{
		String	json = """
		{
			"name":"test", 
			"value":
			{
				"test":
				{
					"name":"test", 
					"value":
					{
						"name":"test", 
						"value":42
					}
				}
			}
		}
		""";
		Type	type	= new ParameterizedTypeImpl(NestedBeanBean.class, Data.class);
		@SuppressWarnings("unchecked")
		NestedBeanBean<Data>	result	= (NestedBeanBean<Data>) JsonTraverser.objectFromString(json, getClass().getClassLoader(), type);
		assertEquals(Data.class.getName(), result.value.get("test").value.getClass().getName());
	}
}
