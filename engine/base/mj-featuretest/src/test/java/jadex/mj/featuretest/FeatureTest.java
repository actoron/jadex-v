package jadex.mj.featuretest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import jadex.common.SUtil;
import jadex.mj.core.MjComponent;
import jadex.mj.core.service.annotation.OnStart;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;
import jadex.mj.featuretest.impl.MjTestFeature1aProvider;
import jadex.mj.featuretest.impl.MjTestLifecycleFeatureProvider;
import jadex.mj.micro.MjMicroAgent;

@Testable
public class FeatureTest
{
	// Test features.
	@SuppressWarnings("unchecked")
	static Class<Object>[]	FEATURE_TYPES	= new Class[]
	{
		IMjTestFeature1.class,
		IMjTestFeature2.class,
		IMjTestLazyFeature.class,
		IMjExecutionFeature.class,
		IMjLifecycleFeature.class				
	};

	@Test
	public void	testLoading()
	{
		// Dummy component for feature loading.
		MjComponent	comp	= new MjComponent() {};
		
		for(Class<Object> type: FEATURE_TYPES)
		{
			Object	feature	= comp.getFeature(type);
			assertTrue(feature!=null, "Feature could not be loaded");
			assertTrue(type.isAssignableFrom(feature.getClass()), "Feature type does not match: "+feature.getClass()+", "+type);
		}
		
		CompletableFuture<Void>	result	= new CompletableFuture<>();
		MjMicroAgent.create(new Object() {
//			@MjAgent
//			MjMicroAgent	self;
			
			@OnStart
			public void start()
			{
				// TODO: check for micro feature.
				result.complete(null);
			}
		});
		
		try
		{
			result.get();
		}
		catch(Exception e)
		{
			SUtil.throwUnchecked(e);
		}
	}
	
	@Test
	public void testLazyFeature()
	{
		// Dummy component for feature loading.
		MjComponent	comp	= new MjComponent() {};

		// Lazy feature should not be found
		for(Object feature: comp.getFeatures())
		{
			assertFalse(feature instanceof IMjTestLazyFeature, "Lazy feature found: "+feature);
		}
		
		// Test that lazy feature is created on demand.
		assertTrue(comp.getFeature(IMjTestLazyFeature.class)!=null, "Lazy feature could bnot be created");
	}
	
	@Test
	public void	testFeatureReplacement()
	{
		// Dummy component for feature loading.
		MjComponent	comp	= new MjComponent() {};
		
		assertTrue(comp.getFeature(IMjLifecycleFeature.class) instanceof MjTestLifecycleFeatureProvider, "Feature is not sub feature: "+comp.getFeature(IMjLifecycleFeature.class));
		assertTrue(comp.getFeature(IMjTestFeature1.class) instanceof MjTestFeature1aProvider, "Feature is not sub feature: "+comp.getFeature(IMjLifecycleFeature.class));
	}
	
	@Test
	public void testOrdering()
	{
		// Dummy component for feature loading.
		MjComponent	comp	= new MjComponent() {};

		// Check if actual ordering matches desired ordering
		for(Object feature: comp.getFeatures())
		{
			System.out.println("Feature: "+feature);
		}
		
	}
}
