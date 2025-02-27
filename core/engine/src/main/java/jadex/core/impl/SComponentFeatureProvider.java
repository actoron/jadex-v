package jadex.core.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import jadex.core.IComponentFeature;

/**
 *  Static helper methods for dealing with features.
 */
public class SComponentFeatureProvider
{
	/** The available providers are cached at startup and do not change during runtime. */
	protected static final List<ComponentFeatureProvider<IComponentFeature>>	ALL_PROVIDERS;
	
	static
	{
		//System.out.println("init providers started");
		
		List<ComponentFeatureProvider<IComponentFeature>>	all	= new ArrayList<>();
		// Collect all feature providers
		ServiceLoader.load(ComponentFeatureProvider.class).forEach(provider ->
		{
			@SuppressWarnings("unchecked")
			ComponentFeatureProvider<IComponentFeature>	oprovider	= provider;
			all.add(oprovider);
		});
		
		// Classpath order undefined (differs betweenn gradle/eclipse
		// -> order feature providers alphabetically by fully qualified class name 
		all.sort((o1, o2) -> o1.getClass().getName().compareTo(o2.getClass().getName()));
		ALL_PROVIDERS = all;
		
		//all.forEach(System.out::println);
	}
			
	/** The providers by type are calculated on demand and cached for further use (comp_type -> map of (feature_type -> provider)). */ 
	protected static final Map<Class<? extends Component>, Map<Class<IComponentFeature>, ComponentFeatureProvider<IComponentFeature>>>	PROVIDERS_BY_TYPE	= Collections.synchronizedMap(new LinkedHashMap<>());
	
	/** The providers by type are calculated on demand and cached for further use (comp_type -> map of (feature_type -> provider)). */ 
	protected static final Map<Class<? extends Component>, List<ComponentFeatureProvider<IComponentFeature>>>	PROVIDERLIST_BY_TYPE	= Collections.synchronizedMap(new LinkedHashMap<>());
	
	/**
	 *  Helper method to get the providers, that are relevant for the given component type.
	 */
	public static List<ComponentFeatureProvider<IComponentFeature>>	getProviderListForComponent(Class<? extends Component> type)
	{
		List<ComponentFeatureProvider<IComponentFeature>>	ret	= PROVIDERLIST_BY_TYPE.get(type);
		if(ret==null)
		{
			getProvidersForComponent(type);
			ret	= PROVIDERLIST_BY_TYPE.get(type);
		}
		return ret;
	}
	
	/**
	 *  Helper method to get the providers, that are relevant for the given component type.
	 */
	public static Map<Class<IComponentFeature>, ComponentFeatureProvider<IComponentFeature>>	getProvidersForComponent(Class<? extends Component> type)
	{
		Map<Class<IComponentFeature>, ComponentFeatureProvider<IComponentFeature>>	ret	= PROVIDERS_BY_TYPE.get(type);
		if(ret==null)
		{
			ret	= new LinkedHashMap<>();
			
			// Collect feature providers for this component type
			// Replaces early providers with later providers for the same feature (e.g. execfeature <- simexecfeature)
			for(ComponentFeatureProvider<IComponentFeature> provider: ALL_PROVIDERS)
			{
				// Ignores providers for incompatible agent types (e.g. no micro features in gpmn agent)
				if(provider.getRequiredComponentType().isAssignableFrom(type))
				{
					// Check for conflict (two providers for same feature type)
					if(ret.containsKey(provider.getFeatureType()))
					{
						if(provider.replacesFeatureProvider(ret.get(provider.getFeatureType())))
						{
							// Both want to replace each other -> fail
							if(ret.get(provider.getFeatureType()).replacesFeatureProvider(provider))
							{
								throw new IllegalStateException("Cyclic replacement of providers for same feature type: "
									+provider.getFeatureType().getName()+", "+ret.get(provider.getFeatureType())+", "+provider);
							}
							// new provider wants to replace existing -> replace
							else
							{
								ret.put(provider.getFeatureType(), provider);
							}
						}
						// existing provider wants to replace new -> nop
						//else if(ret.get(provider.getFeatureType()).replacesFeatureProvider(provider))
						
						// no provider wants to replace the other -> fail
						else if(!ret.get(provider.getFeatureType()).replacesFeatureProvider(provider))
						{
							throw new IllegalStateException("Two providers for same feature type: "
								+provider.getFeatureType().getName()+", "+ret.get(provider.getFeatureType())+", "+provider);
						}
					}
					else
					{
						ret.put(provider.getFeatureType(), provider);
					}
				}
			}
			
			ret = orderComponentFeatures(ret.values()).stream().collect(
			Collectors.toMap(
				ComponentFeatureProvider::getFeatureType, 
				element -> element, 
				(existing, replacement) -> existing, 
				LinkedHashMap::new)
			);
			
			PROVIDERS_BY_TYPE.put(type, ret);
			PROVIDERLIST_BY_TYPE.put(type, new ArrayList<>(ret.values()));
		}
		return ret;
	}
	
	/** The available providers are cached at startup and do not change during runtime. */
	protected static List<IComponentLifecycleManager> LIFECYCLE_PROVIDERS;
	
	public static synchronized List<IComponentLifecycleManager> getLifecycleProviders()
	{
		if(LIFECYCLE_PROVIDERS==null)
		{
			LIFECYCLE_PROVIDERS = ALL_PROVIDERS.stream()
				.filter(provider -> provider instanceof IComponentLifecycleManager)
				.map(provider -> (IComponentLifecycleManager)provider)
				.collect(Collectors.toList());
		}
		return LIFECYCLE_PROVIDERS;
	}
	
	/**
	 *  Build an ordered list of component features.
	 *  @param provs A list of component feature lists.
	 *  @return An ordered list of component features.
	 */
	public static Collection<ComponentFeatureProvider<IComponentFeature>> orderComponentFeatures(Collection<ComponentFeatureProvider<IComponentFeature>> provs)
	{
		DependencyResolver<ComponentFeatureProvider<IComponentFeature>> dr = new DependencyResolver<ComponentFeatureProvider<IComponentFeature>>();

		// visualize feature dependencies for debugging
//		Class<?> cl = SReflect.classForName0("jadex.tools.featuredeps.DepViewerPanel", null);
//		if(cl!=null)
//		{
//			try
//			{
//				Method m = cl.getMethod("createFrame", new Class[]{String.class, DependencyResolver.class});
//				m.invoke(null, new Object[]{name, dr});
//			}
//			catch(Exception e)
//			{
//				e.printStackTrace();
//			}
//		}
		
		Map<Class<?>, ComponentFeatureProvider<IComponentFeature>> provsmap = new HashMap<>();
		for(ComponentFeatureProvider<IComponentFeature> prov: provs)
		{
			provsmap.put(prov.getFeatureType(), prov);
		}

		Map<Class<?>, ComponentFeatureProvider<IComponentFeature>> odeps = new HashMap<>();
		
		for(ComponentFeatureProvider<IComponentFeature> prov: provs)
		{
//			IComponentFeatureFactory last = null;
			// Only use the last feature of a given type (allows overriding features)
			if(provsmap.get(prov.getFeatureType())==prov)
			{
				dr.addNode(prov);
				
				// If overridden old position is used as dependency!
				if(odeps.containsKey(prov.getFeatureType()))
				{
					ComponentFeatureProvider<IComponentFeature> odep = odeps.get(prov.getFeatureType());
					if(odep!=null)
					{
						dr.addDependency(prov, odep);
					}
				}
				
				// else order in current list
//				else 
//				{
//					if(last!=null)
//						dr.addDependency(fac, last);
//					last = fac;
//				}
				
				/*Set<Class<?>> sucs = fac.getSuccessors();
				for(Class<?> suc: sucs)
				{
					if(facsmap.get(fac.getType())!=null && facsmap.get(suc)!=null)
					{
						dr.addDependency(facsmap.get(suc), facsmap.get(fac.getType()));
					}
//					else
//					{
//						System.out.println("Declared dependency not found, ignoring: "+suc+" "+prov.getFeatureType());
//					}
				}*/
				
				Set<Class<?>> pres = prov.getPredecessors(new HashSet<Class<?>>(provsmap.keySet()));
				for(Class<?> pre: pres)
				{
					if(provsmap.get(pre)!=null && provsmap.get(prov.getFeatureType())!=null)
					{
						dr.addDependency(provsmap.get(prov.getFeatureType()), provsmap.get(pre));
					}
					else
					{
						System.out.println("Declared dependency not found, ignoring: "+pre+" "+prov.getFeatureType());
					}
				}
			}
			// Save original dependency of the feature
//			else if(!odeps.containsKey(fac.getType()))
//			{
//				odeps.put(fac.getType(), last);
//			}
		}

		Collection<ComponentFeatureProvider<IComponentFeature>> ret = dr.resolveDependencies(true);
		//System.out.println("ordered features: "+ret);
		return ret;
	}
}
