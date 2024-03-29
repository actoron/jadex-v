package jadex.bdi.runtime.impl;

import jadex.bdi.runtime.IParameterElement;


/**
 *  Abstraction for rplans and rgoals that act as plan.
 */
public interface IInternalPlan extends IParameterElement
{
	/**
	 *  Get the candidate.
	 *  @return The candidate.
	 */
	public ICandidateInfo getCandidate();
	
	/**
	 *  Test if plan has passed.
	 */
	public boolean isPassed();
	
	/**
	 *  Test if plan has failed.
	 */
	public boolean isFailed();
	
	/**
	 *  Test if plan has been aborted.
	 */
	public boolean isAborted();
	
	/**
	 *  Get the exception.
	 */
	public Exception getException();
}
