package jadex.mj.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jadex.common.IValueFetcher;
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
	
	/** The fetcher. */
	protected IValueFetcher fetcher;
	
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
	public <T> T getExistingFeature(Class<T> type)
	{
		//return getFeatures().stream().findFirst(feature -> feature instanceof IMjLifecycle);
		return getFeatures().stream()
	        .filter(feature -> type.isInstance(feature))
	        .map(type::cast)  
	        .findFirst()
	        .orElse(null); 
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
	
	/**
	 *  Get the fetcher.
	 *  @return The fetcher.
	 */
	public IValueFetcher getFetcher()
	{
		if(fetcher==null)
		{
			// Return a fetcher that tries features in reverse order first.
			return new IValueFetcher()
			{
				public Object fetchValue(String name)
				{
					Object	ret	= null;
					boolean	found	= false;
					
					Object[] lfeatures = getFeatures().toArray();
					for(int i=lfeatures.length-1; !found && i>=0; i--)
					{
						if(lfeatures[i] instanceof IValueFetcherProvider)
						{
							IValueFetcher	vf	= ((IValueFetcherProvider)lfeatures[i]).getValueFetcher();
							if(vf!=null)
							{
								try
								{
									// Todo: better (faster) way than throwing exceptions?
									ret	= vf.fetchValue(name);
									found	= true;
								}
								catch(Exception e)
								{
								}
							}
						}
					}
					
					/*if(!found && "$component".equals(name))
					{
						ret	= getInternalAccess();
						found	= true;
					}
					else if(!found && "$config".equals(name))
					{
						ret	= getConfiguration();
						found	= true;
					}*/
					
					if(!found)
						throw new RuntimeException("Value not found: "+name);
//					else
//						System.out.println("fetcher: "+name+" "+ret);
					
					return ret;
				}
			};
		}
		
		return fetcher;
	}
}
