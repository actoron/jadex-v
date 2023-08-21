package jadex.mj.featuretest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadex.mj.core.MjComponent;
import jadex.mj.core.impl.SComponentFactory;
import jadex.mj.featuretest.impl.MjTestFeature2Provider;
import jadex.mj.featuretest.impl.MjTestLazyFeatureProvider;

public class BootstrappingTest
{
	public static List<String>	bootstraps	= new ArrayList<>();
	
	@Test
	public void	testBootstrapping()
	{
		List<String>	expected	= Arrays.asList(new String[]
		{
			MjTestFeature2Provider.class.getSimpleName()+"_beforeCreate",
			MjTestLazyFeatureProvider.class.getSimpleName()+"_beforeCreate",
			MjTestLazyFeatureProvider.class.getSimpleName()+"_afterCreate",
			MjTestFeature2Provider.class.getSimpleName()+"_afterCreate"
		});
		
		SComponentFactory.createComponent(MjComponent.class, () -> new MjComponent(null){});
		
		assertEquals(expected, bootstraps);
	}
}
