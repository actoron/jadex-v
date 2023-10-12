package jadex.mj.featuretest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.IMjExecutionFeature;
import jadex.mj.feature.lifecycle.IMjLifecycleFeature;
import jadex.mj.featuretest.impl.MjTestFeature1NewProvider;
import jadex.mj.featuretest.impl.MjTestLifecycleFeatureProvider;
import jadex.mj.micro.MjMicroAgent;
import jadex.mj.micro.impl.MjMicroAgentFeature;

@Testable
public class FeatureTest
{
	// Test features for plain components.
	@SuppressWarnings("unchecked")
	static Class<Object>[]	COMPONENT_FEATURE_TYPES	= new Class[]
	{
		// Ordered alphabetically by fully qualified name
		IMjExecutionFeature.class,
		IMjLifecycleFeature.class,			
		IMjTestFeature1.class,
		IMjTestFeature2.class,
		IMjTestLazyFeature.class,
	};

	// Test features for micro agent components.
	@SuppressWarnings("unchecked")
	static Class<Object>[]	AGENT_FEATURE_TYPES	= new Class[]
	{
		// Ordered alphabetically by fully qualified name
		IMjExecutionFeature.class,
		IMjLifecycleFeature.class,			
		IMjTestFeature1.class,
		IMjTestFeature2.class,
		MjMicroAgentFeature.class,
		IMjTestLazyFeature.class,
	};

	@Test
	public void	testComponentLoading()
	{
		// Dummy component for feature loading.
		doTestLoading(new MjComponent(null), COMPONENT_FEATURE_TYPES);
	}
	
	@Test
	public void	testAgentLoading()
	{
		// Dummy agent component for feature loading.
		doTestLoading(new MjMicroAgent(null, null){}, AGENT_FEATURE_TYPES);
	}
	
	protected void	doTestLoading(MjComponent comp, Class<Object>[] feature_types)
	{
		for(Class<Object> type: feature_types)
		{
			Object	feature	= comp.getFeature(type);
			assertTrue(feature!=null, "Feature could not be loaded");
			assertTrue(type.isAssignableFrom(feature.getClass()), "Feature type does not match: "+feature.getClass()+", "+type);
		}
	}
	
	@Test
	public void testLazyFeature()
	{
		// Dummy component for feature loading.
		MjComponent	comp	= new MjComponent(null);

		// Lazy feature should not be found
		for(Object feature: comp.getFeatures())
		{
			assertFalse(feature instanceof IMjTestLazyFeature, "Lazy feature found: "+feature);
		}
		
		// Test that lazy feature is created on demand.
		assertTrue(comp.getFeature(IMjTestLazyFeature.class)!=null, "Lazy feature could bnot be created");
	}
	
	@Test
	public void	testComponentFeatureReplacement()
	{
		// Dummy component for feature loading.
		MjComponent	comp	= new MjComponent(null);
		
		// Test feature should be replaced
		assertTrue(comp.getFeature(IMjTestFeature1.class) instanceof MjTestFeature1NewProvider, "Feature is not replaced: "+comp.getFeature(IMjLifecycleFeature.class));
		// Lifecycle feature should not be replaced, because replacement applies only to micro agents.
		assertFalse(comp.getFeature(IMjLifecycleFeature.class) instanceof MjTestLifecycleFeatureProvider, "Feature should not be replaced: "+comp.getFeature(IMjLifecycleFeature.class));
	}
	
	@Test
	public void	testAgentFeatureReplacement()
	{
		// Dummy component for feature loading.
		MjComponent	comp	= new MjMicroAgent(null, null){};
		
		// Test feature should be replaced
		assertTrue(comp.getFeature(IMjTestFeature1.class) instanceof MjTestFeature1NewProvider, "Feature is not replaced: "+comp.getFeature(IMjLifecycleFeature.class));
		// Lifecycle feature should not be replaced, because replacement applies only to micro agents.
		assertTrue(comp.getFeature(IMjLifecycleFeature.class) instanceof MjTestLifecycleFeatureProvider, "Feature should not be replaced: "+comp.getFeature(IMjLifecycleFeature.class));
	}
	
	@Test
	public void testComponentOrdering()
	{
		// Dummy component for feature loading.
		doTestOrdering(new MjComponent(null), COMPONENT_FEATURE_TYPES);
	}
		
	@Test
	public void testAgentOrdering()
	{
		// Dummy agent component for feature loading.
		doTestOrdering(new MjMicroAgent(null, null){}, AGENT_FEATURE_TYPES);
	}
		
	protected void doTestOrdering(MjComponent comp, Class<Object>[] feature_types)
	{
		// Force instantiation of lazy features, if any
		for(Class<Object> type: feature_types)
		{
			comp.getFeature(type);
		}
		
		// Check if actual ordering matches desired ordering
		Iterator<Object>	it	= comp.getFeatures().iterator();
		for(Class<Object> featuretype: feature_types)
		{
			assertTrue(it.hasNext(), "Feature missing: "+featuretype);
			Object next	= it.next();
			assertTrue(featuretype.isAssignableFrom(next.getClass()), "Feature type does not match: "+next+", "+featuretype);
		}
		assertFalse(it.hasNext(), () -> "Unexpected feature: "+it.next());		
	}
}
