package jadex.bpmn.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import jadex.core.ResultProvider;


public class RBpmnProcess extends ResultProvider
{
	protected String filename;
	
	protected Map<String, Object> args;
	
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
		return args==null? Collections.EMPTY_MAP: new HashMap<>(args); 
	}
	
	/**
	 *  Get the result value.
	 *  @return the value or null, if not set.
	 */
	public Object getResult(String name)
	{
		return results.get(name); 
	}
	
	/**
	 *  Declare a result value.
	 */
	public RBpmnProcess	declareResult(String name)
	{
		results.put(name, null);
		return this;
	}
	
	/**
	 *  Declare a result value.
	 */
	public boolean hasDeclaredResult(String name)
	{
		return results.containsKey(name);
	}
	
	/**
	 *  Get the results copy.
	 *  @return The results.
	 */
	public Map<String, Object> getResults()
	{
		return new HashMap<>(results); 
	}
}