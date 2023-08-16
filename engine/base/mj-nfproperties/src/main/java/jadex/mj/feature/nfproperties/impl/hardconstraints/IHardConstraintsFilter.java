package jadex.mj.feature.nfproperties.impl.hardconstraints;

import java.util.Collection;
import java.util.Map;

import jadex.common.IFilter;
import jadex.common.Tuple2;

public interface IHardConstraintsFilter extends IFilter<Tuple2<IService, Map<String, Object>>>
{
	/** Returns the value names relevant to this filter */
	public Collection<String> getRelevantValueNames();
}
