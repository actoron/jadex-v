package jadex.bdi.impl.goal;

import jadex.bdi.impl.plan.IPlanBody;
import jadex.bdi.impl.plan.RPlan;

/**
 *  The info objects for plan candidates.
 *  Is on model level and takes instance objects as arguments of methods.
 */
public interface ICandidateInfo
{
	/**
	 *  Get the plan instance.
	 *  @return	The plan instance.
	 */
	public /*IInternalPlan*/RPlan	createPlan(RProcessableElement reason);
	
	/**
	 *  Get the plan body of this candidate.
	 */
	public IPlanBody	getBody();
		
//	
//	/**
//	 *  Get the candidate model element.
//	 *  @return The candiate model element.
//	 */
////	public MPlan getMPlan();
//	public MElement getModelElement();
//	
//	/**
//	 *  Get the element this 
//	 *  candidate was selected for.
//	 *  @return	The processable element.
//	 */
//	public IElement getElement();
	
//	/**
//	 *  Get the raw candidate (e.g. plan pojo).
//	 *  @return The raw candiate.
//	 */
//	public Object getRawCandidate();
}
