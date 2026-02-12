package jadex.bpmn.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class RBpmnProcess
{
	protected String filename;
	
	protected Map<String, Object> args;
	
	/** Context parameters with one of these names will be set as result. */
	protected Set<String> resultnames = new HashSet<>();
	
	/**
	 *  Builder pattern constructor.
	 */
	public RBpmnProcess()
	{
	}
	
	/**
	 *  Info with classname already set.
	 *  @param classname The class name of the BDI agent (with or without "bdi:" prefix.
	 */
	public RBpmnProcess(String filename)
	{
		this.filename = filename;
	}
	
	/**
	 *  Set the class name. 
	 *  @param classname	The class name of the BDI agent (with or without "bdi:" prefix.
	 */
	public RBpmnProcess	setFilename(String filename)
	{
		this.filename = filename;
		return this;
	}
	
	/**
	 *  Get the file name. 
	 */
	public String getFilename()
	{
		return filename;
	}
	
	/**
	 *  Add an argument as name/value pair.
	 */
	public RBpmnProcess	addArgument(String name, Object value)
	{
		if(args==null)
			args = new LinkedHashMap<>();
		args.put(name, value);
		return this;
	}
	
	/**
	 *  Get the argument value.
	 *  @return the value or null, if not set.
	 */
	public Object getArgument(String name)
	{
		return args==null? null: args.get(name); 
	}
	
	public boolean hasArgument(String name)
	{
		return args==null? false: args.containsKey(name);
	}
	
	/**
	 *  Get the arguments copy.
	 *  @return The arguments.
	 */
	public Map<String, Object> getArguments()
	{
		return args==null? Collections.emptyMap(): new HashMap<>(args); 
	}
	
	/**
	 *  Declare a result value.
	 */
	public RBpmnProcess	declareResult(String name)
	{
		resultnames.add(name);
		return this;
	}
	
	/**
	 *  Declare a result value.
	 */
	public boolean hasDeclaredResult(String name)
	{
		return resultnames.contains(name);
	}
	
	/**
	 *  Get a string representation.
	 */
	public String	toString()
	{
		return "RBpmnProcess("+filename+")";
	}
}
