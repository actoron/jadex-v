package jadex.bpmn.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jadex.core.ResultProvider;


public class RBpmnProcess extends ResultProvider
{
	protected String filename;
	
	protected Map<String, Object> args;
	
	protected Set<String> onlydeclared = new HashSet<>();
	
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
		onlydeclared.add(name);
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
	 *  Do not notify the pure declarations.
	 */
	public boolean checkInitialNotify(String name, Object value)
	{
		return !onlydeclared.contains(name);
	}
	
	public void addResult(String name, Object value)
	{
		onlydeclared.remove(name);
		super.addResult(name, value);
	}
	
	/**
	 *  Get the results copy.
	 *  @return The results.
	 */
	public Map<String, Object> getResults()
	{
		return new HashMap<>(results); 
	}

	/**
	 *  Get a string representation.
	 */
	public String	toString()
	{
		return "RBpmnProcess("+filename+")";
	}
}
