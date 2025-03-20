package jadex.injection.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadex.core.IComponent;
import jadex.execution.IExecutionFeature;
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
	
	/** Managed extra objects (e.g. services implemented as separate class). */
	protected List<List<Object>>	extras;
	
	/**
	 *  Create the injection feature.
	 */
	public InjectionFeature(IComponent self)
	{
		this.self	= self;
		this.model	= InjectionModel.get(Collections.singletonList(self.getPojo()));
	}
	
	//-------- lifecycle methods --------
	
	@Override
	public void onStart()
	{
		if(model.getFieldInjections()!=null)
		{
			model.getFieldInjections().handleInjection(self, Collections.singletonList(self.getPojo()), null);
		}

		if(model.getExtraOnStart()!=null)
		{
			model.getExtraOnStart().handleInjection(self, Collections.singletonList(self.getPojo()), null);
		}

		if(model.getOnStart()!=null)
		{
			self.getFeature(IExecutionFeature.class).scheduleStep(()->
				model.getOnStart().handleInjection(self, Collections.singletonList(self.getPojo()), null));
		}
		
		if(model.getMethodInjections()!=null)
		{
			self.getFeature(IExecutionFeature.class).scheduleStep(()->
				model.getMethodInjections().handleInjection(self, Collections.singletonList(self.getPojo()), null));
		}
	}

	@Override
	public void onEnd()
	{
		if(extras!=null)
		{
			for(List<Object> pojos: extras)
			{
				InjectionModel	model	= InjectionModel.get(pojos);
				if(model.getOnEnd()!=null)
				{	
					model.getOnEnd().handleInjection(self, pojos, null);
				}
			}
		}
		
		if(model.getOnEnd()!=null)
		{	
			model.getOnEnd().handleInjection(self, Collections.singletonList(self.getPojo()), null);
		}
	}
	
	//-------- internal methods (to be used by other features) --------
	
	/**
	 *  Add an extra pojo to be managed.
	 *  E.g. inject fields in service impl, call OnStart/End methods.
	 *  
	 *  @param pojos	The actual pojo objects as a hierachy of component pojo plus subobjects.
	 *  				The injection is for the last pojo in the list.
	 */
	public void	addExtraObject(List<Object> pojos)
	{
		if(extras==null)
		{
			extras	= new ArrayList<List<Object>>(4);
		}
		extras.add(pojos);
		
		InjectionModel	model	= InjectionModel.get(pojos);

		if(model.getFieldInjections()!=null)
		{
			model.getFieldInjections().handleInjection(self, pojos, null);
		}

		if(model.getExtraOnStart()!=null)
		{
			model.getExtraOnStart().handleInjection(self, pojos, null);
		}

		if(model.getOnStart()!=null)
		{
			self.getFeature(IExecutionFeature.class).scheduleStep(()->
				model.getOnStart().handleInjection(self, pojos, null));
		}
		
		if(model.getMethodInjections()!=null)
		{
			self.getFeature(IExecutionFeature.class).scheduleStep(()->
				model.getMethodInjections().handleInjection(self, pojos, null));
		}
	}
}
