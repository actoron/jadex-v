package jadex.bdi.runtime.impl;

import jadex.bdi.model.BDIModel;
import jadex.bdi.model.BDIModelLoader;
import jadex.bdi.runtime.BDICreationInfo;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.modelinfo.IModelInfo;

public class BDIAgent extends MicroAgent
{
	public static BDIModelLoader loader = new BDIModelLoader();
	
	public static IComponentHandle create(Object pojo)
	{
		return create(pojo, null, null);
	}
	
	public static IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		BDIAgent agent = null;
		if(pojo instanceof BDICreationInfo)
		{
			agent = Component.createComponent(BDIAgent.class,
				() -> new BDIAgent((BDICreationInfo)pojo, loadModel(((BDICreationInfo)pojo).getClassname(), null), cid, app));
		}
		else
		{
			agent = Component.createComponent(BDIAgent.class,
				() -> new BDIAgent(pojo, loadModel(pojo.getClass().getName(), pojo), cid, app));
		}
		
		return agent.getComponentHandle();
	}
	
	/** Optional creation info, i.e. arguments. */
	protected BDICreationInfo	info;
	
	protected BDIAgent(BDICreationInfo info, IModelInfo model, ComponentIdentifier cid, Application app)
	{
		this((Object)null, model, cid, app);
		this.info	= info;
	}
	
	protected BDIAgent(Object pojo, IModelInfo model, ComponentIdentifier cid, Application app)
	{
		super(pojo!=null ? pojo : createPojo(model), model, cid, app);
	}
	
	protected static Object	createPojo(IModelInfo model)
	{
		try
		{
			return ((BDIModel)model.getRawModel()).getPojoClass().getType(BDIAgent.class.getClassLoader()).getConstructor().newInstance();
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
	
	/**
	 *  Load a  model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return The loaded model.
	 */
	static public IModelInfo	loadModel(String model, Object pojo)
	{
		try
		{
			BDIModel	cached	= (BDIModel)loader.getCachedModel(model, BDIModelLoader.FILE_EXTENSION_BDI, null, null);
			if(cached!=null)
			{
				return cached.getModelInfo();
			}
			else
			{
				ClassLoader cl = BDIAgent.class.getClassLoader();
				IModelInfo mi = loader.loadComponentModel(model, pojo, null, null, cl, null).getModelInfo();
				return mi;
			}
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
}
