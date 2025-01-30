package jadex.micro.agentmethods;

public class Person
{
	protected String name;
	
	protected int age;

	public Person() 
	{
	}
	
	public Person(String name, int age) 
	{
		this.name = name;
		this.age = age;
	}

	public String getName() 
	{
		return name;
	}

	public void setName(String name) 
	{
		this.name = name;
	}

	public int getAge() 
	{
		return age;
	}

	public void setAge(int age) 
	{
		this.age = age;
	}

	@Override
	public String toString() 
	{
		return "Person [name=" + name + ", age=" + age + ", hashcode=" + hashCode() + "]";
	}
}