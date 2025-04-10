package jadex.bdi.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.bdi.impl.goal.APL;
import jadex.bdi.impl.goal.ICandidateInfo;
import jadex.injection.impl.IInjectionHandle;

/**
 *  Meta info about the agent/capability etc.
 */
public class BDIModel
{
	// TODO: probably need separate MInfo in model and (R)ICandidateInfo with correct parent pojos for capability plans 
	protected Map<Class<?>, List<ICandidateInfo>>	plans	= new LinkedHashMap<>();
	
	/**
	 *  Get plans that are triggered by an instance of the element class.
	 * @return 
	 */
	public List<ICandidateInfo> getTriggeredPlans(Class<?> elementclass)
	{
		return plans.get(elementclass);
	}
	
	/**
	 *  Add a plan to the model.
	 */
	protected void	addPlanforGoal(Class<?> goaltype, String planname, IInjectionHandle planhandle)
	{
		List<ICandidateInfo>	goalplans	= plans.get(goaltype);
		if(goalplans==null)
		{
			goalplans	= new ArrayList<>(4);
			plans.put(goaltype, goalplans);
		}
		goalplans.add(new APL.MethodPlanCandidate(planname, planhandle));
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
