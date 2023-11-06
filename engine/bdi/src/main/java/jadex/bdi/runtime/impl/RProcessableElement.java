package jadex.bdi.runtime.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jadex.bdi.model.MConfigParameterElement;
import jadex.bdi.model.MParameter;
import jadex.bdi.model.MProcessableElement;
import jadex.bdi.model.MParameter.Direction;
import jadex.execution.IExecutionFeature;

/**
 *  Runtime element for all elements that can be processed via means-end reasoning.
 */
public abstract class RProcessableElement extends RParameterElement
{
	/** The allowed states. */
	public static enum State
	{
		INITIAL, 
		UNPROCESSED,
		APLAVAILABLE,
		METALEVELREASONING,
		NOCANDIDATES,
		CANDIDATESSELECTED
	};
	
	/** The pojo element. */
	protected Object pojoelement;
	
	/** The applicable plan list. */
	protected APL apl;
	
	/** The tried plans. */
	protected List<IInternalPlan> triedplans;
	
	/** The state. */
	protected State state;

	/**
	 *  Create a new element.
	 */
	public RProcessableElement(MProcessableElement modelelement, Object pojoelement, Map<String, Object> vals, MConfigParameterElement config)
	{
		super(modelelement, vals, config);
		this.pojoelement = pojoelement;
		this.state = State.INITIAL;
	}

	/**
	 *  Get the apl.
	 *  @return The apl.
	 */
	public APL getApplicablePlanList()
	{
		if(apl==null)
			apl = new APL(this);
		return apl;
	}

	/**
	 *  Set the apl.
	 *  @param apl The apl to set.
	 */
	public void setApplicablePlanList(APL apl)
	{
//		if(apl==null)
//			System.out.println("set apl to null: "+this);
		this.apl = apl;
	}

	/**
	 *  Get the pojoelement.
	 *  @return The pojoelement.
	 */
	public Object getPojoElement()
	{
		return pojoelement;
	}

	/**
	 *  Set the pojoelement.
	 *  @param pojoelement The pojoelement to set.
	 */
	public void setPojoElement(Object pojoelement)
	{
		this.pojoelement = pojoelement;
	}
	
	/**
	 *  Add a tried plan.
	 */
	public void addTriedPlan(IInternalPlan plan)
	{
		if(triedplans==null)
		{
			triedplans = new ArrayList<IInternalPlan>();
		}
		triedplans.add(plan);
	}
	
	/**
	 *  Get the triedplans.
	 *  @return The triedplans.
	 */
	public List<IInternalPlan> getTriedPlans()
	{
		return triedplans;
	}

	/**
	 *  Set the triedplans.
	 *  @param triedplans The triedplans to set.
	 */
	public void setTriedPlans(List<IInternalPlan> triedplans)
	{
		this.triedplans = triedplans;
	}

	/**
	 *  Get the state.
	 *  @return The state.
	 */
	public State getState()
	{
		return state;
	}

	/**
	 *  Set the state.
	 */
	public void setState(State state)
	{
		if(getState().equals(state))
			return;
			
		this.state = state;
		
		// start MR when state gets to unprocessed
		if(State.UNPROCESSED.equals(state))
		{
			IExecutionFeature.get().scheduleStep(new FindApplicableCandidatesAction(this));
		}
//		else if(PROCESSABLEELEMENT_APLAVAILABLE.equals(state))
//		{
//			ia.getExternalAccess().scheduleStep(new SelectCandidatesAction(this));
//		}
//		else if(PROCESSABLEELEMENT_CANDIDATESSELECTED.equals(state))
//		{
//			ia.getExternalAccess().scheduleStep(new ExecutePlanStepAction(this, rplan));
//		}
//		else if(PROCESSABLEELEMENT_NOCANDIDATES.equals(state))
//		{
//			
//		}
//		PROCESSABLEELEMENT_METALEVELREASONING
		
	}

	/**
	 *  Called when plan execution has finished.
	 */
	public void planFinished(IInternalPlan rplan)
	{
		if(rplan!=null)
		{
			if(apl!=null)
			{
				// do not add tried plan if apl is already reset because procedural
				// goal semantics is wrong otherwise (isProceduralSucceeded)
				addTriedPlan(rplan);
				apl.planFinished(rplan);
			}
		}
	}
	
	/**
	 *  Test if parameter writes are currently allowed.
	 *  @throws Exception when write not ok.
	 */
	public void	testWriteOK(MParameter mparam)
	{
		if(mparam!=null)
		{
			boolean	ok	= mparam.getDirection()==Direction.INOUT
				|| getState()==null	// in constructor -> initParameters
				|| mparam.getDirection()==Direction.IN && getState()==State.INITIAL
				|| mparam.getDirection()==Direction.OUT && getState()!=State.INITIAL;
			if(!ok)
			{
				throw new IllegalStateException("Cannot write parameter "+getModelElement().getName()+"."+mparam.getName()+" in state "+getState()+".");
			}
		}
	}
}