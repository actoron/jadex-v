package jadex.bpmn.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.common.NameValue;
import jadex.future.ISubscriptionIntermediateFuture;
import jadex.future.SubscriptionIntermediateFuture;


public class RBpmnProcess 
{
	protected String filename;
	protected Map<String, Object> args;
	protected Map<String, Object> results;
	protected List<SubscriptionIntermediateFuture<NameValue>> resultsubscribers;
	
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
			args	= new LinkedHashMap<>(2);
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
	
	/**
	 *  Get the arguments copy.
	 *  @return The arguments.
	 */
	public Map<String, Object> getArguments()
	{
		return args==null? Collections.EMPTY_MAP: new HashMap<>(args); 
	}
	
	/**
	 *  Add an result as name/value pair.
	 */
	public RBpmnProcess	addResult(String name, Object value)
	{
		if(results==null)
			results = new LinkedHashMap<>(2);
		results.put(name, value);
		notifyResult(name, value);
		return this;
	}
	
	/**
	 *  Get the result value.
	 *  @return the value or null, if not set.
	 */
	public Object getResult(String name)
	{
		return results==null? null: results.get(name); 
	}
	
	/**
	 *  Get the results copy.
	 *  @return The results.
	 */
	public Map<String, Object> getResults()
	{
		return results==null? Collections.EMPTY_MAP: new HashMap<>(results); 
	}
	
	protected void notifyResult(String name, Object value)
	{
		if(resultsubscribers!=null)
		{
			NameValue val = new NameValue(name, value);
			resultsubscribers.forEach(sub -> sub.addIntermediateResult(val));
		}
	}
	
	public synchronized ISubscriptionIntermediateFuture<NameValue> subscribeToResults()
	{
		SubscriptionIntermediateFuture<NameValue> ret = new SubscriptionIntermediateFuture<>();
		
		if(resultsubscribers==null)
			resultsubscribers = new ArrayList<>();
		
		resultsubscribers.add(ret);
		
		ret.setTerminationCommand(ex ->
		{
			resultsubscribers.remove(ret);
		});
		
		return ret;
	}
}