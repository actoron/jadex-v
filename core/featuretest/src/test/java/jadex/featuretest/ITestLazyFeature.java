package jadex.featuretest;

import jadex.core.IComponentFeature;
import jadex.core.impl.Component;

public interface ITestLazyFeature extends IComponentFeature
{
	public static ITestLazyFeature	of(Component self)
	{
		return self.getFeature(ITestLazyFeature.class);
	}
}
