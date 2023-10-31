package jadex.featuretest;

import jadex.core.impl.Component;

public interface ITestFeature2
{
	public static ITestFeature2	of(Component self)
	{
		return self.getFeature(ITestFeature2.class);
	}
}
