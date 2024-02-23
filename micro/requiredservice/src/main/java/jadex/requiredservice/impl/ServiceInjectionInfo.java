package jadex.requiredservice.impl;

import jadex.common.FieldInfo;
import jadex.common.MethodInfo;
import jadex.requiredservice.RequiredServiceInfo;

/**
 *  Struct for injection info.
 */
public class ServiceInjectionInfo
{
	/** The fieldinfo. */
	protected FieldInfo fieldinfo;
	
	/** The methodinfo. */
	protected MethodInfo methodinfo;
	
	/** The lazy flag. */
	protected Boolean lazy;
	
	/** The query flag. */
	protected Boolean query;
	
	/** The required flag (fail if not present). */
	protected Boolean required;
	
	/** The active period for query. */
	protected long active;
	
	/** The required service info. */
	protected RequiredServiceInfo reqserinfo;

	/**
	 *  Create a new injection info.
	 */
	public ServiceInjectionInfo()
	{
	}
	
	/**
	 *  Create a new injection info.
	 * /
	public ServiceInjectionInfo(MethodInfo methodInfo, boolean query)
	{
		this.methodinfo = methodInfo;
		this.query = query;
	}*/
	
	/**
	 *  Create a new injection info.
	 * /
	public ServiceInjectionInfo(FieldInfo fieldInfo, boolean lazy, boolean query)
	{
		this.fieldinfo = fieldInfo;
		this.lazy = lazy;
		this.query = query;
	}*/
	
	/**
	 *  Create a new injection info.
	 * /
	public ServiceInjectionInfo(FieldInfo fieldInfo, boolean lazy, boolean query, RequiredServiceInfo reqserinfo)
	{
		this.fieldinfo = fieldInfo;
		this.lazy = lazy;
		this.query = query;
		this.reqserinfo = reqserinfo;
	}*/

	/**
	 *  Get the fieldInfo.
	 *  @return the fieldInfo
	 */
	public FieldInfo getFieldInfo()
	{
		return fieldinfo;
	}

	/**
	 *  Set the fieldInfo.
	 *  @param fieldInfo The fieldInfo to set
	 */
	public ServiceInjectionInfo setFieldInfo(FieldInfo fieldInfo)
	{
		this.fieldinfo = fieldInfo;
		return this;
	}

	/**
	 *  Get the methodInfo.
	 *  @return the methodInfo
	 */
	public MethodInfo getMethodInfo()
	{
		return methodinfo;
	}

	/**
	 *  Set the methodInfo.
	 *  @param methodInfo The methodInfo to set
	 */
	public ServiceInjectionInfo setMethodInfo(MethodInfo methodInfo)
	{
		this.methodinfo = methodInfo;
		return this;
	}

	/**
	 *  Get the lazy.
	 *  @return the lazy
	 */
	public Boolean getLazy()
	{
		return lazy;
	}

	/**
	 *  Set the lazy.
	 *  @param lazy The lazy to set
	 */
	public ServiceInjectionInfo setLazy(Boolean lazy)
	{
		this.lazy = lazy;
		return this;
	}

	/**
	 *  Get the query.
	 *  @return the query
	 */
	public Boolean getQuery()
	{
		return query;
	}

	/**
	 *  Set the query.
	 *  @param query The query to set
	 */
	public ServiceInjectionInfo setQuery(Boolean query)
	{
		this.query = query;
		return this;
	}

	/**
	 *  Get the required service info.
	 *  @return The requiredServiceInfo
	 */
	public RequiredServiceInfo getRequiredServiceInfo()
	{
		return reqserinfo;
	}

	/**
	 *  Set the required service info.
	 *  @param requiredServiceInfo the requiredServiceInfo to set
	 */
	public ServiceInjectionInfo setRequiredServiceInfo(RequiredServiceInfo requiredServiceInfo)
	{
		this.reqserinfo = requiredServiceInfo;
		return this;
	}

	/**
	 * @return the required
	 */
	public Boolean getRequired() 
	{
		return required;
	}

	/**
	 * @param required the required to set
	 */
	public void setRequired(Boolean required) 
	{
		this.required = required;
	}

	/**
	 * @return the active
	 */
	public long getActive() 
	{
		return active;
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(long active) 
	{
		this.active = active;
	}
}