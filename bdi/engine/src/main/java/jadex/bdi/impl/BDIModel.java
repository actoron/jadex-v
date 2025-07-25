package jadex.bdi.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bdi.impl.goal.APL;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.bdi.impl.goal.MGoal;
import jadex.bdi.impl.plan.ClassPlanBody;
import jadex.bdi.impl.plan.IPlanBody;

/**
 *  Meta info about the agent/capability etc.
 */
public class BDIModel
{
	/** The plan bodies for plan classes. */
	// used by @GoalAPLBuild to find plan bodies for plan pojos.
	protected Map<Class<?>, ClassPlanBody>	mplans	= new LinkedHashMap<>();
	
	/** The plans that are triggered by an instance of the element class (e.g. goal). */
	protected Map<Class<?>, List<ICandidateInfo>>	triggeredplans	= new LinkedHashMap<>();
	
	/** The known goals (goal pojoclazz -> goal annotation for meta info). */
	protected Map<Class<?>, MGoal>	goals	= new LinkedHashMap<>();
	
	/** The known beliefs (name->value type, only for static checking, not used at runtime except for bdi viewer). */
	protected Map<String, Class<?>>	beliefs_byname	= new LinkedHashMap<>();
	
	/** The known beliefs (field->name, only for static checking, not used at runtime). */
	protected Map<Field, String>	beliefs_byfield	= new LinkedHashMap<>();
	
	/** The capabilities (path -> prefix). */
	protected Map<List<Class<?>>, String>	capabilities	= new LinkedHashMap<>();	
	
	/**
	 *  Get plan body for pojo class.
	 */
	public ClassPlanBody	getPlanBody(Class<?> pojoclazz)
	{
		
		return mplans.get(pojoclazz);
	}
	
	/**
	 *  Add a plan to the model.
	 */
	protected void	addPlanBody(Class<?> planpojoclazz, ClassPlanBody body)
	{
		if(mplans.put(planpojoclazz, body)!=null)
		{
			throw new UnsupportedOperationException("Plan cannot be declared twice: "+planpojoclazz);
		}
	}
	
	/**
	 *  Get plans that are triggered by an instance of the element class.
	 */
	public List<ICandidateInfo> getTriggeredPlans(Class<?> elementclass)
	{
		
		return triggeredplans.get(elementclass);
	}
	
	/**
	 *  Add a plan to the model.
	 */
	protected void	addPlanforGoal(Class<?> goalpojoclazz, List<Class<?>> planparents, String planname, IPlanBody body)
	{
		List<ICandidateInfo>	goalplans	= triggeredplans.get(goalpojoclazz);
		if(goalplans==null)
		{
			goalplans	= new ArrayList<>(4);
			triggeredplans.put(goalpojoclazz, goalplans);
		}
		goalplans.add(new APL.MPlanCandidate(planparents, planname, body));
	}
	
	/**
	 *  Get meta info for a goal.
	 */
	public MGoal getGoalInfo(Class<?> goalpojoclazz)
	{
		return goals.get(goalpojoclazz);
	}
	
	/**
	 *  Get the pojo goal classes of the agent.
	 */
	public Set<Class<?>> getGoaltypes()
	{
		return goals.keySet();
	}
	
	/**
	 *  Add goal meta info.
	 */
	protected void	addGoal(Class<?> goalpojoclazz, MGoal mgoal)
	{
		if(goals.put(goalpojoclazz, mgoal)!=null)
		{
			throw new UnsupportedOperationException("Goal cannot be declared twice: "+goalpojoclazz);
		}
	}
	
	/**
	 *  Add a belief.
	 */
	protected void	addBelief(String name, Class<?> type, Field field)
	{
		if(beliefs_byname.containsKey(name))
		{
			throw new UnsupportedOperationException("Belief cannot be declared twice: "+name);
		}
		if(beliefs_byfield.containsKey(field))
		{
			throw new UnsupportedOperationException("Belief field cannot be declared twice: "+field);
		}
		
		beliefs_byfield.put(field, name);
		beliefs_byname.put(name, type);
	}
	
	/**
	 *  Get a belief type or null.
	 */
	public Class<?>	getBeliefType(String name)
	{
		return beliefs_byname.get(name);
	}
	
	/**
	 *  Get a belief name or null.
	 */
	public String	getBeliefName(Field field)
	{
		return beliefs_byfield.get(field);
	}
	
	/**
	 *  Get the beliefs
	 */
	public Set<String>	getBeliefNames()
	{
		return beliefs_byname.keySet();
	}
	
	/**
	 *  Add a capability.
	 */
	protected void	addCapability(List<Class<?>> pojoclazzes, List<String> capanames)
	{
		String	prefix	= "";
		for(String name: capanames.reversed())
		{
			prefix 	= name + "." + prefix;
		}
		capabilities.put(pojoclazzes, prefix);
	}
	
	/**
	 *  Get the capabilities.
	 */
	protected Set<List<Class<?>>>	getCapabilities()
	{
		return capabilities.keySet();
	}
	
	public String getCapabilityPrefix(List<Class<?>> pojoclazzes)
	{
		return capabilities.get(pojoclazzes);
	}

	//-------- static part --------
	
	/** The cached bdi models.
	 *  The model is for the complete agent including all sub capabilities.
	 *  Thus only class->model and not List<Class>->model. */
	protected static final Map<Class<?>, BDIModel>	MODELS	= new LinkedHashMap<>();
	
	/**
	 *  Get the model for a pojo class
	 */
	protected static BDIModel	getModel(Class<?> pojoclazz)
	{
		BDIModel	ret;
		synchronized(MODELS)
		{
			ret	= MODELS.get(pojoclazz);
			if(ret==null)
			{
				ret	= new BDIModel();
				MODELS.put(pojoclazz, ret);
			}
		}
		return ret;
	}
}
