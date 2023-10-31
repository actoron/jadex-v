package jadex.featuretest;

import jadex.core.impl.Component;

public interface ITestLazyFeature
{
	public static ITestLazyFeature	of(Component self)
	{
		return self.getFeature(ITestLazyFeature.class);
	}
}
