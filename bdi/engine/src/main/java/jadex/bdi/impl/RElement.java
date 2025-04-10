package jadex.bdi.impl;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import jadex.core.IComponent;

/**
 *  Base element for all runtime elements.
 */
public abstract class RElement
{
	protected static final AtomicInteger cnt	= new AtomicInteger();
	
	//-------- attributes --------

//	/** The model element. */
//	protected MElement modelelement;
	
	/** The model name (e.g. name of method or simple name of pojo class) used for id/event generation. */
	protected String	modelname;
	
	/** The element id. */
	protected String id;
	
	/** The pojo element (if any), e.g. null for method plan. */
	protected Object	pojoelement;
	
	/** The component. */
	protected IComponent	comp;
	
	/** The enclosing (e.g.) capability pojo(s). */
	protected List<Object>	parentpojos;
	
	//-------- constructors --------
	
	/**
	 *  Create a new runtime element.
	 *  
	 *  @param modelname	The model, name or null, if to be derived from pojo class.
	 *  
	 *  Either name or pojoelement must be given
	 */
	public RElement(/*MElement modelelement*/String modelname, Object pojoelement, IComponent comp, List<Object> parentpojos)
	{
//		this.modelelement = modelelement;
		if(modelname==null)
		{
			modelname	= pojoelement.getClass().getName();
		}
		this.modelname	= modelname;
		this.pojoelement	= pojoelement;
		this.comp	= comp;
		this.parentpojos	= parentpojos;
		
		
		this.id = /*name==null? "nomodel": */modelname+"_#"+cnt.incrementAndGet();
	}

	//-------- methods --------
	
//	/**
//	 *  Get the modelelement.
//	 *  @return The modelelement.
//	 */
//	public MElement getModelElement()
//	{
//		return modelelement;
//	}
//
//	/**
//	 *  Set the modelelement.
//	 *  @param modelelement The modelelement to set.
//	 */
//	public void setModelElement(MElement modelelement)
//	{
//		this.modelelement = modelelement;
//	}

	/**
	 *  Get the id.
	 *  @return The id.
	 */
	public String getId()
	{
		return id;
	}

//	/**
//	 *  Set the id.
//	 *  @param id The id to set.
//	 */
//	public void setId(String id)
//	{
//		this.id = id;
//	}

//	/**
//	 *  Get the capability.
//	 *  @return The capability.
//	 */
//	public RCapability getCapability()
//	{
//		return IInternalBDIAgentFeature.get().getCapability();
//	}
//	
//	/**
//	 *  Get the capability model.
//	 *  @return The capability model.
//	 */
//	public MCapability getMCapability()
//	{
//		return (MCapability)getCapability().getModelElement();
//	}
//	
//	/**
//	 *  get the rule system.
//	 *  @return The rule system
//	 */
//	public RuleSystem getRuleSystem()
//	{
//		return IInternalBDIAgentFeature.get().getRuleSystem();
//	}
//	
//	/**
//	 *  Get the element count.
//	 *  @return The element count.
//	 */
//	public long getCount()
//	{
//		long ret = -1;
//		int idx = id.indexOf("#");
//		if(idx!=-1)
//		{
//			ret = Long.parseLong(id.substring(idx+1));
//		}
//		return ret;
//	}
//	
//	/** 
//	 *  Get the string representation.
//	 */
//	public String toString()
//	{
//		return SReflect.getInnerClassName(this.getClass())+"(modelelement=" + modelelement + ", id=" + id + ")";
//	}
	
	/**
	 *  Get the pojo element, if any
	 *  @return The pojo element or null.
	 */
	public Object getPojo()
	{
		return pojoelement;
	}
	
	/**
	 *  Get the parent pojo elements.
	 *  @return The list of pojo elements from agent to direct containing object (e.g. [agent pojo, capability pojo] as parents of a plan).
	 */
	public List<Object> getParentPojos()
	{
		return parentpojos;
	}
	
	/**
	 *  Get the component. 
	 */
	public IComponent	getComponent()
	{
		return comp;
	}
	
	/** 
	 * 
	 */
	public String toString()
	{
		String	ret	= null;
		
		// Use pojo.toString() if not Object.toString()
		if(pojoelement!=null)
		{
			try
			{
				Method	m	= pojoelement.getClass().getMethod("toString");
				if(!m.getDeclaringClass().equals(Object.class))
				{
					ret	= pojoelement.toString();
				}
			}
			catch (Exception e)
			{
			}
		}
		
//		if(ret==null)
//		{
//			if(id.lastIndexOf('$')!=-1)
//			{
//				ret	= id.substring(id.lastIndexOf('$')+1);
//			}
//			else if(id.lastIndexOf('.')!=-1)
//			{
//				ret	= id.substring(id.lastIndexOf('.')+1);
//			}
//			else
//			{
				ret	= id;
//			}
//		}
		
		return ret;
	}
}
