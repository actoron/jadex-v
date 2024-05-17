package jadex.nfproperty.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.common.MethodInfo;
import jadex.nfproperty.impl.modelinfo.NFPropertyInfo;

public class NFPropertyModel 
{
	protected List<NFPropertyInfo> compprops;
	
	protected Map<String, List<NFPropertyInfo>> provprops;
	protected Map<String, Map<MethodInfo, List<NFPropertyInfo>>> provmprops;

	protected Map<String, List<NFPropertyInfo>> reqprops;
	protected Map<String, Map<MethodInfo, List<NFPropertyInfo>>> reqmprops;

	
	public NFPropertyModel()
	{
	}
	
	public void addComponentProperty(NFPropertyInfo info)
	{
		if(compprops==null)
			compprops = new ArrayList<NFPropertyInfo>();
		
		if(compprops.removeIf(prop -> info.getClazz().equals(prop.getClazz())))
			System.out.println("overridden prop: "+info.getClazz());
		
		compprops.add(info);
	}
	
	public void removeComponentProperty(String name)
	{
		if(compprops!=null)
			compprops.removeIf(prop -> name.equals(prop.getName()));
	}
	
	public List<NFPropertyInfo> getComponentProperties()
	{
		return compprops!=null? new ArrayList<NFPropertyInfo>(compprops): Collections.EMPTY_LIST;
	}
	
	
	
	public void addProvidedServiceProperty(String sername, NFPropertyInfo info)
	{
		if(provprops==null)
			provprops = new HashMap<String, List<NFPropertyInfo>>();
		
		List<NFPropertyInfo> props = provprops.get(sername);
		if(props==null)
		{
			props = new ArrayList<NFPropertyInfo>();
			provprops.put(sername, props);
		}
		
		if(props.removeIf(prop -> info.getClazz().equals(prop.getClazz())))
			System.out.println("overridden prop: "+info.getClazz());

		props.add(info);
	}
	
	public void removeProvidedServiceProperty(String sername, String name)
	{
		if(provprops!=null)
		{
			List<NFPropertyInfo> props = provprops.get(sername);
			if(props!=null)
				props.removeIf(prop -> name.equals(prop.getName()));
		}
	}
	
	public List<NFPropertyInfo> getProvidedServiceProperties(String sername)
	{
		return provprops!=null? provprops.get(sername): null;
	}
	
	public Set<String> getProvidedServiceNames()
	{
		return provprops!=null? new HashSet<String>(provprops.keySet()): Collections.EMPTY_SET;
	}
	
	
	
	public void addProvidedServiceMethodProperty(String sername, MethodInfo mi, NFPropertyInfo info)
	{
		if(provmprops==null)
			provmprops = new HashMap<String, Map<MethodInfo, List<NFPropertyInfo>>>();
		
		Map<MethodInfo, List<NFPropertyInfo>> props = provmprops.get(sername);
		if(props==null)
		{
			props = new HashMap<MethodInfo, List<NFPropertyInfo>>();
			provmprops.put(sername, props);
		}
		
		List<NFPropertyInfo> props2 = props.get(mi);
		if(props2==null)
		{
			props2 = new ArrayList<NFPropertyInfo>();
			props.put(mi, props2);
		}
		
		if(props2.removeIf(prop -> info.getClazz().equals(prop.getClazz())))
			System.out.println("overridden prop: "+info.getClazz());

		props2.add(info);
	}
	
	public void removeProvidedServiceMethodProperty(String sername, MethodInfo mi, String name)
	{
		if(provmprops!=null)
		{
			Map<MethodInfo, List<NFPropertyInfo>> props = provmprops.get(sername);
			if(props!=null)
			{
				List<NFPropertyInfo> props2 = props.get(mi);
				props2.removeIf(prop -> name.equals(prop.getName()));
			}
		}
	}
	
	/*public List<NFPropertyInfo> getProvidedServiceMethodProperties(String sername, MethodInfo mi)
	{
		List<NFPropertyInfo> ret = Collections.EMPTY_LIST;
		if(provmprops!=null)
		{
			Map<MethodInfo, List<NFPropertyInfo>> props = provmprops.get(sername);
			if(props!=null)
				ret = props.get(mi);
		}
		return ret;
	}*/
	
	public Map<MethodInfo, List<NFPropertyInfo>> getProvidedServiceMethodProperties(String sername)
	{
		Map<MethodInfo, List<NFPropertyInfo>> ret = Collections.EMPTY_MAP;
		if(provmprops!=null)
			ret = provmprops.get(sername);
		return ret;
	}
	
	public Set<String> getProvidedServiceMethodNames()
	{
		return provmprops!=null? new HashSet<String>(provmprops.keySet()): Collections.EMPTY_SET;
	}
	
	
	
	
	public void addRequiredServiceProperty(String sername, NFPropertyInfo info)
	{
		if(reqprops==null)
			reqprops = new HashMap<String, List<NFPropertyInfo>>();
		
		List<NFPropertyInfo> props = reqprops.get(sername);
		if(props==null)
		{
			props = new ArrayList<NFPropertyInfo>();
			reqprops.put(sername, props);
		}
		
		if(props.removeIf(prop -> info.getClazz().equals(prop.getClazz())))
			System.out.println("overridden prop: "+info.getClazz());

		props.add(info);
	}
	
	public void removeRequiredServiceProperty(String sername, String name)
	{
		if(reqprops!=null)
		{
			List<NFPropertyInfo> props = reqprops.get(sername);
			if(props!=null)
				props.removeIf(prop -> name.equals(prop.getName()));
		}
	}
	
	public List<NFPropertyInfo> getRequiredServiceProperties(String sername)
	{
		return reqprops!=null? reqprops.get(sername): null;
	}
	
	public Set<String> getRequiredServiceNames()
	{
		return reqprops!=null? new HashSet<String>(reqprops.keySet()): Collections.EMPTY_SET;
	}
	
	
	
	public void addRequiredServiceMethodProperty(String sername, MethodInfo mi, NFPropertyInfo info)
	{
		if(reqmprops==null)
			reqmprops = new HashMap<String, Map<MethodInfo, List<NFPropertyInfo>>>();
		
		Map<MethodInfo, List<NFPropertyInfo>> props = reqmprops.get(sername);
		if(props==null)
		{
			props = new HashMap<MethodInfo, List<NFPropertyInfo>>();
			reqmprops.put(sername, props);
		}
		
		List<NFPropertyInfo> props2 = props.get(mi);
		if(props2==null)
		{
			props2 = new ArrayList<NFPropertyInfo>();
			props.put(mi, props2);
		}
		
		if(props2.removeIf(prop -> info.getClazz().equals(prop.getClazz())))
			System.out.println("overridden prop: "+info.getClazz());

		props2.add(info);
	}
	
	public void removeRequiredServiceMethodProperty(String sername, MethodInfo mi, String name)
	{
		if(reqmprops!=null)
		{
			Map<MethodInfo, List<NFPropertyInfo>> props = reqmprops.get(sername);
			if(props!=null)
			{
				List<NFPropertyInfo> props2 = props.get(mi);
				props2.removeIf(prop -> name.equals(prop.getName()));
			}
		}
	}
	
	public List<NFPropertyInfo> getRequiredServiceMethodProperties(String sername, MethodInfo mi)
	{
		List<NFPropertyInfo> ret = Collections.EMPTY_LIST;
		if(reqmprops!=null)
		{
			Map<MethodInfo, List<NFPropertyInfo>> props = reqmprops.get(sername);
			if(props!=null)
				ret = props.get(mi);
		}
		return ret;
	}
	
	public Set<String> getRequiredServiceMethodNames()
	{
		return reqmprops!=null? new HashSet<String>(reqmprops.keySet()): Collections.EMPTY_SET;
	}
}

