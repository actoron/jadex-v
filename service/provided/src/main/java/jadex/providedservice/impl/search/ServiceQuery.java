package jadex.providedservice.impl.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.ClassInfo;
import jadex.common.SUtil;
import jadex.common.Tuple3;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.ServiceScope;

/**
 *  Service query definition. T is the return type for search methods.
 */
public class ServiceQuery<T>
{	
	//-------- constants --------
	
	/** Marker for networks not set. */
	//Hack!!! should not be public??? 
	public static final String[] GROUPS_NOT_SET = new String[]{"__GROUPS_NOT_SET__"};	// TODO: new String[0] for better performance, but unable to check remotely after marshalling!
	
	/** Default matching modes set the elements with OR semantics. */
	public static final Map<String, Boolean> DEFAULT_MATCHINGMODES = SUtil.createHashMap(
		new String[]{ServiceKeyExtractor.KEY_TYPE_TAGS, ServiceKeyExtractor.KEY_TYPE_GROUPS}, 
		new Boolean[]{Boolean.FALSE, Boolean.FALSE});
	
	//-------- attributes --------
	
	/** The service type. */
	protected ClassInfo servicetype;
	
	/** Tags of the service. */
	protected String[] servicetags;
	
	/** The service provider. (rename serviceowner?) */
	//protected IComponentIdentifier provider;
	
	/** Starting point for the search scoping. */
	//protected IComponentIdentifier searchstart;
	
	///** The service platform. (Find a service from another known platform, e.g. cms) */
	//protected IComponentIdentifier platform;
	
	/** The service ID of the target service. Fast lookup of a service by id. */
	protected IServiceIdentifier serviceidentifier;
	
	/** The network names. */
	protected String[] groupnames;
	
	/** Should the service be unrestricted. */
	protected Boolean unrestricted;
	
	/** The search scope. */
	protected ServiceScope scope;
	
	/** The query owner. (rename queryowner?) */
	protected ComponentIdentifier owner;
	
	//-------- influence the result --------
	
	/** Flag, if service by the query owner should be excluded, i.e. do not return my own service. */
	protected boolean excludeowner;
	
	/** The multiple flag. Search for multiple services */
	protected Multiplicity	multiplicity;
	
	/** Flag if event mode is enabled on the query. */
	protected boolean eventmode;
	
	/** The matching mode for multivalued terms. True is and and false is or. */
	protected Map<String, Boolean> matchingmodes;
	
	//-------- identification of a query --------
	
	/** The query id. Id is used for hashcode and equals and the same is used by servicequeryinfo class.
	    Allows for hashing queryies and use a queryinfo object for lookup. */
	protected String id;
	
	/**
	 *  Create a new service query.
	 */
	protected ServiceQuery()
	{
		// Not public to not encourage user to use it.
		// Here it does NOT set the networknames automatically because used for serialization.
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(Class<T> servicetype)
	{
		this(servicetype, null);
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(Class<T> servicetype, ServiceScope scope)
	{
		this(servicetype == null ?(ClassInfo) null : new ClassInfo(servicetype), scope, null);
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(Class<T> servicetype, ServiceScope scope, ComponentIdentifier owner)
	{
		this(servicetype == null ? (ClassInfo) null : new ClassInfo(servicetype), scope, owner);
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(ClassInfo servicetype)
	{
		this(servicetype, null, null);
	}
	
	/**
	 *  Create a new service query.
	 */
	public ServiceQuery(ClassInfo servicetype, ServiceScope scope, ComponentIdentifier owner)
	{
//		if(owner==null)
//			throw new IllegalArgumentException("Owner must not null");
		
		this.servicetype = servicetype;
		this.owner = owner;
		
		this.id = SUtil.createUniqueId();
		this.groupnames = GROUPS_NOT_SET;
		
		setScope(scope);
	}
	
	/**
	 *  Shallow copy constructor.
	 *  @param original Original query.
	 */
	public ServiceQuery(ServiceQuery<T> original)
	{
		this.servicetype = original.servicetype;
		this.scope = original.scope;
		this.servicetags = original.servicetags;
		this.owner = original.owner;
		this.id = original.id;
		this.groupnames = original.groupnames;
		this.matchingmodes = original.matchingmodes;
		//this.platform	= original.platform;
		//this.searchstart	= original.searchstart;
		this.unrestricted = original.unrestricted;
	}

	/**
	 *  Get the service type.
	 *  @return The service type
	 */
	public ClassInfo getServiceType()
	{
		return servicetype;
	}

	/**
	 *  Set the service type.
	 *  @param type The service type to set
	 */
	public ServiceQuery<T> setServiceType(ClassInfo servicetype)
	{
		this.servicetype = servicetype;
		return this;
	}
	
	/**
	 *  Changes the query to event mode.
	 *  
	 *  @param eventmode the event mode state.
	 *  @deprecated For bean purposes only, use setEventMode().
	 */
	@Deprecated
	public void setEventMode(boolean eventmode)
	{
		this.eventmode = eventmode;
	}
	
	/**
	 *  Changes the query to event mode.
	 *  
	 *  @return The new query.
	 */
	@SuppressWarnings("unchecked")
	public ServiceQuery<ServiceEvent> setEventMode()
	{
		this.eventmode = true;
		return (ServiceQuery<ServiceEvent>)this;
	}
	
	/**
	 *  Checks if query is in event mode.
	 *  @return True, if in event mode
	 */
	public boolean isEventMode()
	{
		return eventmode;
	}
	
	/**
	 *  Get the scope.
	 *  @return The scope
	 */
	public ServiceScope getScope()
	{
		return scope;
	}

	/**
	 *  Set the scope.
	 *  @param scope The scope to set
	 */
	public ServiceQuery<T> setScope(ServiceScope scope)
	{
		//if(ServiceScope.EXPRESSION.equals(scope))
		//	throw new IllegalArgumentException("Cannot use scope 'expression' directly.");
		//this.scope = scope;
		this.scope = scope!=null? scope: ServiceScope.VM;
		return this;
	}
	
	/**
	 *  Gets the service tags.
	 *  
	 *  @return The service tags. 
	 */
	public String[] getServiceTags()
	{
		return servicetags;
	}
	
	/**
	 *  Sets the service tags.
	 *  @param servicetags The service tags. 
	 * /
	public ServiceQuery<T> setServiceTags(String... servicetags)
	{
		//TagProperty.checkReservedTags(servicetags);
		
		this.servicetags = servicetags;
		return this;
	}*/
	
	/**
	 *  Sets the service tags.
	 *  @param servicetags The service tags.
	 *  
	 *  todo: move or refactor to hide complexity!?
	 */
	public ServiceQuery<T> setServiceTags(String[] servicetags, IComponent component)
	{
//		this.servicetags = ProvidedServiceFeature.evaluateTags(component,  Arrays.asList(servicetags)).toArray(new String[0]);
		this.servicetags = Arrays.asList(servicetags).toArray(new String[0]);
		return this;
	}

	/**
	 *  Set the provider.
	 *  @param provider The provider to set
	 */
	public ServiceQuery<T> setProvider(ComponentIdentifier provider)
	{
		//this.searchstart = provider;
		//this.scope = ServiceScope.COMPONENT;
		return this;
	}
	
	/**
	 *  Get the provider.
	 *  @return The provider
	 * /
	public IComponentIdentifier getSearchStart()
	{
		return searchstart;
	}*/

	/**
	 *  Set the provider.
	 *  @param provider The provider to set
	 * / 
	public ServiceQuery<T> setSearchStart(IComponentIdentifier searchstart)
	{
		this.searchstart = searchstart;
		return this;
	}*/
	
	/**
	 *  Get the platform.
	 *  @return The platform
	 * /
	public IComponentIdentifier getPlatform()
	{
		return platform;
	}*/
	
	/**
	 *  Set the platform.
	 *  @param platform The platform
	 * /
	public ServiceQuery<T> setPlatform(IComponentIdentifier platform)
	{
		this.platform = platform;
		return this;
	}*/
	
	/**
	 *  Gets the service identifier.
	 *
	 *  @return The service identifier.
	 */
	public IServiceIdentifier getServiceIdentifier()
	{
		return serviceidentifier;
	}

	/**
	 *  Sets the service identifier.
	 *  Also sets the corresponding provider when sid!=null.
	 *
	 *  @param serviceidentifier The service identifier.
	 */
	// TODO: looking up sid shouldn't be search/query?
	public ServiceQuery<T> setServiceIdentifier(IServiceIdentifier serviceidentifier)
	{
		this.serviceidentifier = serviceidentifier;
		// When setting sid also set provider.
		if(serviceidentifier!=null)
			setProvider(serviceidentifier.getProviderId());
		return this;
	}

	/**
	 *  Get the owner.
	 *  @return The owner
	 */
	public ComponentIdentifier getOwner()
	{
		return owner;
	}

	/**
	 *  Set the owner.
	 *  @param owner The owner to set
	 */
	public ServiceQuery<T> setOwner(ComponentIdentifier owner)
	{
		this.owner = owner;
		return this;
	}
	
	/**
	 *  Checks if service of the query owner should be excluded.
	 *  
	 *  @return True, if the services should be excluded.
	 */
	public boolean isExcludeOwner()
	{
		return excludeowner;
	}
	
	/**
	 *  Sets if service of the query owner should be excluded.
	 *  
	 *  @param excludeowner True, if the services should be excluded.
	 */
	public ServiceQuery<T> setExcludeOwner(boolean excludeowner)
	{
		this.excludeowner = excludeowner;
		return this;
	}
	
	/**
	 *  Get the id.
	 *  @return the id
	 */
	public String getId()
	{
		return id;
	}

	/**
	 *  Set the id.
	 *  @param id The id to set
	 */
	public ServiceQuery<T> setId(String id)
	{
		this.id = id;
		return this;
	}
	
	/**
	 *  Get the multiplicity.
	 *  @return the multiplicity
	 */
	public Multiplicity	getMultiplicity()
	{
		return multiplicity;
	}
	
	/**
	 *  Set the multiplicity.
	 *  @param multiplicity The minimum multiplicity to set
	 */
	public ServiceQuery<T> setMultiplicity(int multiplicity)
	{
		return setMultiplicity(multiplicity, -1);
	}
	
	/**
	 *  Set the multiplicity.
	 *  @param multiplicitystart The minimum multiplicity to set
	 *  @param multiplicityend The max multiplicity to set
	 */
	public ServiceQuery<T> setMultiplicity(int multiplicitystart, int multiplicityend)
	{
		return setMultiplicity(new Multiplicity(multiplicitystart, multiplicityend));
	}

	/**
	 *  Set the multiplicity.
	 *  @param multiplicity The multiplicity to set
	 */
	public ServiceQuery<T> setMultiplicity(Multiplicity multiplicity)
	{
		this.multiplicity = multiplicity;
		return this;
	}

	/**
	 *  Gets the specification for the indexer.
	 *  Query needs to be enhanced before calling this method. See RequiredServiceFeature.enhanceQuery()
	 *  
	 *  @return The specification for the indexer.
	 */
	public List<Tuple3<String, String[], Boolean>> getIndexerSearchSpec()
	{
		List<Tuple3<String, String[], Boolean>> ret = new ArrayList<Tuple3<String,String[],Boolean>>();
		
		// Problem with normal vs resticted vs. unrestricted queries
		// normal: - deliver all services (restr. and unrestr.). 
		//         - unrestricted services do not need networks that fit to those from query (they can always be accessed). Therefore key extractor return MATCH_ALWAYS. important for normal queries
		// restricted and unrestricted: use index to find only those. in case of unrestricted query the networks of the query will be omitted (they might have been automatically set)
		
		// normal is both, i.e. unrestricted = null
		if(unrestricted != null)
			ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_UNRESTRICTED, new String[]{unrestricted.toString()}, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_UNRESTRICTED)));
		
		//if(platform != null)
		//	ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_PLATFORM, new String[]{platform.toString()}, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_PLATFORM)));
		
		/*if(ServiceScope.COMPONENT_ONLY.equals(scope))
		{
			if(searchstart != null)
				ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_PROVIDER, new String[]{searchstart.toString()}, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_PROVIDER)));
			else
				ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_PROVIDER, new String[]{owner.toString()}, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_PROVIDER)));
		}*/
		
		if(servicetype != null)
			ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_INTERFACE, new String[]{servicetype.getGenericTypeName()}, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_INTERFACE)));
		
		if(servicetags != null && servicetags.length > 0)
			ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_TAGS, servicetags, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_TAGS)));
		
		if(serviceidentifier != null)
			ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_SID, new String[]{serviceidentifier.toString()}, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_SID)));
		
		assert !Arrays.equals(groupnames, GROUPS_NOT_SET) : "Problem: query not enhanced before processing.";
	
		if((unrestricted==null || Boolean.FALSE.equals(unrestricted)) && groupnames != null && groupnames.length > 0)
			ret.add(new Tuple3<String, String[], Boolean>(ServiceKeyExtractor.KEY_TYPE_GROUPS, groupnames, getMatchingMode(ServiceKeyExtractor.KEY_TYPE_GROUPS)));
		
		return ret;
	}
	
	/**
	 *  Get the matching mode for a key.
	 *  @param key The key name.
	 *  @return True for and, false for or.
	 */
	public Boolean getMatchingMode(String key)
	{
		return matchingmodes==null? DEFAULT_MATCHINGMODES.get(key): matchingmodes.get(key);
	}
	
	/**
	 *  Set a matching mode.
	 *  @param key The key name.
	 *  @param and True for and.
	 */
	public ServiceQuery<T> setMatchingMode(String key, Boolean and)
	{
		if(matchingmodes==null)
			matchingmodes = new HashMap<String, Boolean>(DEFAULT_MATCHINGMODES);
		matchingmodes.put(key, and);
		return this;
	}
	
	
	/**
	 *  Get the unrestricted mode.
	 *  @return The unrestricted mode.
	 */
	public Boolean isUnrestricted()
	{
		return unrestricted;
	}

	/**
	 *  Set the unrestricted mode.
	 *  @param unrestricted the unrestricted to set
	 */
	public ServiceQuery<T> setUnrestricted(Boolean unrestricted)
	{
		this.unrestricted = unrestricted;
		return this;
	}

	/**
	 *  Tests if the query keys matches a service.
	 *  
	 *  @param service The service.
	 *  @return True, if the service matches the keys.
	 */
	protected boolean matchesKeys(IServiceIdentifier service)
	{
		if (servicetype != null && !service.getServiceType().getGenericTypeName().equals(servicetype))
			return false;
		
		if (servicetags != null)
		{
			Set<String> tagsset = ServiceKeyExtractor.getKeysStatic(ServiceKeyExtractor.KEY_TYPE_TAGS, service);
			if (tagsset == null)
				return false;
			
			for (String tag : servicetags)
			{
				if (!tagsset.contains(tag))
				{
					return false;
				}
			}
		}
		
		/*if (ServiceScope.COMPONENT_ONLY.equals(scope) &&
			!((searchstart != null && service.getProviderId().equals(searchstart)) ||
			service.getProviderId().equals(owner)))
			return false;*/
		
//		if (provider != null && !provider.equals(service.getProviderId()))
//			return false;
		
		/*if(platform != null && !platform.equals(service.getProviderId().getRoot()))
			return false;*/
		
		return true;
	}
	
//	/**
//	 *  Get the prepared.
//	 *  @return the prepared
//	 */
//	public boolean isPrepared()
//	{
//		return prepared;
//	}
//
//	/**
//	 *  Set the prepared.
//	 *  @param prepared The prepared to set
//	 */
//	public void setPrepared(boolean prepared)
//	{
//		this.prepared = prepared;
//	}
	
//	/**
//	 *  Prepare the query.
//	 */
//	public void prepare(IComponentIdentifier cid)
//	{
//		if(!prepared)
//		{
//			networknames = getNetworkNames(cid);
//			prepared = true;
//		}
//	}
	
	
	/**
	 *  Get the group names.
	 *  @return the group names
	 */
	public String[] getGroupNames()
	{
		return groupnames;
	}

	/**
	 *  Set the networknames.
	 *  @param networknames The networknames to set
	 */
	public ServiceQuery<T> setGroupNames(String... groupnames)
	{
		this.groupnames = groupnames;
		return this;
	}
	
	/**
	 *  Get the hashcode.
	 */
	public int hashCode()
	{
		return id.hashCode()*13;
	}

	/**
	 *  Test if other object equals this one.
	 */
	public boolean equals(Object obj)
	{
		boolean ret = false;
		if(obj instanceof ServiceQuery)
		{
			ServiceQuery<?> other = (ServiceQuery<?>)obj;
			ret = SUtil.equals(getId(), other.getId());
		}
		return ret;
	}
	
	/**
	 *  Get the target platform if specified (using platform and provider).
	 *  @return The target platform.
	 * /
	public IComponentIdentifier getTargetPlatform()
	{
		if (getPlatform()!=null)
			return getPlatform().getRoot();
		
		if (ServiceScope.COMPONENT_ONLY.equals(scope))
			return searchstart != null ? searchstart.getRoot() : owner.getRoot();
			
		return null;
	}*/
	
	

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		StringBuffer	ret	= new StringBuffer("ServiceQuery(");
		if(servicetype!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("servicetype=");
			ret.append(servicetype);
		}
		
		if(serviceidentifier!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("serviceidentifier=");
			ret.append(serviceidentifier);
		}
		
		if(multiplicity!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("multiplicity=");
			ret.append(multiplicity);
		}
		
		if(servicetags!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("servicetags=");
			ret.append(Arrays.toString(servicetags));
		}
		/*if(searchstart!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("searchstart=");
			ret.append(searchstart);
		}
		if(platform!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("platform=");
			ret.append(platform);
		}*/
		if(groupnames!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("groupnames=");
			ret.append(Arrays.toString(groupnames));
		}

		if(unrestricted!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("unrestricted=");
			ret.append(unrestricted);
		}

		if(scope!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("scope=");
			ret.append(scope);
		}

		if(owner!=null)
		{
			ret.append(ret.length()==13?"":", ");
			ret.append("owner=");
			ret.append(owner);
		}
			
		ret.append(")");
		return ret.toString();
	}
	
	//-------- query multiplicity --------
	
	/**
	 *  Define cases for multiplicity.
	 */
	public static class	Multiplicity
	{
		//-------- constants --------
		
		/** Constant for multiplicity many. */
		public static final int MANY = -1;
		
		/** Constant for multiplicity undefined. */
		public static final int UNDEFINED = -2;
		
		/** '0..1' multiplicity for single optional service. */
		public static Multiplicity	ZERO_ONE	= new Multiplicity(0, 1);
		
		/** '1' multiplicity for required service (default for searchService methods). */
		public static Multiplicity	ONE			= new Multiplicity(1, 1);
		
		/** '0..*' multiplicity for optional multi service (default for searchServices methods). */
		public static Multiplicity	ZERO_MANY	= new Multiplicity(0, -1);

		/** '1..*' multiplicity for required service (default for searchService methods). */
		public static Multiplicity	ONE_MANY	= new Multiplicity(1, -1);
		
		//-------- attributes --------
		
		/** The minimal number of services required. Otherwise search ends with ServiceNotFoundException. */
		private int	from;
		
		/** The maximal number of services returned. Afterwards search/query will terminate. */
		private int to;
		
		//-------- constructors --------

		/**
		 *  Bean constructor.
		 *  Not meant for direct use.
		 *  Defaults to invalid multiplicity ('0..0')!
		 */
		public Multiplicity()
		{
			this.from = -2; // = UNDEFINED
			this.to = -2;
		}
		
		/**
		 *  Create a multiplicity.
		 *  @param from The minimal number of services for the search/query being considered successful (positive integer or 0).
		 *  @param to The maximal number of services returned by the search/query (positive integer or -1 for unlimited).
		 */
		public Multiplicity(int from, int to)
		{
			setFrom(from);
			setTo(to);
		}
		
		//-------- methods --------
		
		/**
		 *  Get the 'from' value, i.e. the minimal number of services required.
		 *  Otherwise search ends with ServiceNotFoundException. 
		 */
		public int getFrom()
		{
			return from;
		}
		
		/**
		 *  Set the 'from' value, i.e. the minimal number of services required.
		 *  Otherwise search ends with ServiceNotFoundException. 
		 *  @param from Positive integer or 0
		 */
		public void setFrom(int from)
		{
			if(from<0)
				throw new IllegalArgumentException("'from' must be a positive value or 0.");
				
			this.from = from;
		}
		
		/**
		 *  Get the 'to' value, i.e. The maximal number of services returned.
		 *  Afterwards search/query will terminate.
		 */
		public int getTo()
		{
			return to;
		}
		
		/**
		 *  Get the 'to' value, i.e. The maximal number of services returned.
		 *  Afterwards search/query will terminate.
		 *  @param to	Positive integer or -1 for unlimited.
		 */
		public void setTo(int to)
		{
			if(to!=-1 && to<1)
				throw new IllegalArgumentException("'to' must be a positive value or -1.");
			
			this.to = to;
		}
		
		/**
		 *  Get a string representation of the multiplicity.		
		 */
		@Override
		public String toString()
		{
			return from==to ? Integer.toString(from) : from + ".." + (to==-1 ? "*" : Integer.toString(to));
		}
	}
}
