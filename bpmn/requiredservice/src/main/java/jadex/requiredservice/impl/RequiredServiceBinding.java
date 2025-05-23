package jadex.requiredservice.impl;

import java.util.ArrayList;
import java.util.List;

import jadex.common.UnparsedExpression;
import jadex.providedservice.ServiceScope;

/**
 *  Required service binding information.
 */
public class RequiredServiceBinding
{
	//-------- attributes --------
	
	/** The service name. */
	protected String name;
	
	/** The component name used for searching. */
	protected String componentname;
	
	/** The component type, i.e. the model name used for searching. */
	protected String componenttype;

	///** Information about the component to create. */
	//protected ComponentInstanceInfo creationinfo;
	
	// Decided to drop caching support for v4
//	/** Flag if binding is dynamic. */
//	protected boolean dynamic;

	/** The search scope. */
	protected ServiceScope scope;
		
	/** The scope expression (if any). */
	protected UnparsedExpression scopeexpression;
	
//	/** The create flag. */
//	protected boolean create;
	// Dropped support for v4
	
//	/** The recover flag. */
//	protected boolean recover;
	// Dropped support for v4
	
	/** The interceptors. */
	protected List<UnparsedExpression> interceptors;
	
	//-------- constructors --------

	/**
	 *  Create a new binding. 
	 */
	public RequiredServiceBinding()
	{
	}
	
	/**
	 *  Create a new binding. 
	 */
	public RequiredServiceBinding(String name, ServiceScope scope)
	{
		this(name, null, null,
			scope, null);
	}

	/**
	 *  Create a new binding.
	 */
	public RequiredServiceBinding(String name, String componentname, String componenttype,
		ServiceScope scope, UnparsedExpression[] interceptors)
	{
		this.name = name;
		this.componentname = componentname;
		this.componenttype = componenttype;
		this.scope = scope;
		if(interceptors!=null)
		{
			for(int i=0; i<interceptors.length; i++)
			{
				addInterceptor(interceptors[i]);
			}
		}
	}
	
	/**
	 *  Create a new binding.
	 */
	public RequiredServiceBinding(RequiredServiceBinding orig)
	{
		this(orig.getName(), orig.getComponentName(), orig.getComponentType(), 
			orig.getScope(), orig.getInterceptors()	);
	}

	//-------- methods --------
	
	/**
	 *  Get the creationinfo.
	 *  @return The creationinfo.
	 * /
	public ComponentInstanceInfo getCreationInfo()
	{
		return creationinfo;
	}*/

	/**
	 *  Set the creationinfo.
	 *  @param creationinfo The creationinfo to set.
	 * /
	public CreationInfo setCreationInfo(ComponentInstanceInfo creationinfo)
	{
		this.creationinfo = creationinfo;
	}*/

	/**
	 *  Get the name.
	 *  @return the name.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 *  Set the name.
	 *  @param name The name to set.
	 */
	public RequiredServiceBinding setName(String name)
	{
		this.name = name;
		return this;
	}

	/**
	 *  Get the componentname.
	 *  @return the componentname.
	 */
	public String getComponentName()
	{
		return componentname;
	}

	/**
	 *  Set the componentname.
	 *  @param componentname The componentname to set.
	 */
	public RequiredServiceBinding setComponentName(String componentname)
	{
		this.componentname = componentname;
		return this;
	}

	/**
	 *  Get the componenttype.
	 *  @return the componenttype.
	 */
	public String getComponentType()
	{
		return componenttype;
	}

	/**
	 *  Set the componenttype.
	 *  @param componenttype The componenttype to set.
	 */
	public RequiredServiceBinding setComponentType(String componenttype)
	{
		this.componenttype = componenttype;
		return this;
	}

	/**
	 *  Get the scope.
	 *  @return the scope.
	 */
	public ServiceScope getScope()
	{
		return scope;
	}

	/**
	 *  Set the scope.
	 *  @param scope The scope to set.
	 */
	public RequiredServiceBinding setScope(ServiceScope scope)
	{
		this.scope = scope;
		return this;
	}
	
	/**
	 *  Get the scope expression.
	 *  @return The scope expression.
	 */
	public UnparsedExpression getScopeExpression()
	{
		return scopeexpression;
	}

	/**
	 *  Set the scope expression.
	 *  @param expression The scope expression to set.
	 */
	public RequiredServiceBinding setScopeExpression(UnparsedExpression expression)
	{
		this.scopeexpression = expression;
		return this;
	}
	
	/**
	 *  Add an interceptor.
	 *  @param interceptor The interceptor.
	 */
	public RequiredServiceBinding addInterceptor(UnparsedExpression interceptor)
	{
		if(interceptors==null)
			interceptors = new ArrayList<UnparsedExpression>();
		interceptors.add(interceptor);
		return this;
	}
	
	/**
	 *  Remove an interceptor.
	 *  @param interceptor The interceptor.
	 */
	public RequiredServiceBinding removeInterceptor(UnparsedExpression interceptor)
	{
		interceptors.remove(interceptor);
		return this;
	}
	
	/**
	 *  Get the interceptors.
	 *  @return All interceptors.
	 */
	public UnparsedExpression[] getInterceptors()
	{
		return interceptors==null? new UnparsedExpression[0]:
			interceptors.toArray(new UnparsedExpression[interceptors.size()]);
	}
	
//	/**
//	 *  Get the creationname.
//	 *  @return The creationname.
//	 */
//	public String getCreationName()
//	{
//		return creationname;
//	}
//
//	/**
//	 *  Set the creationname.
//	 *  @param creationname The creationname to set.
//	 */
//	public void setCreationName(String creationname)
//	{
//		this.creationname = creationname;
//	}
//
//	/**
//	 *  Get the creationtype.
//	 *  @return The creationtype.
//	 */
//	public String getCreationType()
//	{
//		return creationtype;
//	}
//
//	/**
//	 *  Set the creationtype.
//	 *  @param creationtype The creationtype to set.
//	 */
//	public void setCreationType(String creationtype)
//	{
//		this.creationtype = creationtype;
//	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return " scope=" + scope + ", componentname=" + componentname
			+ ", componenttype="+ componenttype	;//+" , creationcomp="+creationinfo;
	}

	
}
