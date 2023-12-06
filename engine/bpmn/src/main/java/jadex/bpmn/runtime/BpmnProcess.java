package jadex.bpmn.runtime;

import jadex.bpmn.BpmnModelLoader;
import jadex.bpmn.model.MBpmnModel;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.impl.Component;
import jadex.micro.MicroAgent;
import jadex.model.modelinfo.IModelInfo;

public class BpmnProcess extends MicroAgent
{
	protected static BpmnModelLoader loader = new BpmnModelLoader();
	
	public static void create(Object pojo)
	{
		create(pojo, null);
	}
	
	public static void	create(Object pojo, ComponentIdentifier cid)
	{
		String	filename;
		if(pojo instanceof String)
		{
			filename	= (String)pojo;
			if(filename.startsWith("bpmn:"))
				filename	= filename.substring(5);
			String	ffilename	= filename ;
			Component.createComponent(BpmnProcess.class,
					() -> new BpmnProcess((Object)null, loadModel(ffilename), cid));
		}
		else if(pojo instanceof RBpmnProcess)
		{
			filename = ((RBpmnProcess)pojo).getFilename();
			if(filename.startsWith("bpmn:"))
				filename = filename.substring(5);
			String	ffilename	= filename ;
			Component.createComponent(BpmnProcess.class,
				() -> new BpmnProcess((RBpmnProcess)pojo, loadModel(ffilename), cid));
		}
	}
	
	protected BpmnProcess(RBpmnProcess info, IModelInfo model, ComponentIdentifier cid)
	{
		this((Object)info, model, cid);
	}
	
	protected BpmnProcess(Object pojo, IModelInfo model, ComponentIdentifier cid)
	{
		super(pojo!=null ? pojo : createPojo(model), model, cid);
	}
	
	public RBpmnProcess getProcessPojo() 
	{
		return (RBpmnProcess)getPojo();
	}
	
	protected static Object	createPojo(IModelInfo model)
	{
		try
		{
			return new RBpmnProcess().setFilename(model.getFilename());
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
	static public IModelInfo loadModel(final String model)
	{
		try
		{
			MBpmnModel cached = (MBpmnModel)loader.getCachedModel(model, BpmnModelLoader.FILE_EXTENSION_BPMN2, null, null);
			if(cached!=null)
			{
				return cached.getModelInfo();
			}
			else
			{
				ClassLoader cl = BpmnProcess.class.getClassLoader();
				//public MBpmnModel loadBpmnModel(String name, String[] imports, ClassLoader classloader, Object context) throws Exception
				IModelInfo mi = loader.loadBpmnModel(model, null, cl, null).getModelInfo();
				return mi;
			}
		}
		catch(Exception e)
		{
			throw SUtil.throwUnchecked(e);
		}
	}
}
