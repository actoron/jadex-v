package jadex.featuretest;

import jadex.core.IComponentFeature;
import jadex.core.impl.Component;

public interface ITestFeature2 extends IComponentFeature
{
	public static ITestFeature2	of(Component self)
	{
		return self.getFeature(ITestFeature2.class);
	}
}
