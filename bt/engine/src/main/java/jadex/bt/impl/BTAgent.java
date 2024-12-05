package jadex.bt.impl;

import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.modelinfo.IModelInfo;

public class BTAgent extends MicroAgent
{
	public static IExternalAccess create(Object pojo)
	{
		return create(pojo, null, null);
	}
	
	public static IExternalAccess create(Object pojo, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(BTAgent.class, () -> 
		{
			// this is executed before the features are inited
			return loadModel(pojo.getClass().toString(), pojo, null).thenApply(model ->
			{
				//System.out.println("loaded micro model: "+model);
				
				return new BTAgent(pojo, model, cid, app);
			}).get();
		});
		
		return comp.getExternalAccess();
	}
	
	public BTAgent(Object pojo, IModelInfo model)
	{
		this(pojo, model, null, null);
	}
	
	public BTAgent(Object pojo, IModelInfo model, ComponentIdentifier cid, Application app)
	{
		super(pojo, model, cid, app);
	}
}
