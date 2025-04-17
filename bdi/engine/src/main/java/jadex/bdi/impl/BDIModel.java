package jadex.bdi.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.bdi.annotation.Goal;
import jadex.bdi.impl.goal.APL;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.bdi.impl.plan.IPlanBody;

/**
 *  Meta info about the agent/capability etc.
 */
public class BDIModel
{
	/**
	 *  Meta-info for a goal.
	 *  @param target Is target condition present?
	 *  @param maintain Is maintain condition present?
	 *  @param annotation goal flags.
	 */ 
	public record MGoal(boolean target, boolean maintain, Goal annotation) {};
	
	
	/** The plans that are triggered by an instance of the element class. */
	// TODO: probably need separate MInfo in model and (R)ICandidateInfo with correct parent pojos for capability plans
	protected Map<Class<?>, List<ICandidateInfo>>	plans	= new LinkedHashMap<>();
	
	/** The known goals (goal pojoclazz -> goal annotation for meta info). */
	protected Map<Class<?>, MGoal>	goals	= new LinkedHashMap<>();
	
	
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
		goalplans.add(new APL.PlanCandidate(planname, body));
	}
	
	/**
	 *  Get meta info for a goal.
	 */
	public MGoal getGoalInfo(Class<?> goalpojoclazz)
	{
		return goals.get(goalpojoclazz);
	}
	
	/**
	 *  Add goal meta info.
	 */
	protected void	addGoal(Class<?> goalpojoclazz, boolean target, boolean maintain, Goal annotation)
	{
		goals.put(goalpojoclazz, new MGoal(target, maintain, annotation));
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
