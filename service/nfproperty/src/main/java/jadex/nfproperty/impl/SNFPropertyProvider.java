package jadex.nfproperty.impl;

import java.util.Map;

import jadex.common.MethodInfo;
import jadex.core.IExternalAccess;
import jadex.future.IFuture;
import jadex.nfproperty.INFProperty;
import jadex.nfproperty.INFPropertyMetaInfo;
import jadex.providedservice.IServiceIdentifier;

/**
 *  Static helper class for accessing nf properties also remotely.
 */
public class SNFPropertyProvider
{
	/**
	 *  Returns the declared names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public static IFuture<String[]> getNFPropertyNames(IExternalAccess component)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(ia ->
		{
			INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
			return nfp.getComponentPropertyProvider().getNFPropertyNames();
		});*/
	}
	
	/**
	 *  Returns the declared names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 * /
	public static IFuture<String[]> getNFPropertyNames(IExternalAccess component)
	{
		return component.scheduleStep(new IPriorityComponentStep<String[]>()
		{
			@Classname("getNFPropertyNames0")
			public IFuture<String[]> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getComponentPropertyProvider().getNFPropertyNames();
			}
		});
	}*/
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public static IFuture<String[]> getNFAllPropertyNames(IExternalAccess component)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<String[]>()
		{
			@Classname("getNFAllPropertyName1")
			public IFuture<String[]> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp==null? new Future<String[]>(SUtil.EMPTY_STRING_ARRAY): nfp.getComponentPropertyProvider().getNFAllPropertyNames();
			}
		});*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public static IFuture<Map<String, INFPropertyMetaInfo>> getNFPropertyMetaInfos(IExternalAccess component)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Map<String, INFPropertyMetaInfo>>()
		{
			@Classname("getNFPropertyMetaInfos2")
			public IFuture<Map<String, INFPropertyMetaInfo>> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp==null? new Future<Map<String, INFPropertyMetaInfo>>((Map<String, INFPropertyMetaInfo>)null) :nfp.getComponentPropertyProvider().getNFPropertyMetaInfos();
			}
		});*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public static IFuture<INFPropertyMetaInfo> getNFPropertyMetaInfo(IExternalAccess component, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<INFPropertyMetaInfo>()
		{
			@Classname("getNFPropertyMetaInfo3")
			public IFuture<INFPropertyMetaInfo> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getComponentPropertyProvider().getNFPropertyMetaInfo(name);
			}
		});*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public static <T> IFuture<T> getNFPropertyValue(IExternalAccess component, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<T>()
		{
			@Classname("getNFPropertyValue4")
			public IFuture<T> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getComponentPropertyProvider().getNFPropertyValue(name);
			}
		});*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
//	public <T, U> IFuture<T> getNFPropertyValue(String name, Class<U> unit);
	public static <T, U> IFuture<T> getNFPropertyValue(IExternalAccess component, final String name, final U unit)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<T>()
		{
			@Classname("getNFPropertyValue5")
			public IFuture<T> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getComponentPropertyProvider().getNFPropertyValue(name, unit);
			}
		});*/
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param nfprop The property.
	 */
	public static IFuture<Void> addNFProperty(IExternalAccess component, final INFProperty<?, ?> nfprop)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("addNFProperty6")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getComponentPropertyProvider().addNFProperty(nfprop);
			}
		});*/
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param The name.
	 */
	public static IFuture<Void> removeNFProperty(IExternalAccess component, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("removeNFProperty7")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getComponentPropertyProvider().removeNFProperty(name);
			}
		});*/
	}
	
	/**
	 *  Shutdown the provider.
	 */
	public static IFuture<Void> shutdownNFPropertyProvider(IExternalAccess component)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("shutdownNFPropertyProvider8")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getComponentPropertyProvider().shutdownNFPropertyProvider();
			}
		});*/
	}
	
	//-------- service methods --------
	
	/**
	 *  Returns the declared names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public static IFuture<String[]> getNFPropertyNames(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<String[]> ret = new Future<String[]>();

		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, String[]>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<String[]>()
				{
					@Classname("getNFPropertyNames9")
					public IFuture<String[]> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getNFPropertyNames();
					}
				}).addResultListener(new DelegationResultListener<String[]>(ret));
			}
		});
		
		return ret;*/
	}
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public static IFuture<String[]> getNFAllPropertyNames(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<String[]> ret = new Future<String[]>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, String[]>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<String[]>()
				{
					@Classname("getNFAllPropertyNames10")
					public IFuture<String[]> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getNFAllPropertyNames();
					}
				}).addResultListener(new DelegationResultListener<String[]>(ret));
			}
		});
		
		return ret;*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public static IFuture<Map<String, INFPropertyMetaInfo>> getNFPropertyMetaInfos(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Map<String, INFPropertyMetaInfo>> ret = new Future<Map<String, INFPropertyMetaInfo>>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Map<String, INFPropertyMetaInfo>>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Map<String, INFPropertyMetaInfo>>()
				{
					@Classname("getNFPropertyMetaInfos11")
					public IFuture<Map<String, INFPropertyMetaInfo>> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						if (nfp != null)
						{
							INFMixedPropertyProvider prov = nfp.getProvidedServicePropertyProvider(sid);
							if (prov != null)
							{
								IFuture<Map<String, INFPropertyMetaInfo>> metainf = prov.getNFPropertyMetaInfos();
								if (metainf != null)
								{
									return metainf;
								}
							}
						}
						return new Future<Map<String,INFPropertyMetaInfo>>(new HashMap<String,INFPropertyMetaInfo>());
					}
				}).addResultListener(new DelegationResultListener<Map<String, INFPropertyMetaInfo>>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public static IFuture<INFPropertyMetaInfo> getNFPropertyMetaInfo(IExternalAccess component, final IServiceIdentifier sid, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<INFPropertyMetaInfo> ret = new Future<INFPropertyMetaInfo>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, INFPropertyMetaInfo>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<INFPropertyMetaInfo>()
				{
					@Classname("getNFPropertyMetaInfo12")
					public IFuture<INFPropertyMetaInfo> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getNFPropertyMetaInfo(name);
					}
				}).addResultListener(new DelegationResultListener<INFPropertyMetaInfo>(ret));
			}
		});
		
		return ret;*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public static <T> IFuture<T> getNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<T> ret = new Future<T>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, T>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<T>()
				{
					@Classname("getNFPropertyValue13")
					public IFuture<T> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getNFPropertyValue(name);
					}
				}).addResultListener(new DelegationResultListener<T>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
//	public <T, U> IFuture<T> getNFPropertyValue(String name, Class<U> unit);
	public static <T, U> IFuture<T> getNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final String name, final U unit)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<T> ret = new Future<T>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, T>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<T>()
				{
					@Classname("getNFPropertyValue14")
					public IFuture<T> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getNFPropertyValue(name, unit);
					}
				}).addResultListener(new DelegationResultListener<T>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param nfprop The property.
	 */
	public static IFuture<Void> addNFProperty(IExternalAccess component, final IServiceIdentifier sid, final INFProperty<?, ?> nfprop)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Void> ret = new Future<Void>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Void>()
				{
					@Classname("addNFProperty15")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).addNFProperty(nfprop);
					}
				}).addResultListener(new DelegationResultListener<Void>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param The name.
	 */
	public static IFuture<Void> removeNFProperty(IExternalAccess component, final IServiceIdentifier sid, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Void> ret = new Future<Void>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Void>()
				{
					@Classname("removeNFProperty16")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).removeNFProperty(name);
					}
				}).addResultListener(new DelegationResultListener<Void>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Shutdown the provider.
	 */
	public static IFuture<Void> shutdownNFPropertyProvider(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Void> ret = new Future<Void>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Void>()
				{
					@Classname("shutdownNFPropertyProvider17")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).shutdownNFPropertyProvider();
					}
				}).addResultListener(new DelegationResultListener<Void>(ret));
			}
		});

		return ret;*/
	}
	
	//-------- provided service methods --------
	
	/**
	 *  Returns meta information about a non-functional properties of all methods.
	 *  @return The meta information about a non-functional properties.
	 */
	public static IFuture<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> getMethodNFPropertyMetaInfos(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> ret = new Future<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Map<MethodInfo, Map<String, INFPropertyMetaInfo>>>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>>()
				{
					@Classname("getMethodNFPropertyMetaInfos18")
					public IFuture<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfos();
					}
				}).addResultListener(new DelegationResultListener<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Returns the names of all non-functional properties of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @return The names of the non-functional properties of the specified method.
	 */
	public static IFuture<String[]> getMethodNFPropertyNames(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<String[]> ret = new Future<String[]>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, String[]>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<String[]>()
				{
					@Classname("getMethodNFPropertyNames19")
					public IFuture<String[]> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyNames(method);
					}
				}).addResultListener(new DelegationResultListener<String[]>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Returns the names of all non-functional properties of this method.
	 *  This includes the properties of all parent components.
	 *  @return The names of the non-functional properties of this method.
	 */
	public static IFuture<String[]> getMethodNFAllPropertyNames(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<String[]> ret = new Future<String[]>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, String[]>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<String[]>()
				{
					@Classname("getMethodNFAllPropertyNames20")
					public IFuture<String[]> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getMethodNFAllPropertyNames(method);
					}
				}).addResultListener(new DelegationResultListener<String[]>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Returns meta information about a non-functional properties of a method.
	 *  @return The meta information about a non-functional properties.
	 */
	public static IFuture<Map<String, INFPropertyMetaInfo>> getMethodNFPropertyMetaInfos(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Map<String, INFPropertyMetaInfo>> ret = new Future<Map<String, INFPropertyMetaInfo>>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Map<String, INFPropertyMetaInfo>>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Map<String, INFPropertyMetaInfo>>()
				{
					@Classname("getMethodNFPropertyMetaInfos21")
					public IFuture<Map<String, INFPropertyMetaInfo>> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfos(method);
					}
				}).addResultListener(new DelegationResultListener<Map<String, INFPropertyMetaInfo>>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of the specified method.
	 */
	public static IFuture<INFPropertyMetaInfo> getMethodNFPropertyMetaInfo(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<INFPropertyMetaInfo> ret = new Future<INFPropertyMetaInfo>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, INFPropertyMetaInfo>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<INFPropertyMetaInfo>()
				{
					@Classname("getMethodNFPropertyMetaInfo22")
					public IFuture<INFPropertyMetaInfo> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfo(method, name);
					}
				}).addResultListener(new DelegationResultListener<INFPropertyMetaInfo>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
	public static <T> IFuture<T> getMethodNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<T> ret = new Future<T>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, T>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<T>()
				{
					@Classname("getMethodNFPropertyValue23")
					public IFuture<T> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyValue(method, name);
					}
				}).addResultListener(new DelegationResultListener<T>(ret));
			}
		});
		
//		ret.addResultListener(new IResultListener<T>()
//		{
//			public void resultAvailable(T result)
//			{
//				System.out.println("t: "+result);
//			}
//
//			public void exceptionOccurred(Exception exception)
//			{
//				System.out.println("ex: "+exception);
//			}
//		});
		
		return ret;*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method, performs unit conversion.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
//	public <T, U> IFuture<T> getNFPropertyValue(Method method, String name, Class<U> unit);
	public static <T, U> IFuture<T> getMethodNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name, final U unit)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<T> ret = new Future<T>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, T>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<T>()
				{
					@Classname("getMethodNFPropertyValue24")
					public IFuture<T> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyValue(method, name, unit);
					}
				}).addResultListener(new DelegationResultListener<T>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param nfprop The property.
	 */
	public static IFuture<Void> addMethodNFProperty(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final INFProperty<?, ?> nfprop)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Void> ret = new Future<Void>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Void>()
				{
					@Classname("addMethodNFProperty25")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).addMethodNFProperty(method, nfprop);
					}
				}).addResultListener(new DelegationResultListener<Void>(ret));
			}
		});

		return ret;*/
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param The name.
	 */
	public static IFuture<Void> removeMethodNFProperty(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*final Future<Void> ret = new Future<Void>();
		
		component.getExternalAccessAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				result.scheduleStep(new IPriorityComponentStep<Void>()
				{
					@Classname("removeMethodNFProperty26")
					public IFuture<Void> execute(IInternalAccess ia)
					{
						INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
						return nfp.getProvidedServicePropertyProvider(sid).removeMethodNFProperty(method, name);
					}
				}).addResultListener(new DelegationResultListener<Void>(ret));
			}
		});

		return ret;*/
	}

	//-------- required properties --------
	
	/**
	 *  Returns the declared names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public static IFuture<String[]> getRequiredNFPropertyNames(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<String[]>()
		{
			@Classname("getRequiredNFPropertyNames27")
			public IFuture<String[]> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getNFPropertyNames();
			}
		});*/
	}
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public static IFuture<String[]> getRequiredNFAllPropertyNames(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<String[]>()
		{
			@Classname("getRequiredNFAllPropertyNames28")
			public IFuture<String[]> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getNFAllPropertyNames();
			}
		});*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public static IFuture<Map<String, INFPropertyMetaInfo>> getRequiredNFPropertyMetaInfos(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Map<String, INFPropertyMetaInfo>>()
		{
			@Classname("getRequiredNFPropertyMetaInfos29")
			public IFuture<Map<String, INFPropertyMetaInfo>> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getNFPropertyMetaInfos();
			}
		});*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public static IFuture<INFPropertyMetaInfo> getRequiredNFPropertyMetaInfo(IExternalAccess component, final IServiceIdentifier sid, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<INFPropertyMetaInfo>()
		{
			@Classname("getRequiredNFPropertyMetaInfo30")
			public IFuture<INFPropertyMetaInfo> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getNFPropertyMetaInfo(name);
			}
		});*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public static <T> IFuture<T> getRequiredNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<T>()
		{
			@Classname("getRequiredNFPropertyValue31")
			public IFuture<T> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getNFPropertyValue(name);
			}
		});*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
//	public <T, U> IFuture<T> getNFPropertyValue(String name, Class<U> unit);
	public static <T, U> IFuture<T> getRequiredNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final String name, final U unit)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<T>()
		{
			@Classname("getRequiredNFPropertyValue32")
			public IFuture<T> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getNFPropertyValue(name, unit);
			}
		});*/
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param nfprop The property.
	 */
	public static IFuture<Void> addRequiredNFProperty(IExternalAccess component, final IServiceIdentifier sid, final INFProperty<?, ?> nfprop)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("addRequiredNFProperty33")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).addNFProperty(nfprop);
			}
		});*/
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param The name.
	 */
	public static IFuture<Void> removeRequiredNFProperty(IExternalAccess component, final IServiceIdentifier sid, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("removeRequiredNFProperty34")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).removeNFProperty(name);
			}
		});*/
	}
	
	/**
	 *  Shutdown the provider.
	 */
	public static IFuture<Void> shutdownRequiredNFPropertyProvider(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("shutdownRequiredNFPropertyProvider35")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).shutdownNFPropertyProvider();
			}
		});*/
	}
	
	/**
	 *  Returns meta information about a non-functional properties of all methods.
	 *  @return The meta information about a non-functional properties.
	 */
	public static IFuture<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> getRequiredMethodNFPropertyMetaInfos(IExternalAccess component, final IServiceIdentifier sid)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>>()
		{
			@Classname("getRequiredMethodNFPropertyMetaInfos36")
			public IFuture<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getMethodNFPropertyMetaInfos();
			}
		});*/
	}
	
	/**
	 *  Returns the names of all non-functional properties of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @return The names of the non-functional properties of the specified method.
	 */
	public static IFuture<String[]> getRequiredMethodNFPropertyNames(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<String[]>()
		{
			@Classname("getRequiredMethodNFPropertyNames37")
			public IFuture<String[]> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getMethodNFPropertyNames(method);
			}
		});*/
	}
	
	/**
	 *  Returns the names of all non-functional properties of this method.
	 *  This includes the properties of all parent components.
	 *  @return The names of the non-functional properties of this method.
	 */
	public static IFuture<String[]> getRequiredMethodNFAllPropertyNames(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<String[]>()
		{
			@Classname("getRequiredMethodNFAllPropertyNames38")
			public IFuture<String[]> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getMethodNFAllPropertyNames(method);
			}
		});*/
	}
	
	/**
	 *  Returns meta information about a non-functional properties of a method.
	 *  @return The meta information about a non-functional properties.
	 */
	public static IFuture<Map<String, INFPropertyMetaInfo>> getRequiredMethodNFPropertyMetaInfos(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Map<String, INFPropertyMetaInfo>>()
		{
			@Classname("getRequiredMethodNFPropertyMetaInfos39")
			public IFuture<Map<String, INFPropertyMetaInfo>> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getMethodNFPropertyMetaInfos(method);
			}
		});*/
	}
	
	/**
	 *  Returns the meta information about a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of the specified method.
	 */
	public static IFuture<INFPropertyMetaInfo> getRequiredMethodNFPropertyMetaInfo(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<INFPropertyMetaInfo>()
		{
			@Classname("getRequiredMethodNFPropertyMetaInfo40")
			public IFuture<INFPropertyMetaInfo> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getMethodNFPropertyMetaInfo(method, name);
			}
		});*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
	public static <T> IFuture<T> getRequiredMethodNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<T>()
		{
			@Classname("getRequiredMethodNFPropertyValue41")
			public IFuture<T> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getMethodNFPropertyValue(method, name);
			}
		});*/
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method, performs unit conversion.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
//	public <T, U> IFuture<T> getNFPropertyValue(Method method, String name, Class<U> unit);
	public static <T, U> IFuture<T> getRequiredMethodNFPropertyValue(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name, final U unit)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<T>()
		{
			@Classname("getRequiredMethodNFPropertyValue42")
			public IFuture<T> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).getMethodNFPropertyValue(method, name, unit);
			}
		});*/
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param nfprop The property.
	 */
	public static IFuture<Void> addRequiredMethodNFProperty(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final INFProperty<?, ?> nfprop)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("addRequiredMethodNFProperty43")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).addMethodNFProperty(method, nfprop);
			}
		});*/
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param The name.
	 */
	public static IFuture<Void> removeRequiredMethodNFProperty(IExternalAccess component, final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		throw new UnsupportedOperationException();
		
		/*return component.scheduleStep(new IPriorityComponentStep<Void>()
		{
			@Classname("removeRequiredMethodNFProperty44")
			public IFuture<Void> execute(IInternalAccess ia)
			{
				INFPropertyComponentFeature nfp = ia.getFeature(INFPropertyComponentFeature.class);
				return nfp.getRequiredServicePropertyProvider(sid).removeMethodNFProperty(method, name);
			}
		});*/
	}
	
}
