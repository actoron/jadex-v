package jadex.micro;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadex.common.ClassInfo;
import jadex.common.FieldInfo;
import jadex.common.MethodInfo;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.Tuple3;
import jadex.model.ServiceCallInfo;
import jadex.model.impl.CacheableKernelModel;
import jadex.model.modelinfo.IModelInfo;

/**
 *  The micro agent model. 
 */
public class MicroModel extends CacheableKernelModel
{
	/** The micro agent class. */
	protected ClassInfo pojoclass;
	
	///** The class loader. */
	//protected ClassLoader classloader;
	
	/** The agent methods for given annotations (if any). */
	protected Map<Class<? extends Annotation>, MethodInfo>	agentmethods;
	
	/** The injection info. */
	protected InjectionInfoHolder ii;
	
	/**
	 *  Create a new model.
	 */
	public MicroModel(IModelInfo modelinfo)
	{
		super(modelinfo);
		this.ii = new InjectionInfoHolder();
	}
	
	/**
	 *  Get the injection info holder.
	 *  @return The injection info.
	 */
	public InjectionInfoHolder getInjectionInfoHolder()
	{
		return ii;
	}

	/**
	 *  Add an injection field.
	 *  @param field The field. 
	 */
	public void addAgentInjection(FieldInfo field)
	{
		ii.addAgentInjection(field);
	}
	
	/**
	 *  Get the agent injection fields.
	 *  @return The fields.
	 */
	public FieldInfo[] getAgentInjections()
	{
		return ii.getAgentInjections();
	}
	
	/**
	 *  Add an injection field.
	 *  @param field The field. 
	 */
	public void addParentInjection(FieldInfo field)
	{
		ii.addParentInjection(field);
	}
	
	/**
	 *  Get the parentinjections fields.
	 *  @return The fields.
	 */
	public FieldInfo[] getParentInjections()
	{
		return ii.getParentInjections();
	}
	
	/**
	 *  Add an injection field.
	 *  @param name The name.
	 *  @param field The field. 
	 */
	public void addArgumentInjection(String name, FieldInfo field, String convert)
	{
		ii.addArgumentInjection(name, field, convert);
	}
	
	/**
	 *  Get the argument injection fields.
	 *  @return The fields.
	 */
	public Tuple2<FieldInfo, String>[] getArgumentInjections(String name)
	{
		return ii.getArgumentInjections(name);
	}
	
	/**
	 *  Get the argument injection names.
	 *  @return The names.
	 */
	public String[] getArgumentInjectionNames()
	{
		return ii.getArgumentInjectionNames();
	}
	
	/**
	 *  Add an injection field.
	 *  @param name The name.
	 *  @param field The field. 
	 */
	public void addResultInjection(String name, FieldInfo field, String convert, String convback)
	{
		ii.addResultInjection(name, field, convert, convback);
	}
	
	/**
	 *  Get the result injection field.
	 *  @return The fields.
	 */
	public Tuple3<FieldInfo, String, String> getResultInjection(String name)
	{
		return ii.getResultInjection(name);
	}
	
	/**
	 *  Get the Result injection names.
	 *  @return The names.
	 */
	public String[] getResultInjectionNames()
	{
		return ii.getResultInjectionNames();
	}
	
//	/**
//	 *  Add an injection field.
//	 *  @param name The name.
//	 *  @param field The field. 
//	 */
//	public void addServiceInjection(String name, FieldInfo field, boolean lazy, boolean query)
//	{
//		ii.addServiceInjection(name, field, lazy, query);
//	}
//	
//	/**
//	 *  Add an injection method.
//	 *  @param name The name.
//	 *  @param method The method. 
//	 */
//	public void addServiceInjection(String name, MethodInfo method)
//	{
//		ii.addServiceInjection(name, method);
//	}
//	
//	/**
//	 *  Add an injection field.
//	 *  @param name The name.
//	 *  @param method The method. 
//	 */
//	public void addServiceInjection(String name, MethodInfo method, boolean query)
//	{
//		ii.addServiceInjection(name, method, query);
//	}
	
	/**
	 *  Get the service injection fields.
	 *  @return The field or method infos.
	 * /
	public ServiceInjectionInfo[] getServiceInjections(String name)
	{
		return ii.getServiceInjections(name);
	}*/
	
	/**
	 *  Get the service injection names.
	 *  @return The names.
	 * /
	public String[] getServiceInjectionNames()
	{
		return ii.getServiceInjectionNames();
	}*/
	
	/**
	 *  Add an injection field.
	 *  @param name The name.
	 *  @param field The field. 
	 */
	public void addFeatureInjection(String name, FieldInfo field)
	{
		ii.addFeatureInjection(name, field);
	}
	
	/**
	 *  Get the feature injection fields.
	 *  @return The fields.
	 */
	public FieldInfo[] getFeatureInjections()
	{
		return ii.getFeatureInjections();
	}
	
	/**
	 *  Add an call field.
	 *  @param name The name.
	 *  @param field The field. 
	 */
	public void addServiceCall(ServiceCallInfo call)
	{
		ii.addServiceCall(call);
	}
	
	/**
	 *  Get the service call fields.
	 *  @return The field or method infos.
	 */
	public List<ServiceCallInfo> getServiceCalls()
	{
		return ii.getServiceCalls();
	}
	
	/**
	 *  Set the service calls.
	 */
	public void setServiceCalls(List<ServiceCallInfo> servicecalls)
	{
		ii.setServiceCalls(servicecalls);
	}
	
	/**
	 *  Get the pojo class.
	 *  @return The pojoclass.
	 */
	public ClassInfo getPojoClass()
	{
		return pojoclass;
	}

	/**
	 *  Set the pojo class.
	 *  @param pojoclass The pojoclass to set
	 */
	public void setPojoClass(ClassInfo pojoclass)
	{
		this.pojoclass = pojoclass;
	}

	/**
	 *  Get the classloader.
	 *  @return the classloader.
	 * /
	public ClassLoader getClassloader()
	{
		return classloader;
	}*/

	/**
	 *  Set the classloader.
	 *  @param classloader The classloader to set.
	 * /
	public void setClassloader(ClassLoader classloader)
	{
		this.classloader = classloader;
	}*/

	/**
	 *  Set an agent method.
	 */
	public void setAgentMethod(Class<? extends Annotation> ann, MethodInfo mi)
	{
		if(agentmethods==null)
			agentmethods = new HashMap<Class<? extends Annotation>, MethodInfo>();
		
		if(!agentmethods.containsKey(ann))
		{
			agentmethods.put(ann, mi);
		}
		else
		{
			MethodInfo prev = agentmethods.get(ann);
			if(SUtil.equals(mi.getClassName(), prev.getClassName()))
			{
				throw new RuntimeException("Only one @"+ann.getSimpleName()+" method allowed in "+mi.getClassName());
			}
		}
	}
	
	/**
	 *  Get an agent method.
	 */
	public MethodInfo getAgentMethod(Class<? extends Annotation> ann)
	{
		return agentmethods!=null ? agentmethods.get(ann) : null;
	}
	
	
}
