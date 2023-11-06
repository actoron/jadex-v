package jadex.bdi.runtime;

import jadex.bdi.runtime.impl.BDIAgentFeature;
import jadex.core.IComponent;

/**
 *  Base class for non-bytecode-enhanced BDI agents.
 */
public class BDIBaseGoal
{
	/** The bdi agent. */
	public IComponent __agent; // IBDIClassGenerator.AGENT_FIELD_NAME
	
	/** The global name. */
	public String __globalname; // GLOBALNAME_FIELD_NAME
	
	/**
	 *  Set a value and throw the change events.
	 *  @param paramname The name.
	 *  @param value The value.
	 */
	public void setParameterValue(String paramname, Object value)
	{
		BDIAgentFeature.writeParameterField(value, paramname, this);
	}
}
