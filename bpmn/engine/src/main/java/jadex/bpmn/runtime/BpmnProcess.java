package jadex.bpmn.runtime;

import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.model.io.BpmnModelLoader;
import jadex.bpmn.runtime.impl.BpmnValueProvider;
import jadex.common.SUtil;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ValueProvider;
import jadex.future.IFuture;
import jadex.model.modelinfo.IModelInfo;

public class BpmnProcess extends Component
{
	protected static final BpmnModelLoader loader = new BpmnModelLoader();

	public static IFuture<IComponentHandle> create(Object pojo)
	{
		return create(pojo, null, null);
	}
	
	public static IFuture<IComponentHandle> create(Object pojo, ComponentIdentifier cid, Application app)
	{
		return Component.createComponent(new BpmnProcess((RBpmnProcess)pojo, cid, app));
	}
	
	protected BpmnProcess(RBpmnProcess info, ComponentIdentifier cid, Application app)
	{
		super(info, cid, app);
	}
	
	public RBpmnProcess getPojo() 
	{
		return (RBpmnProcess)super.getPojo();
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
	
	@Override
	public ValueProvider getValueProvider() 
	{
//		if(valueprovider==null)
//			valueprovider = new BpmnValueProvider(this);
//		return valueprovider;
		return new BpmnValueProvider(this);
	}
}
