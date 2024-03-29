package jadex.bdi.runtime.impl;

import jadex.bdi.model.MCapability;
import jadex.bdi.model.MElement;
import jadex.common.SReflect;
import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
import jadex.rules.eca.RuleSystem;

/**
 *  Base element for all runtime elements.
 */
public abstract class RElement
{
	protected static long cnt;
	
	//-------- attributes --------

	/** The model element. */
	protected MElement modelelement;
		
	/** The element id. */
	protected String id;
	
	//-------- constructors --------
	
	/**
	 *  Create a new runtime element.
	 */
	public RElement(MElement modelelement)
	{
		this.modelelement = modelelement;
		this.id = modelelement==null? "nomodel": modelelement.getName()+"_#"+cnt++;
	}

	//-------- methods --------
	
	/**
	 *  Get the modelelement.
	 *  @return The modelelement.
	 */
	public MElement getModelElement()
	{
		return modelelement;
	}

	/**
	 *  Set the modelelement.
	 *  @param modelelement The modelelement to set.
	 */
	public void setModelElement(MElement modelelement)
	{
		this.modelelement = modelelement;
	}

	/**
	 *  Get the id.
	 *  @return The id.
	 */
	public String getId()
	{
		return id;
	}

	/**
	 *  Set the id.
	 *  @param id The id to set.
	 */
	public void setId(String id)
	{
		this.id = id;
	}

//	/** 
//	 *  Get the hashcode.
//	 *  @return The hashcode.
//	 */
//	public int hashCode()
//	{
//		return 31 + id.hashCode();
//	}
//
//	/** 
//	 *  Test if equal to other object.
//	 *  @param obj The other object.
//	 *  @return True, if equal.
//	 */
//	public boolean equals(Object obj)
//	{
//		boolean ret = false;
//		if(obj instanceof RElement)
//		{
//			ret = ((RElement)obj).getId().equals(getId());
//		}
//		return ret;
//	}
	
	/**
	 *  Get the agent.
	 *  @return The agent
	 */
	public IComponent getAgent()
	{
		return IExecutionFeature.get().getComponent();
	}
	
	/**
	 *  Get the capability.
	 *  @return The capability.
	 */
	public RCapability getCapability()
	{
		return IInternalBDIAgentFeature.get().getCapability();
	}
	
	/**
	 *  Get the capability model.
	 *  @return The capability model.
	 */
	public MCapability getMCapability()
	{
		return (MCapability)getCapability().getModelElement();
	}
	
	/**
	 *  get the rule system.
	 *  @return The rule system
	 */
	public RuleSystem getRuleSystem()
	{
		return IInternalBDIAgentFeature.get().getRuleSystem();
	}
	
	/**
	 *  Get the element count.
	 *  @return The element count.
	 */
	public long getCount()
	{
		long ret = -1;
		int idx = id.indexOf("#");
		if(idx!=-1)
		{
			ret = Long.parseLong(id.substring(idx+1));
		}
		return ret;
	}
	
	/** 
	 *  Get the string representation.
	 */
	public String toString()
	{
		return SReflect.getInnerClassName(this.getClass())+"(modelelement=" + modelelement + ", id=" + id + ")";
	}
}
