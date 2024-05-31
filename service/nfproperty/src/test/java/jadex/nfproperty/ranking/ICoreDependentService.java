package jadex.nfproperty.ranking;

import jadex.future.IFuture;
import jadex.nfproperty.annotation.NFProperties;
import jadex.nfproperty.annotation.NFProperty;

/**
 *  Empty Test Service for non-functional properties.
 */
@NFProperties(@NFProperty(CoreNumberProperty.class))
public interface ICoreDependentService
{
	/**
	 *  Service method for test purposes.
	 */
	public IFuture<Void> testMethod();
}
