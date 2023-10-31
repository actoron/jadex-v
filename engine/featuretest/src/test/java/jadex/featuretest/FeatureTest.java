package jadex.featuretest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;

import jadex.core.impl.Component;
import jadex.feature.execution.IExecutionFeature;
import jadex.featuretest.impl.TestFeature1NewProvider;
import jadex.featuretest.impl.TestFeature2NewProvider;
import jadex.micro.MicroAgent;
import jadex.micro.impl.MicroAgentFeature;
import jadex.model.IModelFeature;

@Testable
public class FeatureTest
{
	// Test features for plain components.
	@SuppressWarnings("unchecked")
	static Class<Object>[]	COMPONENT_FEATURE_TYPES	= new Class[]
	{
		// Ordered alphabetically by fully qualified name of provider (wtf?)
		IExecutionFeature.class,
		ITestFeature1.class,
		ITestFeature2.class,
		ITestLazyFeature.class,
	};

	// Test features for micro agent components.
	@SuppressWarnings("unchecked")
	static Class<Object>[]	AGENT_FEATURE_TYPES	= new Class[]
	{
		// Ordered alphabetically by fully qualified name of provider (wtf?)
		IExecutionFeature.class,
		ITestFeature1.class,
		ITestFeature2.class,
		MicroAgentFeature.class,
		IModelFeature.class,
		ITestLazyFeature.class,
	};

	@Test
	public void	testComponentLoading()
	{
		// Dummy component for feature loading.
		doTestLoading(new Component(null), COMPONENT_FEATURE_TYPES);
	}
	
	@Test
	public void	testAgentLoading()
	{
		// Dummy agent component for feature loading.
		doTestLoading(new MicroAgent(null, null){}, AGENT_FEATURE_TYPES);
	}
	
	protected void	doTestLoading(Component comp, Class<Object>[] feature_types)
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
		Component	comp	= new Component(null);

		// Lazy feature should not be found
		for(Object feature: comp.getFeatures())
		{
			assertFalse(feature instanceof ITestLazyFeature, "Lazy feature found: "+feature);
		}
		
		// Test that lazy feature is created on demand.
		assertTrue(comp.getFeature(ITestLazyFeature.class)!=null, "Lazy feature could bnot be created");
	}
	
	@Test
	public void	testComponentFeatureReplacement()
	{
		// Dummy component for feature loading.
		Component	comp	= new Component(null);
		
		// IMjTestFeature1 feature should be replaced
		assertTrue(comp.getFeature(ITestFeature1.class) instanceof TestFeature1NewProvider, "Feature is not replaced: "+comp.getFeature(ITestFeature1.class));
		// IMjTestFeature2 feature should not be replaced, because replacement applies only to micro agents.
		assertFalse(comp.getFeature(ITestFeature2.class) instanceof TestFeature2NewProvider, "Feature should not be replaced: "+comp.getFeature(ITestFeature2.class));
	}
	
	@Test
	public void	testAgentFeatureReplacement()
	{
		// Dummy component for feature loading.
		Component	comp	= new MicroAgent(null, null){};
		
		// IMjTestFeature1 feature should be replaced
		assertTrue(comp.getFeature(ITestFeature1.class) instanceof TestFeature1NewProvider, "Feature is not replaced: "+comp.getFeature(ITestFeature1.class));
		// IMjTestFeature2 feature should be replaced.
		assertTrue(comp.getFeature(ITestFeature2.class) instanceof TestFeature2NewProvider, "Feature should not be replaced: "+comp.getFeature(ITestFeature2.class));
	}
	
	@Test
	public void testComponentOrdering()
	{
		// Dummy component for feature loading.
		doTestOrdering(new Component(null), COMPONENT_FEATURE_TYPES);
	}
		
	@Test
	public void testAgentOrdering()
	{
		// Dummy agent component for feature loading.
		doTestOrdering(new MicroAgent(null, null){}, AGENT_FEATURE_TYPES);
	}
		
	protected void doTestOrdering(Component comp, Class<Object>[] feature_types)
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
