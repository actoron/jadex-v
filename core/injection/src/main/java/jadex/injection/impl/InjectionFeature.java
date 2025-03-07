package jadex.injection.impl;

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
	
	/**
	 *  Create the injection feature.
	 */
	public InjectionFeature(IComponent self)
	{
		this.self	= self;
		this.model	= InjectionModel.get(self.getPojo().getClass());
	}
	
	//-------- lifecycle methods --------
	
	@Override
	public void onStart()
	{
//		System.out.println("started: "+this+", "+IComponentManager.get().getCurrentComponent());
		model.getOnStart().accept(self);
	}

	@Override
	public void onEnd()
	{
//		System.out.println("stopped: "+this+", "+IComponentManager.get().getCurrentComponent());
		model.getOnEnd().accept(self);
	}
}
