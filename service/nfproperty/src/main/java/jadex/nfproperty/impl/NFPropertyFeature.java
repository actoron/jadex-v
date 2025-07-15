package jadex.nfproperty.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jadex.collection.ILRUEntryCleaner;
import jadex.collection.LRU;
import jadex.common.MethodInfo;
import jadex.common.SUtil;
import jadex.common.Tuple2;
import jadex.common.UnparsedExpression;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingFunction;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.core.impl.ILifecycle;
import jadex.future.DefaultResultListener;
import jadex.future.Future;
import jadex.future.FutureBarrier;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.future.ITerminableIntermediateFuture;
import jadex.future.TerminableIntermediateDelegationFuture;
import jadex.nfproperty.INFMixedPropertyProvider;
import jadex.nfproperty.INFProperty;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.INFPropertyMetaInfo;
import jadex.nfproperty.INFPropertyProvider;
import jadex.nfproperty.annotation.Tag;
import jadex.nfproperty.annotation.Tags;
import jadex.nfproperty.impl.modelinfo.NFPropertyInfo;
import jadex.nfproperty.impl.search.IRankingSearchTerminationDecider;
import jadex.nfproperty.impl.search.IServiceRanker;
import jadex.nfproperty.impl.search.ServiceRankingDelegationResultListener;
import jadex.nfproperty.impl.search.ServiceRankingDelegationResultListener2;
import jadex.nfproperty.sensor.service.TagProperty;
import jadex.providedservice.IService;
import jadex.providedservice.IServiceIdentifier;

public class NFPropertyFeature implements ILifecycle, INFPropertyFeature  
{
	//-------- attributes --------
	
	protected Component self;
	
	/** The component property provider. */
	protected INFPropertyProvider compprovider;
	
	/** The nf property providers for required services. */
	protected Map<IServiceIdentifier, INFMixedPropertyProvider> proserprops;
	
	/** The nf property providers for required services. */
	protected Map<IServiceIdentifier, INFMixedPropertyProvider> reqserprops;
	
	/** The max number of preserved req service providers. */
	protected int maxreq;
	
	///** The parent provider. */
	//protected INFPropertyProvider parent;
	
	//-------- constructors --------
	
	protected NFPropertyFeature(Component self)
	{
		this.self = self;
	}
	
	public Component getComponent()
	{
		return self;
	}
	
	@Override
	public void	init()
	{
//		ModelInfo model = (ModelInfo)self.getFeature(IModelFeature.class).getModel();
//		NFPropertyModel mymodel = (NFPropertyModel)model.getFeatureModel(INFPropertyFeature.class);
//		if(mymodel==null)
//			mymodel = loadModel();
//		
//		if(mymodel!=null)
//		{
//			// Init nf component props
//			FutureBarrier<Void> bar = new FutureBarrier<Void>();
//			
//			Collection<NFPropertyInfo> nfprops = mymodel.getComponentProperties();
//			for(NFPropertyInfo nfprop: nfprops)
//			{
//				try
//				{
//					Class<?> clazz = nfprop.getClazz().getType(getComponent().getClassLoader(), model.getAllImports());
//					INFProperty<?, ?> nfp = AbstractNFProperty.createProperty(clazz, getComponent(), null, null, nfprop.getParameters());
//					bar.add(getComponentPropertyProvider().addNFProperty(nfp));
//				}
//				catch(Exception e)
//				{
//					System.out.println("Property creation problem: "+e);
//				}
//			}
//			
//			Collection<String> names = mymodel.getProvidedServiceNames();
//			IProvidedServiceFeature psf = self.getFeature(IProvidedServiceFeature.class);
//			final NFPropertyModel fmymodel = mymodel;
//			
//			names.forEach(name -> 
//			{
//				IService ser = psf.getProvidedService(name);
//				
//				Map<MethodInfo, List<NFPropertyInfo>> nfps = fmymodel.getProvidedServiceMethodProperties(name);
//				nfps.entrySet().forEach(entry ->
//				{
//					bar.add(addNFMethodProperties(entry.getValue(), ser, entry.getKey()));
//				});
//				
//				List<NFPropertyInfo> snfps = fmymodel.getProvidedServiceProperties(name);
//				if(snfps!=null)
//				{
//					bar.add(addNFProperties(snfps, ser));
//					
//					// tags handled directly in provided service now
//					// Hack?! must update tags in sid :-(
//					/*IServiceIdentifier sid = ser.getServiceId();
//					getProvidedServicePropertyProvider(sid).getNFPropertyValue(TagProperty.NAME).then(val ->
//					{
//						Collection<String> coll = val == null ? new ArrayList<String>() : new LinkedHashSet<String>((Collection<String>)val);
//						Set<String> tags = new LinkedHashSet<String>(coll);
//						((ServiceIdentifier)sid).setTags(tags);
//						// Hack!!! re-index
//						ServiceRegistry reg = (ServiceRegistry)ServiceRegistry.getRegistry();
//						reg.updateService(sid);
//					}).catchEx(ex -> System.out.println("not found tag"));*/
//				}
//			});
//			
//			bar.waitFor().get();
		}
//	}
	
	/**
	 *  Called when the feature is shutdowned.
	 */
	public void	cleanup()
	{
		FutureBarrier<Void> bar = new FutureBarrier<Void>();
		
		if(compprovider!=null)
			bar.add(compprovider.shutdownNFPropertyProvider());
		if(proserprops!=null)
			proserprops.values().stream().forEach(p -> bar.add(p.shutdownNFPropertyProvider()));
		if(reqserprops!=null)
			reqserprops.values().stream().forEach(p -> bar.add(p.shutdownNFPropertyProvider()));

		bar.waitFor().get();
	}
	
	/**
	 *  Get the component property provider.
	 */
	public INFPropertyProvider getComponentPropertyProvider()
	{
		if(compprovider==null)
			this.compprovider = new NFPropertyProvider(null, getComponent()); 
		
		return compprovider;
	}
	
	/**
	 *  Get the required service property provider for a service.
	 */
	public INFMixedPropertyProvider getRequiredServicePropertyProvider(IServiceIdentifier sid)
	{
		INFMixedPropertyProvider ret = null;
		if(reqserprops==null)
		{
			reqserprops = new LRU<IServiceIdentifier, INFMixedPropertyProvider>(maxreq, new ILRUEntryCleaner<IServiceIdentifier, INFMixedPropertyProvider>()
			{
				public void cleanupEldestEntry(Entry<IServiceIdentifier, INFMixedPropertyProvider> eldest)
				{
					eldest.getValue().shutdownNFPropertyProvider().addResultListener(new DefaultResultListener<Void>()
					{
						public void resultAvailable(Void result)
						{
						}
					});
				}
			}); 
		}
		ret = reqserprops.get(sid);
		if(ret==null)
		{
			ret = new NFMethodPropertyProvider(getComponent().getId(), getComponent()); 
			reqserprops.put(sid, ret);
//			System.out.println("created req ser provider: "+sid+" "+hashCode());
		}
		return ret;
	}
	
	/**
	 *  Has the service a property provider.
	 */
	public boolean hasRequiredServicePropertyProvider(IServiceIdentifier sid)
	{
		return reqserprops!=null? reqserprops.get(sid)!=null: false;
	}
	
	/**
	 *  Get the provided service property provider for a service.
	 */
	public INFMixedPropertyProvider getProvidedServicePropertyProvider(IServiceIdentifier sid)
	{
		INFMixedPropertyProvider ret = null;
		if(proserprops==null)
		{
			proserprops = new HashMap<IServiceIdentifier, INFMixedPropertyProvider>();
		}
		ret = proserprops.get(sid);
		if(ret==null)
		{
			ret = new NFMethodPropertyProvider(getComponent().getId(), getComponent()); 
			proserprops.put(sid, ret);
		}
		return ret;
	}
	
//		/**
//		 *  Get the provided service property provider for a service.
//		 */
//		public INFMixedPropertyProvider getProvidedServicePropertyProvider(Class<?> iface)
//		{
//		}
	
	/**
	 *  Init the service and method nf properties. 
	 * /
	public IFuture<Void> initNFProperties(final IInternalService ser, Class<?> impltype)
	{
		final Future<Void> ret = new Future<Void>();
		
		List<Class<?>> classes = new ArrayList<Class<?>>();
		Class<?> superclazz = ser.getServiceId().getServiceType().getType(getComponent().getClassLoader());
		while(superclazz != null && !Object.class.equals(superclazz))
		{
			classes.add(superclazz);
			superclazz = superclazz.getSuperclass();
		}
		
		if(impltype!=null)
		{
			superclazz = impltype;
			while(superclazz != null && !BasicService.class.equals(superclazz) && !Object.class.equals(superclazz))
			{
				classes.add(superclazz);
				superclazz = superclazz.getSuperclass();
			}
		}
//			Collections.reverse(classes);
		
		int cnt = 0;
		
		LateCounterListener<Void> lis = new LateCounterListener<Void>(new DelegationResultListener<Void>(ret));
		
		Map<MethodInfo, Method> meths = new HashMap<MethodInfo, Method>();
		for(Class<?> sclazz: classes)
		{
			if(sclazz.isAnnotationPresent(NFProperties.class))
			{
				addNFProperties(sclazz.getAnnotation(NFProperties.class), ser).addResultListener(lis);
				cnt++;
			}
			
			if(sclazz.isAnnotationPresent(Tags.class))
			{
				addTags(sclazz.getAnnotation(Tags.class), ser).addResultListener(lis);
				cnt++;
			}
			
			Method[] methods = sclazz.getMethods();
			for(Method m : methods)
			{
				if(m.isAnnotationPresent(NFProperties.class))
				{
					MethodInfo mis = new MethodInfo(m.getName(), m.getParameterTypes());
					if(!meths.containsKey(mis))
					{
						meths.put(mis, m);
					}
				}
			}
		}
		
		for(MethodInfo key: meths.keySet())
		{
			addNFMethodProperties(meths.get(key).getAnnotation(NFProperties.class), ser, key).addResultListener(lis);
			cnt++;
		}
		
		// Set the number of issued calls
		lis.setMax(cnt);

		return ret;
	}*/
	
	/**
	 *  Add nf properties from a type.
	 */
	public IFuture<Void> addNFProperties(List<NFPropertyInfo> infos, IService ser)
	{
		FutureBarrier<Void> bar = new FutureBarrier<>();
		INFMixedPropertyProvider prov = getProvidedServicePropertyProvider(ser.getServiceId());
		
		for(NFPropertyInfo nfprop : infos)
		{
			Class<?> clazz = nfprop.getClazz().getType(ComponentManager.get().getClassLoader());
			INFProperty<?, ?> prop = AbstractNFProperty.createProperty(clazz, getComponent(), ser, null, nfprop.getParameters());
			bar.add(prov.addNFProperty(prop));
		}
		return bar.waitFor();
	}
	
	/**
	 *  Add nf properties from a type.
	 */
	public IFuture<Void> addTags(Tags tags, IService ser)
	{
		INFMixedPropertyProvider prov = getProvidedServicePropertyProvider(ser.getServiceId());
		
		List<UnparsedExpression> params = new ArrayList<>();
		
//			if(tags.argumentname().length()>0)
//				params.add(new UnparsedExpression(TagProperty.ARGUMENT, "\""+tags.argumentname()+"\""));
		
		for(int i=0; i<tags.value().length; i++)
		{
			Tag tag = tags.value()[i];
			
			/*if(tag.include().length()>0)
			{
				try
				{
					IModelFeature mf = getComponent().getFeature(IModelFeature.class);
					Object val = SJavaParser.evaluateExpression(tag.include(), mf.getModel().getAllImports(), mf.getFetcher(), IComponentManager.get().getClassLoader());
					if(val instanceof Boolean && ((Boolean)val).booleanValue())
						params.add(new UnparsedExpression(TagProperty.NAME+"_"+i, tag.value()));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{*/
				params.add(new UnparsedExpression(TagProperty.NAME+"_"+i, tag.value()));
			//}
		}
		
		IFuture<Void> ret = IFuture.DONE;
		if(params.size()>0)
		{
			INFProperty<?, ?> prop = AbstractNFProperty.createProperty(TagProperty.class, getComponent(), ser, null, params);
			ret = prov.addNFProperty(prop);
		}
		return ret;
	}
	
	/**
	 *  Add nf properties from a type.
	 */
	public IFuture<Void> addNFMethodProperties(List<NFPropertyInfo> nfprops, IService ser, MethodInfo mi)
	{
		INFMixedPropertyProvider prov = getProvidedServicePropertyProvider(ser.getServiceId());
		FutureBarrier<Void> bar = new FutureBarrier<>();
		
		for(NFPropertyInfo nfprop: nfprops)
		{
			Class<?> clazz = nfprop.getClazz().getType(ComponentManager.get().getClassLoader());
			INFProperty<?, ?> prop = AbstractNFProperty.createProperty(clazz, getComponent(), ser, mi, nfprop.getParameters());
			bar.add(prov.addMethodNFProperty(mi, prop));
		}
		
		return bar.waitFor();
	}
	
	/**
	 *  Get external feature facade.
	 */
	public <T> T getExternalFacade(Object context)
	{
		T ret = null;
		if(context instanceof IService)
		{
//				IServiceIdentifier sid = (IServiceIdentifier)context;
			ret = (T)getProvidedServicePropertyProvider(((IService)context).getServiceId());
		}
		else 
		{
			ret = (T)getComponentPropertyProvider();
		}
		
		return ret;
	}
	
//		/**
//		 * 
//		 */
//		public <T> Class<T> getExternalFacadeType(Object context)
//		{
//			Class<T> ret = (Class<T>)INFPropertyComponentFeature.class;
//			if(context instanceof IService)
//			{
//				ret = (Class<T>)INFMixedPropertyProvider.class;
//			}
//			return ret;
//		}
	
	
	/**
	 *  Returns the declared names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getNFPropertyNames()
	{
		return getComponentPropertyProvider().getNFPropertyNames();
	}
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getNFAllPropertyNames()
	{
		return getComponentPropertyProvider().getNFAllPropertyNames();
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<Map<String, INFPropertyMetaInfo>> getNFPropertyMetaInfos()
	{
		return getComponentPropertyProvider().getNFPropertyMetaInfos();
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<INFPropertyMetaInfo> getNFPropertyMetaInfo(String name)
	{
		return getNFPropertyMetaInfo(name);
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public <T> IFuture<T> getNFPropertyValue(String name)
	{
		return getComponentPropertyProvider().getNFPropertyValue(name);
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public <T, U> IFuture<T> getNFPropertyValue(String name, U unit)
	{
		return getComponentPropertyProvider().getNFPropertyValue(name, unit);
	}
	
	/**
	 *  Returns the current value of a non-functional property of this component.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this component as string.
	 */
	public IFuture<String> getNFPropertyPrettyPrintValue(String name) 
	{
		return getComponentPropertyProvider().getNFPropertyPrettyPrintValue(name);
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param nfprop The property.
	 */
	public IFuture<Void> addNFProperty(INFProperty<?, ?> nfprop)
	{
		return getComponentPropertyProvider().addNFProperty(nfprop);
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param The name.
	 */
	public IFuture<Void> removeNFProperty(String name)
	{
		return getComponentPropertyProvider().removeNFProperty(name);
	}
	
	/**
	 *  Shutdown the provider.
	 */
	public IFuture<Void> shutdownNFPropertyProvider()
	{
		return getComponentPropertyProvider().shutdownNFPropertyProvider();
	}
	
	//-------- service methods --------
	
	/**
	 *  Returns the declared names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getNFPropertyNames(IServiceIdentifier sid)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			 return getProvidedServicePropertyProvider(sid).getNFPropertyNames();
		}
		else
		{
			final Future<String[]> ret = new Future<String[]>();

			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<String[]>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				//return nfp.getProvidedServicePropertyProvider(sid).getNFPropertyNames();
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getNFPropertyNames(): new Future<String[]>(SUtil.EMPTY_STRING_ARRAY);
			}).delegateTo(ret);
			
			/*component.getComponentHandleAsync(sid.getProviderId()).addResultListener(new ExceptionDelegationResultListener<IExternalAccess, String[]>(ret)
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
			});*/

			return ret;
		}
	}
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getNFAllPropertyNames(final IServiceIdentifier sid)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			 return getProvidedServicePropertyProvider(sid).getNFAllPropertyNames();
		}
		else
		{
			final Future<String[]> ret = new Future<String[]>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<String[]>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getNFAllPropertyNames(): new Future<String[]>(SUtil.EMPTY_STRING_ARRAY);
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<Map<String, INFPropertyMetaInfo>> getNFPropertyMetaInfos(IServiceIdentifier sid)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			INFMixedPropertyProvider prov = getProvidedServicePropertyProvider(sid);
			if(prov!=null)
			{
				IFuture<Map<String, INFPropertyMetaInfo>> metainf = prov.getNFPropertyMetaInfos();
				if(metainf!=null)
				{
					return metainf;
				}
			}
			return new Future<Map<String,INFPropertyMetaInfo>>(new HashMap<String,INFPropertyMetaInfo>());
		}
		else
		{
			final Future<Map<String, INFPropertyMetaInfo>> ret = new Future<Map<String, INFPropertyMetaInfo>>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Map<String, INFPropertyMetaInfo>>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				INFMixedPropertyProvider prov = nfp.getProvidedServicePropertyProvider(sid);
				if(prov!=null)
				{
					IFuture<Map<String, INFPropertyMetaInfo>> metainf = prov.getNFPropertyMetaInfos();
					if(metainf!=null)
						return metainf;
				}
				return new Future<Map<String,INFPropertyMetaInfo>>(new HashMap<String,INFPropertyMetaInfo>());				
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<INFPropertyMetaInfo> getNFPropertyMetaInfo(final IServiceIdentifier sid, final String name)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getNFPropertyMetaInfo(name);
		}
		else
		{
			final Future<INFPropertyMetaInfo> ret = new Future<INFPropertyMetaInfo>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<INFPropertyMetaInfo>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getNFPropertyMetaInfo(name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public <T> IFuture<T> getNFPropertyValue(final IServiceIdentifier sid, final String name)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getNFPropertyValue(name);
		}
		else
		{
			final Future<T> ret = new Future<T>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<T>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getNFPropertyValue(name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public <T, U> IFuture<T> getNFPropertyValue(final IServiceIdentifier sid, final String name, final U unit)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getNFPropertyValue(name, unit);
		}
		else
		{
			final Future<T> ret = new Future<T>();
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<T>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getNFPropertyValue(name, unit): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service as string.
	 */
	public IFuture<String> getNFPropertyPrettyPrintValue(IServiceIdentifier sid, String name) 
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getNFPropertyPrettyPrintValue(name);
		}
		else
		{
			final Future<String> ret = new Future<String>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<String>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getNFPropertyPrettyPrintValue(name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param nfprop The property.
	 */
	public IFuture<Void> addNFProperty(final IServiceIdentifier sid, final INFProperty<?, ?> nfprop)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).addNFProperty(nfprop);
		}
		else
		{
			final Future<Void> ret = new Future<Void>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Void>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).addNFProperty(nfprop): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param The name.
	 */
	public IFuture<Void> removeNFProperty(final IServiceIdentifier sid, final String name)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).removeNFProperty(name);
		}
		else
		{
			final Future<Void> ret = new Future<Void>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Void>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).removeNFProperty(name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Shutdown the provider.
	 */
	public IFuture<Void> shutdownNFPropertyProvider(final IServiceIdentifier sid)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).shutdownNFPropertyProvider();
		}
		else
		{
			final Future<Void> ret = new Future<Void>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Void>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).shutdownNFPropertyProvider(): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	//-------- provided service methods --------
	
	/**
	 *  Returns meta information about a non-functional properties of all methods.
	 *  @return The meta information about a non-functional properties.
	 */
	public IFuture<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> getMethodNFPropertyMetaInfos(final IServiceIdentifier sid)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfos();
		}
		else
		{
			final Future<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> ret = new Future<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>>();

			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfos(): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the names of all non-functional properties of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @return The names of the non-functional properties of the specified method.
	 */
	public IFuture<String[]> getMethodNFPropertyNames(final IServiceIdentifier sid, final MethodInfo method)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyNames(method);
		}
		else
		{
			final Future<String[]> ret = new Future<String[]>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<String[]>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyNames(method): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the names of all non-functional properties of this method.
	 *  This includes the properties of all parent components.
	 *  @return The names of the non-functional properties of this method.
	 */
	public IFuture<String[]> getMethodNFAllPropertyNames(final IServiceIdentifier sid, final MethodInfo method)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyNames(method);
		}
		else
		{
			final Future<String[]> ret = new Future<String[]>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<String[]>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFAllPropertyNames(method): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns meta information about a non-functional properties of a method.
	 *  @return The meta information about a non-functional properties.
	 */
	public IFuture<Map<String, INFPropertyMetaInfo>> getMethodNFPropertyMetaInfos(final IServiceIdentifier sid, final MethodInfo method)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfos(method);
		}
		else
		{
			final Future<Map<String, INFPropertyMetaInfo>> ret = new Future<Map<String, INFPropertyMetaInfo>>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Map<String, INFPropertyMetaInfo>>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfos(method): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the meta information about a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of the specified method.
	 */
	public IFuture<INFPropertyMetaInfo> getMethodNFPropertyMetaInfo(final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfo(method, name);
		}
		else
		{
			final Future<INFPropertyMetaInfo> ret = new Future<INFPropertyMetaInfo>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<INFPropertyMetaInfo>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyMetaInfo(method, name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
	public <T> IFuture<T> getMethodNFPropertyValue(final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyValue(method, name);
		}
		else
		{
			final Future<T> ret = new Future<T>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<T>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyValue(method, name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method, performs unit conversion.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
//		public <T, U> IFuture<T> getNFPropertyValue(Method method, String name, Class<U> unit);
	public <T, U> IFuture<T> getMethodNFPropertyValue(final IServiceIdentifier sid, final MethodInfo method, final String name, final U unit)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyValue(method, name, unit);
		}
		else
		{
			final Future<T> ret = new Future<T>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<T>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyValue(method, name, unit): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	

	public IFuture<String> getMethodNFPropertyPrettyPrintValue(IServiceIdentifier sid, MethodInfo method, String name) 
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).getMethodNFPropertyPrettyPrintValue(method, name);
		}
		else
		{
			final Future<String> ret = new Future<String>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<String>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).getMethodNFPropertyPrettyPrintValue(method, name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param nfprop The property.
	 */
	public IFuture<Void> addMethodNFProperty(final IServiceIdentifier sid, final MethodInfo method, final INFProperty<?, ?> nfprop)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).addMethodNFProperty(method, nfprop);
		}
		else
		{
			final Future<Void> ret = new Future<Void>();
			
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Void>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).addMethodNFProperty(method, nfprop): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param The name.
	 */
	public IFuture<Void> removeMethodNFProperty(final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		if(sid.getProviderId().equals(getComponent().getId()))
		{
			return getProvidedServicePropertyProvider(sid).removeMethodNFProperty(method, name);
		}
		else
		{
			final Future<Void> ret = new Future<Void>();
			IComponentHandle ex = getComponent().getComponentHandle(sid.getProviderId());
			
			ex.scheduleAsyncStep((IThrowingFunction<IComponent, IFuture<Void>>)agent ->
			{
				INFPropertyFeature nfp = agent.getFeature(INFPropertyFeature.class);
				return nfp!=null? nfp.getProvidedServicePropertyProvider(sid).removeMethodNFProperty(method, name): new Future<>(new RuntimeException("No nf properties"));
			}).delegateTo(ret);
			
			return ret;
		}
	}

	//-------- required properties --------
	
	/**
	 *  Returns the declared names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getRequiredNFPropertyNames(final IServiceIdentifier sid)
	{
		return getRequiredServicePropertyProvider(sid).getNFPropertyNames();
	}
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getRequiredNFAllPropertyNames(final IServiceIdentifier sid)
	{
		return getRequiredServicePropertyProvider(sid).getNFAllPropertyNames();
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<Map<String, INFPropertyMetaInfo>> getRequiredNFPropertyMetaInfos(final IServiceIdentifier sid)
	{
		return getRequiredServicePropertyProvider(sid).getNFPropertyMetaInfos();
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<INFPropertyMetaInfo> getRequiredNFPropertyMetaInfo(final IServiceIdentifier sid, final String name)
	{
		return getRequiredServicePropertyProvider(sid).getNFPropertyMetaInfo(name);
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public <T> IFuture<T> getRequiredNFPropertyValue(final IServiceIdentifier sid, final String name)
	{
		return getRequiredServicePropertyProvider(sid).getNFPropertyValue(name);
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
//		public <T, U> IFuture<T> getNFPropertyValue(String name, Class<U> unit);
	public <T, U> IFuture<T> getRequiredNFPropertyValue(final IServiceIdentifier sid, final String name, final U unit)
	{
		return getRequiredServicePropertyProvider(sid).getNFPropertyValue(name, unit);
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public IFuture<String> getRequiredNFPropertyPrettyPrintValue(IServiceIdentifier sid, String name) 
	{
		return getRequiredServicePropertyProvider(sid).getNFPropertyPrettyPrintValue(name);
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param nfprop The property.
	 */
	public IFuture<Void> addRequiredNFProperty(final IServiceIdentifier sid, final INFProperty<?, ?> nfprop)
	{
		return getRequiredServicePropertyProvider(sid).addNFProperty(nfprop);
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param The name.
	 */
	public IFuture<Void> removeRequiredNFProperty(final IServiceIdentifier sid, final String name)
	{
		return getRequiredServicePropertyProvider(sid).removeNFProperty(name);
	}
	
	/**
	 *  Shutdown the provider.
	 */
	public IFuture<Void> shutdownRequiredNFPropertyProvider(final IServiceIdentifier sid)
	{
		return getRequiredServicePropertyProvider(sid).shutdownNFPropertyProvider();
	}
	
	/**
	 *  Returns meta information about a non-functional properties of all methods.
	 *  @return The meta information about a non-functional properties.
	 */
	public IFuture<Map<MethodInfo, Map<String, INFPropertyMetaInfo>>> getRequiredMethodNFPropertyMetaInfos(final IServiceIdentifier sid)
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFPropertyMetaInfos();
	}
	
	/**
	 *  Returns the names of all non-functional properties of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @return The names of the non-functional properties of the specified method.
	 */
	public IFuture<String[]> getRequiredMethodNFPropertyNames(final IServiceIdentifier sid, final MethodInfo method)
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFPropertyNames(method);
	}
	
	/**
	 *  Returns the names of all non-functional properties of this method.
	 *  This includes the properties of all parent components.
	 *  @return The names of the non-functional properties of this method.
	 */
	public IFuture<String[]> getRequiredMethodNFAllPropertyNames(final IServiceIdentifier sid, final MethodInfo method)
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFAllPropertyNames(method);
	}
	
	/**
	 *  Returns meta information about a non-functional properties of a method.
	 *  @return The meta information about a non-functional properties.
	 */
	public IFuture<Map<String, INFPropertyMetaInfo>> getRequiredMethodNFPropertyMetaInfos(final IServiceIdentifier sid, final MethodInfo method)
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFPropertyMetaInfos(method);
	}
	
	/**
	 *  Returns the meta information about a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of the specified method.
	 */
	public IFuture<INFPropertyMetaInfo> getRequiredMethodNFPropertyMetaInfo(final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFPropertyMetaInfo(method, name);
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
	public <T> IFuture<T> getRequiredMethodNFPropertyValue(final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFPropertyValue(method, name);
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method, performs unit conversion.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
//		public <T, U> IFuture<T> getNFPropertyValue(Method method, String name, Class<U> unit);
	public <T, U> IFuture<T> getRequiredMethodNFPropertyValue(final IServiceIdentifier sid, final MethodInfo method, final String name, final U unit)
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFPropertyValue(method, name, unit);
	}
	
	/**
	 *  Returns the current value of a non-functional property of the specified method, performs unit conversion.
	 *  @param method The method targeted by this operation.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of the specified method.
	 */
	public IFuture<String> getRequiredMethodNFPropertyPrettyPrintValue(IServiceIdentifier sid, MethodInfo method, String name) 
	{
		return getRequiredServicePropertyProvider(sid).getMethodNFPropertyPrettyPrintValue(method, name);
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param nfprop The property.
	 */
	public IFuture<Void> addRequiredMethodNFProperty(final IServiceIdentifier sid, final MethodInfo method, final INFProperty<?, ?> nfprop)
	{
		return getRequiredServicePropertyProvider(sid).addMethodNFProperty(method, nfprop);
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param method The method targeted by this operation.
	 *  @param The name.
	 */
	public IFuture<Void> removeRequiredMethodNFProperty(final IServiceIdentifier sid, final MethodInfo method, final String name)
	{
		return getRequiredServicePropertyProvider(sid).removeMethodNFProperty(method, name);
	}
	
	/**
	 *  Rank the services of a search with a specific ranker.
	 */
	public <S> ITerminableIntermediateFuture<S> rankServices(ITerminableIntermediateFuture<S> searchfut, 
		IServiceRanker<S> ranker, IRankingSearchTerminationDecider<S> decider)
	{
		TerminableIntermediateDelegationFuture<S> ret = new TerminableIntermediateDelegationFuture<S>();
		searchfut.addResultListener(new ServiceRankingDelegationResultListener<S>(ret, searchfut, ranker, decider));
		return ret;
	}
	
	/**
	 *  Rank the services of a search with a specific ranker and emit the scores.
	 */
	public <S> ITerminableIntermediateFuture<Tuple2<S, Double>> rankServicesWithScores(ITerminableIntermediateFuture<S> searchfut, 
		IServiceRanker<S> ranker, IRankingSearchTerminationDecider<S> decider)
	{
		TerminableIntermediateDelegationFuture<Tuple2<S, Double>> ret = new TerminableIntermediateDelegationFuture<Tuple2<S, Double>>();
		searchfut.addResultListener(new ServiceRankingDelegationResultListener2<S>(ret, searchfut, ranker, decider));
		return ret;
	}	
	
	/**
	 *  Counter listener that allows to set the max after usage.
	 */
	public static class LateCounterListener<T> implements IResultListener<T>
	{
		IResultListener<T> delegate;
		int max = -1;
		int cnt = 0;
		
		public LateCounterListener(IResultListener<T> delegate)
		{
			this.delegate = delegate;
		}
		
		public void resultAvailable(T result)
		{
			cnt++;
			check();
		}
		
		public void exceptionOccurred(Exception exception)
		{
			cnt++;
			check();
		}
		
		protected void check()
		{
			if(max>-1 && max==cnt)
			{
				delegate.resultAvailable(null);
			}
		}
		
		public void setMax(int max)
		{
			this.max = max;
			check();
		}
	}
}
