package jadex.provided2.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import jadex.core.IComponent;
import jadex.injection.IInjectionFeature;
import jadex.injection.impl.InjectionFeature;
import jadex.provided2.IProvided2Feature;

/**
 *  Component feature that handles detection and registration of provided services.
 */
public class Provided2Feature implements IProvided2Feature
{
	/** The component. */
	protected IComponent	self;
	
	protected Set<Object>	services;
	
	/**
	 *  Create the injection feature.
	 */
	public Provided2Feature(IComponent self)
	{
		this.self	= self;
	}

	/**
	 *  Register a service.
	 */
	protected void	addService(Set<Class<?>> interfaces,  Object pojo)
	{
		if(services==null)
		{
			services	= new LinkedHashSet<>();
		}
		
		// May be added already due to first field service found and then service interface found again as extra object.
		if(!services.contains(pojo))
		{
			services.add(pojo);
			System.out.println("add service: "+pojo+" "+interfaces);
			if(pojo!=self.getPojo())
			{
				((InjectionFeature)self.getFeature(IInjectionFeature.class)).addExtraObject(pojo);
			}
		}
	}
}
