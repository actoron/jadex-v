package jadex.mj.core.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import jadex.mj.core.MjComponent;

/**
 *  Static helper methods for dealing whith features.
 */
public class SMjFeatureProvider
{
	/** The available providers are cached at startup and do not change during runtime. */
	protected static final List<MjFeatureProvider<Object>>	ALL_PROVIDERS;
	static
	{
		List<MjFeatureProvider<Object>>	all	= new ArrayList<>();
		// Collect all feature providers
		ServiceLoader.load(MjFeatureProvider.class).forEach(provider ->
		{
				@SuppressWarnings("unchecked")
				MjFeatureProvider<Object>	oprovider	= provider;
				all.add(oprovider);
		});
		ALL_PROVIDERS	= all;
	}
			
	/** The providers by type are calculated on demand and cached for further use. */ 
	protected static final Map<Class<? extends MjComponent>, Map<Class<Object>, MjFeatureProvider<Object>>>	PROVIDERS_BY_TYPE	= Collections.synchronizedMap(new LinkedHashMap<>());
	
	/**
	 *  Helper method to get the providers, that are relevant for the given component type.
	 */
	public static Map<Class<Object>, MjFeatureProvider<Object>>	getProvidersForComponent(Class<? extends MjComponent> type)
	{
		Map<Class<Object>, MjFeatureProvider<Object>>	ret	= PROVIDERS_BY_TYPE.get(type);
		if(ret==null)
		{
			ret	= new LinkedHashMap<>();
			// Collect feature providers for this component type
			// Replaces early providers with later providers for the same feature (e.g. execfeature <- simexecfeature)
			for(MjFeatureProvider<Object> provider: ALL_PROVIDERS)
			{
				// Ignores providers for incompatible agent types (e.g. no micro features in gpmn agent)
				if(provider.getRequiredComponentType().isAssignableFrom(type))
				{
					ret.put(provider.getFeatureType(), provider);
				}
			}
			PROVIDERS_BY_TYPE.put(type, ret);
		}
		return ret;
	}

	/** 
	 *  Helper method to convert (potentially) checked exceptions to unchecked ones.
	 */
	public static RuntimeException	throwUnchecked(Throwable t)
	{
		if(t instanceof InvocationTargetException)
		{
			throw throwUnchecked(((InvocationTargetException)t).getTargetException());
		}
		else if(t instanceof Error)
		{
			throw (Error)t;
		}
		else if(t instanceof RuntimeException)
		{
			throw (RuntimeException)t;
		}
		else
		{
			throw new RuntimeException(t);
		}
	}
}
