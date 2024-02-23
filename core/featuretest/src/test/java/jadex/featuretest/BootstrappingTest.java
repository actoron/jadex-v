package jadex.featuretest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.core.impl.Component;
import jadex.featuretest.impl.TestFeature2Provider;
import jadex.featuretest.impl.TestLazyFeatureProvider;

public class BootstrappingTest
{
	public static List<String>	bootstraps	= new ArrayList<>();
	
	@Test
	public void	testBootstrapping()
	{
		List<String>	expected	= Arrays.asList(new String[]
		{
			TestFeature2Provider.class.getSimpleName()+"_beforeCreate",
			TestLazyFeatureProvider.class.getSimpleName()+"_beforeCreate",
			TestLazyFeatureProvider.class.getSimpleName()+"_afterCreate",
			TestFeature2Provider.class.getSimpleName()+"_afterCreate"
		});
		
		Component.createComponent(Component.class, () -> new Component(null));
		
		assertEquals(expected, bootstraps);
	}
}
