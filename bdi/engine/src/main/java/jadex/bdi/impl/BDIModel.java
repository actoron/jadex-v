package jadex.bdi.impl;

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
	// use dby @GoalAPLBuild to find plan bodies for plan pojo.
	protected Map<Class<?>, ClassPlanBody>	mplans	= new LinkedHashMap<>();
	
	/** The plans that are triggered by an instance of the element class. */
	// TODO: probably need separate MInfo in model and (R)ICandidateInfo with correct parent pojos for capability plans
	protected Map<Class<?>, List<ICandidateInfo>>	plans	= new LinkedHashMap<>();
	
	/** The known goals (goal pojoclazz -> goal annotation for meta info). */
	protected Map<Class<?>, MGoal>	goals	= new LinkedHashMap<>();
	
	
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
		mplans.put(planpojoclazz, body);
	}
	
	/**
	 *  Get plans that are triggered by an instance of the element class.
	 */
	public List<ICandidateInfo> getTriggeredPlans(Class<?> elementclass)
	{
		
		return plans.get(elementclass);
	}
	
	/**
	 *  Add a plan to the model.
	 */
	protected void	addPlanforGoal(Class<?> goalpojoclazz, String planname, IPlanBody body)
	{
		List<ICandidateInfo>	goalplans	= plans.get(goalpojoclazz);
		if(goalplans==null)
		{
			goalplans	= new ArrayList<>(4);
			plans.put(goalpojoclazz, goalplans);
		}
		goalplans.add(new APL.MPlanCandidate(planname, body));
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
		goals.put(goalpojoclazz, mgoal);
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
