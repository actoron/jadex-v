package jadex.featuretest;

import jadex.core.impl.Component;

public interface ITestFeature1
{
	public static ITestFeature1	of(Component self)
	{
		return self.getFeature(ITestFeature1.class);
	}
}
