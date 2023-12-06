package jadex.bpmn.runtime;

import jadex.bpmn.BpmnModelLoader;
import jadex.bpmn.model.MBpmnModel;
import jadex.common.SUtil;
import jadex.core.ComponentIdentifier;
import jadex.core.IExternalAccess;
import jadex.core.impl.Component;
import jadex.model.IModelFeature;
import jadex.model.impl.IInternalModelFeature;
import jadex.model.modelinfo.IModelInfo;

public class BpmnProcess extends Component
{
	protected static BpmnModelLoader loader = new BpmnModelLoader();

	public static IExternalAccess create(Object pojo)
	{
		return create(pojo, null);
	}
	
	public static IExternalAccess create(Object pojo, ComponentIdentifier cid)
	{
		Component comp = null;
		String	filename;
		if(pojo instanceof String)
		{
			filename	= (String)pojo;
			if(filename.startsWith("bpmn:"))
				filename	= filename.substring(5);
			String	ffilename	= filename ;
			comp = Component.createComponent(BpmnProcess.class,
				() -> new BpmnProcess((Object)null, loadModel(ffilename), cid));
		}
		else if(pojo instanceof RBpmnProcess)
		{
			filename = ((RBpmnProcess)pojo).getFilename();
			if(filename.startsWith("bpmn:"))
				filename = filename.substring(5);
			String	ffilename	= filename ;
			comp = Component.createComponent(BpmnProcess.class,
				() -> new BpmnProcess((RBpmnProcess)pojo, loadModel(ffilename), cid));
		}
		
		return comp.getExternalAccess();
	}
	
	protected RBpmnProcess pojo;
	
	protected BpmnProcess(RBpmnProcess info, IModelInfo model, ComponentIdentifier cid)
	{
		this((Object)info, model, cid);
	}
	
	protected BpmnProcess(Object pojo, IModelInfo model, ComponentIdentifier cid)
	{
		super(cid);
		((IInternalModelFeature)this.getFeature(IModelFeature.class)).setModel(model);
		this.pojo = (RBpmnProcess)(pojo!=null ? pojo : createPojo(model));
	}
	
	public RBpmnProcess getPojo() 
	{
		return pojo;
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
