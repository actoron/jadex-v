package jadex.injection.impl;

import java.util.ArrayList;
import java.util.List;

import jadex.core.IComponent;
import jadex.execution.impl.ILifecycle;
import jadex.injection.IInjectionFeature;

/**
 *  Component feature allowing e.g. @OnStart methods and field injections.
 */
public class InjectionFeature implements IInjectionFeature, ILifecycle
{
	/** The component. */
	protected IComponent	self;
	
	/** The model caching all invocation stuff. */
	protected InjectionModel	model;
	
	/** Managed extra objects (e.g. services implemented as sepoarate class). */
	protected List<Object>	extras;
	
	/**
	 *  Create the injection feature.
	 */
	public InjectionFeature(IComponent self)
	{
		this.self	= self;
		this.model	= InjectionModel.get(self.getPojo()!=null ? self.getPojo().getClass() : Object.class);
	}
	
	//-------- lifecycle methods --------
	
	@Override
	public void onStart()
	{
		model.getFieldInjections().handleInjection(self, self.getPojo(), null);
		model.getExtraOnStart().handleInjection(self, self.getPojo(), null);
		model.getOnStart().handleInjection(self, self.getPojo(), null);		
		model.getMethodInjections().handleInjection(self, self.getPojo(), null);
	}

	@Override
	public void onEnd()
	{
		if(extras!=null)
		{
			for(Object pojo: extras)
			{
				InjectionModel	model	= InjectionModel.get(pojo.getClass());
				model.getOnEnd().handleInjection(self, pojo, null);				
			}
		}
		
		model.getOnEnd().handleInjection(self, self.getPojo(), null);
	}
	
	//-------- internal methods (to be used by other features) --------
	
	/**
	 *  Add an extra pojo to be managed.
	 *  E.g. inject fields, call OnStart/End methods.
	 */
	public void	addExtraObject(Object pojo)
	{
		if(extras==null)
		{
			extras	= new ArrayList<Object>(4);
		}
		extras.add(pojo);
		
		InjectionModel	model	= InjectionModel.get(pojo.getClass());
		model.getFieldInjections().handleInjection(self, pojo, null);
		model.getExtraOnStart().handleInjection(self, pojo, null);
		model.getOnStart().handleInjection(self, pojo, null);
		model.getMethodInjections().handleInjection(self, pojo, null);
	}
}
