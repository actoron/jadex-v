package jadex.nfproperty.impl;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.core.IExternalAccess;
import jadex.future.CounterResultListener;
import jadex.future.DelegationResultListener;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.nfproperty.INFProperty;
import jadex.nfproperty.INFPropertyFeature;
import jadex.nfproperty.INFPropertyMetaInfo;
import jadex.nfproperty.INFPropertyProvider;

/**
 *  Base impl for nf property property provider.
 */
public class NFPropertyProvider implements INFPropertyProvider
{
	/** The parent. */
	protected ComponentIdentifier parent;
	
	/** The component. */
	protected IComponent component;
	
	/** Non-functional properties. */
	protected Map<String, INFProperty<?, ?>> nfproperties;
	
	/**
	 *  Create a new provider.
	 */
	public NFPropertyProvider(ComponentIdentifier parent, IComponent component)
	//public NFPropertyProvider(IComponent component)
	{
		this.parent = parent;
		this.component = component;
	}
	
//	/**
//	 *  Create a new provider.
//	 */
//	public NFPropertyProvider(INFPropertyProvider parent)
//	{
//		this.nfparent = parent;
//	}
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getNFPropertyNames()
	{
		return new Future<String[]>(nfproperties != null? nfproperties.keySet().toArray(new String[nfproperties.size()]) : new String[0]);
	}
	
	/**
	 *  Returns the names of all non-functional properties of this service.
	 *  @return The names of the non-functional properties of this service.
	 */
	public IFuture<String[]> getNFAllPropertyNames()
	{
		final Future<String[]> ret = new Future<String[]>();
		final String[] myprops = nfproperties != null? nfproperties.keySet().toArray(new String[nfproperties.size()]) : new String[0];
		
		if(getParentId()!=null)
		{
//			IComponentManagementService cms = getInternalAccess().getFeature(IRequiredServicesFeature.class).getLocalService(new ServiceQuery<>(IComponentManagementService.class));
			IExternalAccess com = getComponent().getExternalAccess(getParentId());
			IFuture<String[]> result = com.scheduleAsyncStep(agent ->
			{
				return agent.getFeature(INFPropertyFeature.class).getNFAllPropertyNames();
			});
			
			result.then(res ->
			{
				Set<String> tmp = new LinkedHashSet<String>();
				for(String p: res)
				{
					tmp.add(p);
				}
				for(String p: myprops)
				{
					tmp.add(p);
				}
				ret.setResult((String[])tmp.toArray(new String[tmp.size()]));
			}).catchEx(ret);
		}
		else
		{
			ret.setResult(myprops);
		}
		
//		getParent().addResultListener(new ExceptionDelegationResultListener<INFPropertyProvider, String[]>(ret)
//		{
//			public void customResultAvailable(INFPropertyProvider parent)
//			{
//				if(parent!=null)
//				{
//					parent.getNFAllPropertyNames().addResultListener(new DelegationResultListener<String[]>(ret)
//					{
//						public void customResultAvailable(String[] result)
//						{
//							Set<String> tmp = new LinkedHashSet<String>();
//							for(String p: result)
//							{
//								tmp.add(p);
//							}
//							for(String p: myprops)
//							{
//								tmp.add(p);
//							}
//							ret.setResult((String[])tmp.toArray(new String[tmp.size()]));
//						}
//					});
//				}
//				else
//				{
//					ret.setResult(myprops);
//				}
//			}
//		});
			
		return ret;
	}
	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<Map<String, INFPropertyMetaInfo>> getNFPropertyMetaInfos()
	{
		Future<Map<String, INFPropertyMetaInfo>> ret = new Future<Map<String,INFPropertyMetaInfo>>();
		
		Map<String, INFPropertyMetaInfo> res = new HashMap<String, INFPropertyMetaInfo>();
		if(nfproperties!=null)
		{
			for(String key: nfproperties.keySet())
			{
				res.put(key, nfproperties.get(key).getMetaInfo());
			}
		}
		ret.setResult(res);
		
		return ret;
	}

	
	/**
	 *  Returns the meta information about a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @return The meta information about a non-functional property of this service.
	 */
	public IFuture<INFPropertyMetaInfo> getNFPropertyMetaInfo(final String name)
	{
		final Future<INFPropertyMetaInfo> ret = new Future<INFPropertyMetaInfo>();
		
		INFPropertyMetaInfo mi = nfproperties != null? nfproperties.get(name) != null? nfproperties.get(name).getMetaInfo() : null : null;
		
		if(mi != null)
		{
			ret.setResult(mi);
		}
		else 
		{
			if(getParentId()!=null)
			{
				IExternalAccess com = getComponent().getExternalAccess(getParentId());
				IFuture<INFPropertyMetaInfo> result = com.scheduleAsyncStep(agent ->
				{
					return agent.getFeature(INFPropertyFeature.class).getNFPropertyMetaInfo(name);
				});
				result.delegateTo(ret);
			}
			else
			{
				ret.setException(new RuntimeException("Property not found: "+name));
			}
		}
		
		return ret;
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public <T> IFuture<T> getNFPropertyValue(final String name)
	{
		final Future<T> ret = new Future<T>();
		
		INFProperty<T, ?> prop = (INFProperty<T, ?>) (nfproperties != null? nfproperties.get(name) : null);
		
		if(prop != null)
		{
			try
			{
				prop.getValue().addResultListener(new DelegationResultListener<T>(ret));
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		else 
		{
			if(getParentId()!=null)
			{
				IExternalAccess com = getComponent().getExternalAccess(getParentId());
				IFuture<T> result = com.scheduleAsyncStep(agent ->
				{
					IFuture<T> res = agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(name);
					return res;
				});
				result.delegateTo(ret);
			}
			else
			{
				ret.setException(new RuntimeException("Property not found: "+name));
			}
		}	
		
		return ret;
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service, performs unit conversion.
	 *  @param name Name of the property.
	 *  @param unit Unit of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
//	public<T, U> IFuture<T> getNFPropertyValue(String name, Class<U> unit)
	public<T, U> IFuture<T> getNFPropertyValue(final String name, final U unit)
	{
		final Future<T> ret = new Future<T>();
		
		INFProperty<T, U> prop = (INFProperty<T, U>) (nfproperties != null? nfproperties.get(name) : null);
		
		if(prop!=null)
		{
			try
			{
				prop.getValue(unit).addResultListener(new DelegationResultListener<T>(ret));
			}
			catch (Exception e)
			{
				ret.setException(e);
		//		ret.setException(new ClassCastException("Requested value type (" + String.valueOf(type) + ") does not match value type (" + String.valueOf(reto.getClass()) + ") for this non-functional property: " + name));
			}
		}
		else 
		{
			if(getParentId()!=null)
			{
				IExternalAccess com = getComponent().getExternalAccess(getParentId());
				IFuture<T> result = com.scheduleAsyncStep(agent ->
				{
					IFuture<T> res = agent.getFeature(INFPropertyFeature.class).getNFPropertyValue(name, unit);
					return res;
				});
				result.delegateTo(ret);
			}
			else
			{
				ret.setException(new RuntimeException("Property not found: "+name));
			}
		}	
		
		return ret;
	}
	
	/**
	 *  Returns the current value of a non-functional property of this service.
	 *  @param name Name of the property.
	 *  @param type Type of the property value.
	 *  @return The current value of a non-functional property of this service.
	 */
	public IFuture<String> getNFPropertyPrettyPrintValue(String name) 
	{
		final Future<String> ret = new Future<String>();
		
		INFProperty<?, ?> prop = (INFProperty<?, ?>) (nfproperties != null? nfproperties.get(name) : null);
		
		if(prop != null)
		{
			try
			{
				prop.getPrettyPrintValue().addResultListener(new DelegationResultListener<String>(ret));
			}
			catch(Exception e)
			{
				ret.setException(e);
			}
		}
		else 
		{
			if(getParentId()!=null)
			{
				IExternalAccess com = getComponent().getExternalAccess(getParentId());
				IFuture<String> result = com.scheduleAsyncStep(agent ->
				{
					IFuture<String> res = agent.getFeature(INFPropertyFeature.class).getNFPropertyPrettyPrintValue(name);
					return res;
				});
				result.delegateTo(ret);
			}
			else
			{
				ret.setException(new RuntimeException("Property not found: "+name));
			}
		}	
		
		return ret;
	}
	
	/**
	 *  Add a non-functional property.
	 *  @param metainfo The metainfo.
	 */
	public IFuture<Void> addNFProperty(INFProperty<?, ?> nfprop)
	{
		final Future<Void> ret = new Future<Void>();
		if(nfproperties==null)
			nfproperties = new HashMap<String, INFProperty<?,?>>();
		nfproperties.put(nfprop.getName(), nfprop);
		
		/*if(getComponent().getFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOALL, PublishEventLevel.COARSE))
		{
			MonitoringEvent me = new MonitoringEvent(getComponent().getId(), getComponent().getDescription().getCreationTime(), 
				MonitoringEvent.TYPE_PROPERTY_ADDED, System.currentTimeMillis(), PublishEventLevel.COARSE);
			me.setProperty("propname", nfprop.getName());
			getComponent().getFeature(IMonitoringComponentFeature.class).publishEvent(me, PublishTarget.TOALL).addResultListener(new DelegationResultListener<Void>(ret));
		}
		else*/
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	/**
	 *  Remove a non-functional property.
	 *  @param The name.
	 */
	public IFuture<Void> removeNFProperty(final String name)
	{
		final Future<Void> ret = new Future<Void>();
		if(nfproperties!=null)
		{
			INFProperty<?, ?> prop = nfproperties.remove(name);
			if(prop!=null)
			{
				prop.dispose().addResultListener(new DelegationResultListener<Void>(ret)
				{
					public void customResultAvailable(Void result)
					{
						/*if(getComponent().getFeature(IMonitoringComponentFeature.class).hasEventTargets(PublishTarget.TOALL, PublishEventLevel.COARSE))
						{
							MonitoringEvent me = new MonitoringEvent(getComponent().getId(), getComponent().getDescription().getCreationTime(), 
								MonitoringEvent.TYPE_PROPERTY_REMOVED, System.currentTimeMillis(), PublishEventLevel.COARSE);
							me.setProperty("propname", name);
							getComponent().getFeature(IMonitoringComponentFeature.class).publishEvent(me, PublishTarget.TOALL).addResultListener(new DelegationResultListener<Void>(ret));
						}
						else*/
						{
							ret.setResult(null);
						}
					}
				});
			}
			else
			{
				ret.setResult(null);
			}
		}
		else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	public ComponentIdentifier getParentId()
	{
		return parent;
	}
	
	/**
	 *  Shutdown the provider.
	 */
	public IFuture<Void> shutdownNFPropertyProvider()
	{
		Future<Void> ret = new Future<Void>();
		if(nfproperties!=null)
		{
			CounterResultListener<Void> lis = new CounterResultListener<Void>(nfproperties.size(), true, new DelegationResultListener<Void>(ret));
			for(INFProperty<?, ?> prop: nfproperties.values())
			{
				prop.dispose().addResultListener(lis);
			}
		}
		else
		{
			ret.setResult(null);
		}
		return ret;
	}
	
	/**
	 *  Get the internal access.
	 */
	public IComponent getComponent()
	{
		return component;
	}

	
}
