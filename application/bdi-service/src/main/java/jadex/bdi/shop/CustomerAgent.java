package jadex.bdi.shop;

import jadex.bdi.Val;
import jadex.bdi.annotation.BDIAgent;
import jadex.bdi.annotation.Belief;
import jadex.bdi.annotation.Capability;

/**
 *  Customer capability.
 */
@BDIAgent
public class CustomerAgent
{
	//-------- attributes --------

	/** The customer capability. */
	@Capability//(beliefmapping=@Mapping("money"))
	protected CustomerCapability cap = new CustomerCapability();
	
	/** The money. */
	@Belief
	protected Val<Double> money = new Val<>(100.0);
}
