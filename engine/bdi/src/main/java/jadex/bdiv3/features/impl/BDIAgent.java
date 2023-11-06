package jadex.bdiv3.features.impl;

import jadex.bdiv3.BDICreationInfo;
import jadex.bdiv3.BDIModelLoader;
import jadex.bdiv3.model.BDIModel;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.modelinfo.IModelInfo;

public class BDIAgent extends MicroAgent
{
	protected static BDIModelLoader loader = new BDIModelLoader();
	
	public static void create(Object pojo)
	{
		create(pojo, null);
	}
	
	public static void	create(Object pojo, ComponentIdentifier cid)
	{
		String	classname;
		if(pojo instanceof String)
		{
			classname	= (String)pojo;
			if(classname.startsWith("bdi:"))
				classname	= classname.substring(4);
			String	fclassname	= classname ;
			Component.createComponent(BDIAgent.class,
					() -> new BDIAgent((Object)null, loadModel(fclassname), cid));
		}
		else if(pojo instanceof BDICreationInfo)
		{
			classname	= ((BDICreationInfo)pojo).getClassname();
			if(classname.startsWith("bdi:"))
				classname	= classname.substring(4);
			String	fclassname	= classname ;
			Component.createComponent(BDIAgent.class,
					() -> new BDIAgent((BDICreationInfo)pojo, loadModel(fclassname), cid));
		}
		else
		{
			Component.createComponent(BDIAgent.class,
					() -> new BDIAgent(pojo, loadModel(pojo.getClass().getName()), cid));
		}
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
	static public IModelInfo	loadModel(final String model)
	{
		try
		{
			BDIModel	cached	= (BDIModel)loader.getCachedModel(model, BDIModelLoader.FILE_EXTENSION_BDIV3, null, null);
			if(cached!=null)
			{
				return cached.getModelInfo();
			}
			else
			{
				ClassLoader cl = BDIAgent.class.getClassLoader();
				IModelInfo mi = loader.loadComponentModel(model, null, null, null, cl, null).getModelInfo();
				return mi;
			}
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
}
