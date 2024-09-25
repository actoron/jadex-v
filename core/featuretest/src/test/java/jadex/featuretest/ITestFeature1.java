package jadex.featuretest;

import jadex.core.IComponentFeature;
import jadex.core.impl.Component;

public interface ITestFeature1 extends IComponentFeature
{
	public static ITestFeature1	of(Component self)
	{
		return self.getFeature(ITestFeature1.class);
	}
}
