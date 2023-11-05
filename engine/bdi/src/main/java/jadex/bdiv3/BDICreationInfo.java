package jadex.bdiv3;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *  Allows providing arguments when starting an agent.
 */
public class BDICreationInfo
{
	protected String	classname;
	protected Map<String, Object>	args;
	
	/**
	 *  Builder pattern constructor.
	 */
	public BDICreationInfo()
	{
	}

	/**
	 *  Info with classname already set.
	 *  @param classname	The class name of the BDI agent (with or without "bdi:" prefix.
	 */
	public BDICreationInfo(String classname)
	{
		this.classname	= classname;
	}
	
	/**
	 *  Set the class name. 
	 *  @param classname	The class name of the BDI agent (with or without "bdi:" prefix.
	 */
	public BDICreationInfo	setClassname(String classname)
	{
		this.classname	= classname;
		return this;
	}
	
	/**
	 *  Get the class name. 
	 */
	public String	getClassname()
	{
		return classname;
	}
	
	/**
	 *  Add an argument as name/value pair.
	 */
	public BDICreationInfo	addArgument(String name, Object value)
	{
		if(args==null)
			args	= new LinkedHashMap<>(2);
		args.put(name, value);
		return this;
	}
	
	/**
	 *  Get the argument value.
	 *  @return the value or null, if not set.
	 */
	public Object	getArgument(String name)
	{
		return args==null ? null : args.get(name); 
	}
}
