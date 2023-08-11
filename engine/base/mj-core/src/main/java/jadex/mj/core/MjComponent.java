package jadex.mj.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jadex.mj.core.impl.MjFeatureProvider;
import jadex.mj.core.impl.SMjFeatureProvider;

/**
 *  Base class for Jadex components, which provides access to component features.
 */
public class MjComponent
{

	/** The providers for this component type, stored by the feature type they provide.
	 *  Is also used at runtime to instantiate lazy features.*/
	protected Map<Class<Object>, MjFeatureProvider<Object>>	providers;
	
	/** The feature instances of this component, stored by the feature type. */
	protected Map<Class<Object>, Object>	features	= new LinkedHashMap<>();
	
	/**
	 *  Create a new component and instantiate all features (except lazy features).
	 */
	protected MjComponent()
	{
		// Fetch relevant providers (potentially cached)
		providers	= SMjFeatureProvider.getProvidersForComponent(getClass());
		// Instantiate all features (except lazy ones).
		providers.values().forEach(provider ->
		{
			if(!provider.isLazyFeature())
			{
				Object	feature	= provider.createFeatureInstance(this);
				features.put(provider.getFeatureType(), feature);
			}
		});
	}
	
	/**
	 *  Get a copy of the set of currently instantiated features.
	 *  Does not include lazy, which have not yet been accessed.  
	 */
	public Set<Object>	getFeatures()
	{
		return new LinkedHashSet<>(features.values());
	}
	
	/**
	 *  Get the feature instance for the given type.
	 *  Instantiates lazy features if needed.
	 */
	public <T> T getFeature(Class<T> type)
	{
		if(features.containsKey(type))
		{
			@SuppressWarnings("unchecked")
			T	ret	= (T)features.get(type);
			return ret;
		}
		else if(providers.containsKey(type))
		{
			try
			{
				MjFeatureProvider<?>	provider	= providers.get(type);
				assert provider.isLazyFeature();
				@SuppressWarnings("unchecked")
				T	ret	= (T)provider.createFeatureInstance(this);
				@SuppressWarnings("unchecked")
				Class<Object> otype	= (Class<Object>)type;
				features.put(otype, ret);
				return ret;
			}
			catch(Throwable t)
			{
				throw SMjFeatureProvider.throwUnchecked(t);
			}
		}
		else
		{
			throw new RuntimeException("No such feature: "+type);
		}
	}
}
