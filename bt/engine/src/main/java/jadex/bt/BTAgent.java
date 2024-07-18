package jadex.bt;

import jadex.core.ComponentIdentifier;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.modelinfo.IModelInfo;

public class BTAgent extends MicroAgent
{
	public static IExternalAccess create(Node pojo)
	{
		return create(pojo, null);
	}
	
	public static IExternalAccess create(Node pojo, ComponentIdentifier cid)
	{
		Component comp = Component.createComponent(BTAgent.class, () -> 
		{
			// this is executed before the features are inited
			return loadModel(pojo.getClass().toString(), pojo, null).thenApply(model ->
			{
				//System.out.println("loaded micro model: "+model);
				
				return new BTAgent(pojo, model, cid);
			}).get();
		});
		
		return comp.getExternalAccess();
	}
	
	public BTAgent(Node pojo, IModelInfo model)
	{
		this(pojo, model, null);
	}
	
	public BTAgent(Node pojo, IModelInfo model, ComponentIdentifier cid)
	{
		super(pojo, model, cid);
	}
}
