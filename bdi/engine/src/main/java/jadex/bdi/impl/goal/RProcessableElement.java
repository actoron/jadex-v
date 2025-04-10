package jadex.bdi.impl.goal;

import java.lang.reflect.Method;
import java.util.List;

import jadex.bdi.impl.RElement;
import jadex.bdi.impl.plan.RPlan;
import jadex.core.IComponent;

/**
 *  Runtime element for all elements that can be processed via means-end reasoning.
 */
public abstract class RProcessableElement extends RElement//extends RParameterElement
{
	/** The applicable plan list. */
	protected APL apl;
	
//	/** The tried plans. */
//	protected List<IInternalPlan> triedplans;
	
	/**
	 *  Create a new element.
	 */
	public RProcessableElement(/*MProcessableElement modelelement,*/Object pojoelement, IComponent comp, List<Object> pojoparents/*, Map<String, Object> vals*/)
	{
		super(null, pojoelement, comp, pojoparents);
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

//	/**
//	 *  Add a tried plan.
//	 */
//	public void addTriedPlan(IInternalPlan plan)
//	{
//		if(triedplans==null)
//		{
//			triedplans = new ArrayList<IInternalPlan>();
//		}
//		triedplans.add(plan);
//	}
//	
//	/**
//	 *  Get the triedplans.
//	 *  @return The triedplans.
//	 */
//	public List<IInternalPlan> getTriedPlans()
//	{
//		return triedplans;
//	}
//
//	/**
//	 *  Set the triedplans.
//	 *  @param triedplans The triedplans to set.
//	 */
//	public void setTriedPlans(List<IInternalPlan> triedplans)
//	{
//		this.triedplans = triedplans;
//	}

	/**
	 *  Called when plan execution has finished.
	 */
	public void planFinished(/*IInternalPlan*/RPlan rplan)
	{
		if(rplan!=null)
		{
			if(apl!=null)
			{
//				// do not add tried plan if apl is already reset because procedural
//				// goal semantics is wrong otherwise (isProceduralSucceeded)
//				addTriedPlan(rplan);
//				apl.planFinished(rplan);
			}
		}
	}
	
//	/**
//	 *  Test if parameter writes are currently allowed.
//	 *  @throws Exception when write not ok.
//	 */
//	public void	testWriteOK(MParameter mparam)
//	{
//		if(mparam!=null)
//		{
//			boolean	ok	= mparam.getDirection()==Direction.INOUT
//				|| getState()==null	// in constructor -> initParameters
//				|| mparam.getDirection()==Direction.IN && getState()==State.INITIAL
//				|| mparam.getDirection()==Direction.OUT && getState()!=State.INITIAL;
//			if(!ok)
//			{
//				throw new IllegalStateException("Cannot write parameter "+getModelElement().getName()+"."+mparam.getName()+" in state "+getState()+".");
//			}
//		}
//	}
	
	/** 
	 * 
	 */
	public String toString()
	{
		String	ret	= null;
		
		// Use pojo.toString() if not Object.toString()
		if(getPojo()!=null)
		{
			try
			{
				Method	m	= getPojo().getClass().getMethod("toString");
				if(!m.getDeclaringClass().equals(Object.class))
				{
					ret	= getPojo().toString();
				}
			}
			catch (Exception e)
			{
			}
		}
		
		if(ret==null)
		{
			if(id.lastIndexOf('$')!=-1)
			{
				ret	= id.substring(id.lastIndexOf('$')+1);
			}
			else if(id.lastIndexOf('.')!=-1)
			{
				ret	= id.substring(id.lastIndexOf('.')+1);
			}
			else
			{
				ret	= id;
			}
		}
		
		return ret;
	}
}