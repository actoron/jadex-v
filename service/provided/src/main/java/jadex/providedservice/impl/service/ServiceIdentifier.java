package jadex.providedservice.impl.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadex.common.ClassInfo;
import jadex.common.SReflect;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.providedservice.IServiceIdentifier;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Security;
import jadex.providedservice.annotation.Service;


/**
 *  Service identifier for uniquely identifying a service.
 *  Is composed of the container id and the service name.
 */
public class ServiceIdentifier implements IServiceIdentifier
{
	//-------- attributes --------
	
	/** The provider identifier. */
	protected ComponentIdentifier providerid;
		
	/** The service name. */
	protected String servicename;
	
	/** The service type. */
	protected ClassInfo type;
	
	/** The service super types. */
	protected ClassInfo[] supertypes;

	/** The scope. */
	protected ServiceScope scope;
	
	/** The group names (shared object with security service). */
	protected Set<String> groupnames;
	
	/** Is the service unrestricted. */
	protected boolean unrestricted;
	
	/** The string representation (cached for reducing memory consumption). */
	protected String tostring;
	
	/** The tags. */
	protected Collection<String> tags;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service identifier.
	 */
	public ServiceIdentifier()
	{
	}
	
	/**
	 *  Create a new service identifier.
	 */
	private ServiceIdentifier(IComponent provider, Class<?> type, String servicename, ServiceScope scope, Set<String> networknames, Boolean unrestricted, Collection<String> tags)
	{
		this(provider.getId(), new ClassInfo(type), getSuperClasses(type), servicename,
			scope, networknames, unrestricted, tags);
		
		/*this.providerid = provider.getId();
		this.type	= new ClassInfo(type);
		this.supertypes = superinfos.toArray(new ClassInfo[superinfos.size()]);
		this.servicename = servicename;
		//this.rid = rid;
		//@SuppressWarnings({"unchecked"})
		//Set<String>	networknames = (Set<String>)Starter.getPlatformValue(providerid, Starter.DATA_NETWORKNAMESCACHE);
		//this.networknames	= networknames;
		
		//this.unrestricted = unrestricted!=null ? unrestricted : isUnrestricted(provider, type);
		
		setScope(scope);*/
	}
	
	
	
	/**
	 *  Create a new service identifier.
	 */
	private ServiceIdentifier(ComponentIdentifier providerid, ClassInfo type, ClassInfo[] supertypes, String servicename, 
		ServiceScope scope, Set<String> groupnames, boolean unrestricted, Collection<String> tags)
	{
		this.providerid = providerid;
		this.type	= type;
		this.supertypes = supertypes;
		this.servicename = servicename;
		this.groupnames = groupnames;
		this.unrestricted = unrestricted;
		this.tags = tags;
		setScope(scope);
	}
	
	public static ClassInfo[] getSuperClasses(Class<?> type)
	{
		List<ClassInfo> superinfos = new ArrayList<ClassInfo>();
		for(Class<?> sin: SReflect.getSuperInterfaces(new Class[]{type}))
		{
			if(sin.isAnnotationPresent(Service.class))
				superinfos.add(new ClassInfo(sin));
		}
		return superinfos.toArray(new ClassInfo[superinfos.size()]);
	}
	
	/**
	 *  Create a new service identifier for the own component.
	 */
	public static IServiceIdentifier createServiceIdentifier(IComponent provider, String servicename, 
		Class<?> servicetype, ServiceScope scope, Collection<String> tags)
	{
//		if(servicetype.getName().indexOf("IServicePool")!=-1)
//			System.out.println("sdjhvkl");
		Security security = getSecurityLevel(provider, null, null);//info, serviceimpl, servicetype, null, null);
		Set<String>	roles = ServiceIdentifier.getRoles(security, provider);
		//ServiceScope scope = info!=null ? info.getScope() : null;
		
		return new ServiceIdentifier(provider, servicetype, servicename!=null? servicename: generateServiceName(servicetype), scope, roles,
			roles!=null && roles.contains(Security.UNRESTRICTED), tags);
	}
	
	/**
	 *  Create a new service identifier for a potentially remote component.
	 */
	public static ServiceIdentifier	createServiceIdentifier(ComponentIdentifier providerid, Class<?> type, ClassInfo[] supertypes, 
		String servicename, ServiceScope scope, Set<String> networknames, boolean unrestricted, Collection<String> tags)
	{
		return new ServiceIdentifier(providerid, new ClassInfo(type), supertypes, servicename, scope, networknames, unrestricted, tags);
	}
	
	/** The id counter. */
	protected static long idcnt;
	
	/**
	 *  Generate a unique name.
	 *  @param The calling service class.
	 */
	public static String generateServiceName(Class<?> service)
	{
		synchronized(ServiceIdentifier.class)
		{
			return SReflect.getInnerClassName(service)+"_#"+idcnt++;
		}
	}
	
	//-------- methods --------
	
	/**
	 *  Get the service provider identifier.
	 *  @return The provider id.
	 */
	public ComponentIdentifier getProviderId()
	{
		return providerid;
	}
	
	/**
	 *  Set the providerid.
	 *  @param providerid The providerid to set.
	 */
	public void setProviderId(ComponentIdentifier providerid)
	{
		this.providerid = providerid;
	}
	
	/**
	 *  Get the service type.
	 *  @return The service type.
	 */
	public ClassInfo getServiceType()
	{
		return type;
	}

	/**
	 *  Set the service type.
	 *  @param type The service type.
	 */
	public void	setServiceType(ClassInfo type)
	{
		this.type	= type;
	}
	
	/**
	 *  Get the service super types.
	 *  @return The service super types.
	 */
	public ClassInfo[] getServiceSuperTypes()
	{
		return supertypes;
	}

	/**
	 *  Set the service super types.
	 *  @param type The service super types.
	 */
	public void	setServiceSuperTypes(ClassInfo[] supertypes)
	{
		this.supertypes	= supertypes;
	}

	/**
	 *  Get the service name.
	 *  @return The service name.
	 */
	public String getServiceName()
	{
		return servicename;
	}
	
	/**
	 *  Set the servicename.
	 *  @param servicename The servicename to set.
	 */
	public void setServiceName(String servicename)
	{
		this.servicename = servicename;
	}

	/**
	 *  Get the scope.
	 *  @return The scope.
	 */
	public ServiceScope getScope()
	{
//		return scope==null? ServiceScope.GLOBAL: scope;
		return scope;
	}

	/**
	 *  Set the scope.
	 *  @param scope The scope to set.
	 */
	public void setScope(ServiceScope scope)
	{
		//if(ServiceScope.EXPRESSION.equals(scope))
		//	throw new IllegalArgumentException("Cannot use scope 'expression' directly.");
		
//		if(ServiceScope.DEFAULT.equals(scope))
//			System.out.println("setting def");
		
		// default publication scope is platform
		// Replace DEFAULT with PLATFORM scope (do we want this here or during resolution?) 
		this.scope = scope!=null? //&& !ServiceScope.DEFAULT.equals(scope)? 
			scope : ServiceScope.VM;
	}
	
	/**
	 *  Get the network names.
	 *  @return the network names
	 */
	public Set<String> getGroupNames()
	{
		return groupnames;
	}

	/**
	 *  Set the network names.
	 *  @param networknames The network names to set
	 */
	public void setGroupNames(Set<String> groupnames)
	{
		this.groupnames = groupnames;
	}
	
	/**
	 *  Check if the service has unrestricted access. 
	 *  @return True, if it is unrestricted.
	 */
	public boolean isUnrestricted()
	{
		return unrestricted;
	}
	
	/**
	 *  Set the unrestricted flag.
	 *  @param unrestricted The unrestricted flag.
	 */
	public void setUnrestricted(boolean unrestricted) 
	{
		this.unrestricted = unrestricted;
	}
	
	/**
	 *  Get the service tags.
	 *  @return The tags.
	 */
	public Collection<String> getTags()
	{
		return tags;
	}
	
	/**
	 *  Set the tags.
	 *  @param tags the tags to set
	 */
	public void setTags(Set<String> tags)
	{
		this.tags = tags;
	}
	
//	/**
//	 *  Method to provide the security level.
//	 */
//	public static Security getSecurityLevel(Class<?> ctype)
//	{
//		return ctype!=null ? ctype.getAnnotation(Security.class) : null;
//	}
//	
//	/**
//	 *  Is the service unrestricted.
//	 *  @param access The access.
//	 *  @param ctype The service interface.
//	 *  @return True, if is unrestricted.
//	 */
//	public static boolean isUnrestricted(Component access, ClassInfo ctype)
//	{
//		Set<String>	roles	= getRoles(getSecurityLevel(ctype.getType(access.getClassLoader(), access.getFeature(IModelFeature.class).getModel().getAllImports())), access);
//		return roles!=null && roles.contains(Security.UNRESTRICTED);
//	}
//	
//	/**
//	 *  Is the service unrestricted.
//	 *  @param access The access.
//	 *  @param ctype The service interface.
//	 *  @return True, if is unrestricted.
//	 */
//	public static boolean isUnrestricted(Component access, Class<?> ctype)
//	{
//		Set<String>	roles	= getRoles(getSecurityLevel(ctype), access);
//		return roles!=null && roles.contains(Security.UNRESTRICTED);
//	}
//	
	/**
	 *  Get the roles from an annotation.
	 *  @param sec	The security annotation or null.
	 *  @param provider	The component that owns the service.
	 *  @return The roles, if any or null, if none given or sec==null.
	 */
	public static Set<String>	getRoles(Security sec, IComponent provider)
	{
		Set<String>	ret	= null;
		String[]	roles	= sec!=null ? sec.roles() : null;
		if(roles!=null && roles.length>0)
		{
			// Evaluate, if a role is given as expression.
			ret	= new HashSet<String>();
			for(String role: roles)
			{
				ret.add(role);
				// TODO: use injection to fetch roles on init.
//				ret.add((String)SJavaParser.evaluateExpressionPotentially(role, provider.getFeature(IModelFeature.class).getModel().getAllImports(), provider.getValueProvider().getFetcher(), provider.getClassLoader()));
			}
		}
		return ret;
	}
	
	/**
	 *  Find the most specific security setting.
	 */ 
	public static Security getSecurityLevel(IComponent access, Method method, IServiceIdentifier sid)
	{
		Security level = null;
		
//		// at runtime: have to refetch info from model
//		if(info==null && sid!=null)
//		{
//			ProvidedServiceInfo	found	= null;
//			ProvidedServiceModel model = (ProvidedServiceModel)((ModelInfo)access.getFeature(IModelFeature.class).getModel()).getFeatureModel(IProvidedServiceFeature.class);
//			ProvidedServiceInfo[] pros = model.getServices();
//			for(ProvidedServiceInfo psi: pros)
//			{
//				if(psi.getType().equals(sid.getServiceType()))
//				{
//					// Match when type and name are equal
//					if(sid.getServiceName().equals(psi.getName()))
//					{
//						found	= psi;
//						break;
//					}
//					
//					// Potential match when type is equal and no other service with same type
//					else if(found==null)
//					{
//						found	= psi;
//					}
//					
//					// Two services with same type -> fail if settings differ because we don't know which to use
//					else if(Arrays.equals(psi.getSecurity().roles(), found.getSecurity().roles()))
//					{
//						throw new RuntimeException("Use specific names for security settings on provided services with same type: "+psi.getType());
//					}
//				}
//			}
//			info	= found;
//		}
//		
//		// Instance level -> check for instance settings in provided service description			
//		if(info!=null && info.getSecurity()!=null && info.getSecurity().roles().length>0)
//		{
//			level	= info.getSecurity();
//		}
//		
//		// at runtime: fetch implclass from service
//		if(level==null && implclass==null && sid!=null)
//		{
//			Object impl = access.getFeature(IProvidedServiceFeature.class).getProvidedServiceRawImpl(sid);
//			implclass = impl!=null ? impl.getClass() : null;
//		}
//		
//		// For service call -> look for annotation in impl class hierarchy
//		// Precedence: hierarchy before specificity (e.g. class annotation in subclass wins over method annotation in superclass)
//		while(level==null && implclass!=null)
//		{
//			// Specificity: method before class
//			if(method!=null)
//			{
//				Method declmeth = SReflect.getDeclaredMethod0(implclass, method.getName(), method.getParameterTypes());
//				if(declmeth != null)
//				{
//					level = declmeth.getAnnotation(Security.class);
//				}
//			}
//			
//			if(level==null)
//			{
//				level	= implclass.getAnnotation(Security.class);
//			}
//			
//			implclass	= implclass.getSuperclass();
//		}
//			
//		// at runtime: fetch interface from sid
//		if(level==null && type==null && sid!=null)
//		{
//			type = sid.getServiceType().getType(access.getClassLoader());
//		}
//		
//		// For service call -> look for annotation in interface hierarchy
//		// Precedence: hierarchy before specificity (e.g. class annotation in subclass wins over method annotation in superclass)
//		if(level==null && type!=null)
//		{
//			List<Class<?>>	types = new LinkedList<Class<?>>();
//			types.add(type);
//			while(level==null && !types.isEmpty())
//			{
//				type	= types.remove(0);
//				
//				// Only consider interfaces that contain or inherit the method (if any)
//				if(method==null || SReflect.getMethod(type, method.getName(), method.getParameterTypes())!=null)
//				{
//					// Specificity: method before class
//					if(method!=null)
//					{
//						Method declmeth = SReflect.getDeclaredMethod0(type, method.getName(), method.getParameterTypes());
//						if(declmeth != null)
//						{
//							level = declmeth.getAnnotation(Security.class);
//						}
//					}
//					
//					if(level==null)
//					{
//						level	= type.getAnnotation(Security.class);
//					}
//					
//					// prepend -> depth first search
//					types.addAll(0, Arrays.asList(type.getInterfaces()));
//				}
//			}
//		}
//
//		// Default: e.g. remote invocation on non-service methods?
//		if(level==null && method!=null)
//		{
//			level = method.getAnnotation(Security.class);
//		}
//		
////			// Default to interface if not specified in impl.
////			if(level==null)
////			{
////				level = method.getAnnotation(Security.class);
////				Class<?> type = sid.getServiceType().getType(access.getClassLoader());
////				
////				if(level==null && type != null)
////				{
////					type = SReflect.getDeclaringInterface(type, method.getName(), method.getParameterTypes());
////					
////					if(type != null)
////					{
////						Method declmeth = null;
////						try
////						{
////							declmeth = type.getDeclaredMethod(method.getName(), method.getParameterTypes());
////						}
////						catch (Exception e)
////						{
////							// Should not happen, we know the method is there...
////						}
////						level = declmeth.getAnnotation(Security.class);
////						if (level == null)
////							level = type.getAnnotation(Security.class);
////					}
////				}
//				
//		/*if(level==null && access.getDescription().isSystemComponent())
//		{
//			level = DEFAULT_SYSTEM_SECURITY;
//		}*/
//		
//		// level==null -> disallow direct access to components (overridden by TRUSTED platform)
//		
		return level;
	}
	
	/**
	 *  Get the hashcode.
	 *  @return The hashcode.
	 */
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((providerid == null) ? 0 : providerid.hashCode());
		result = prime * result + ((servicename == null) ? 0 : servicename.hashCode());
		return result;
	}

	/**
	 *  Test if an object is equal to this one.
	 *  @param obj The object.
	 *  @return True, if equal.
	 */
	public boolean equals(Object obj)
	{
		boolean ret = false;
		if(obj instanceof IServiceIdentifier)
		{
			IServiceIdentifier sid = (IServiceIdentifier)obj;
			ret = sid.getProviderId().equals(getProviderId()) && sid.getServiceName().equals(getServiceName());
		}
		return ret;
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		if(tostring==null)
		{
			tostring	= getServiceName()+"@"+getProviderId();
//		return "ServiceIdentifier(providerid=" + providerid + ", type=" + type
//				+ ", servicename=" + servicename + ")";
		}
		return tostring;
	}
}
