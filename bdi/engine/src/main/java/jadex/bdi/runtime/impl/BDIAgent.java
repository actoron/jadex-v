package jadex.bdi.runtime.impl;

import jadex.bdi.model.BDIModel;
import jadex.bdi.model.BDIModelLoader;
import jadex.bdi.runtime.BDICreationInfo;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.modelinfo.IModelInfo;

public class BDIAgent extends MicroAgent
{
	public static BDIModelLoader loader = new BDIModelLoader();

	public static IExternalAccess create(Object pojo)
	{
		return create(pojo, null);
	}

	public static IExternalAccess create(Object pojo, ComponentIdentifier cid)
	{
		String	classname;
		BDIAgent agent = null;
		if(pojo instanceof String)
		{
			classname	= (String)pojo;
			if(classname.startsWith("bdi:"))
				classname	= classname.substring(4);
			String	fclassname	= classname ;
			agent = Component.createComponent(BDIAgent.class,
					() -> new BDIAgent((Object)null, loadModel(fclassname, null), cid));
		}
		else if(pojo instanceof BDICreationInfo)
		{
			classname	= ((BDICreationInfo)pojo).getClassname();
			if(classname.startsWith("bdi:"))
				classname	= classname.substring(4);
			String	fclassname	= classname ;
			agent = Component.createComponent(BDIAgent.class,
					() -> new BDIAgent((BDICreationInfo)pojo, loadModel(fclassname, null), cid));
		}
		else
		{
			agent = Component.createComponent(BDIAgent.class,
					() -> new BDIAgent(pojo, loadModel(pojo.getClass().getName(), pojo), cid));
		}

		return agent.getExternalAccess();
	}

	/** Optional creation info, i.e. arguments. */
	protected BDICreationInfo	info;

	protected BDIAgent(BDICreationInfo info, IModelInfo model, ComponentIdentifier cid)
	{
		this((Object)null, model, cid);
		this.info	= info;
	}

	protected BDIAgent(Object pojo, IModelInfo model, ComponentIdentifier cid)
	{
		super(pojo!=null ? pojo : createPojo(model), model, cid);
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
