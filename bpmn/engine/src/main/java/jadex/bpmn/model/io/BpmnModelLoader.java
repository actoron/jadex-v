package jadex.bpmn.model.io;

import jadex.bpmn.model.MBpmnModel;
import jadex.bpmn.runtime.BpmnProcess;
import jadex.common.ResourceInfo;
import jadex.model.ICacheableModel;
import jadex.model.impl.AbstractModelLoader;
import jadex.model.modelinfo.ModelInfo;

/**
 *  Loader for eclipse STP BPMN models (.bpmn files).
 */
public class BpmnModelLoader extends AbstractModelLoader
{
	//-------- constants --------
	
	/** The BPMN file extension. */
	public static final String	FILE_EXTENSION_BPMN	= ".bpmn";
	
	/** The BPMN 2 file extension. */
	public static final String	FILE_EXTENSION_BPMN2	= ".bpmn2";
	
	//-------- constructors --------
	
	/**
	 *  Create a new BPMN model loader.
	 */
	public BpmnModelLoader()
	{
		super(new String[]{FILE_EXTENSION_BPMN, FILE_EXTENSION_BPMN2});
		
		AbstractModelLoader.addLoader(BpmnProcess.class, this);
	}

	//-------- methods --------
	
	
	/**
	 *  Load a BPMN model.
	 *  @param name	The filename or logical name (resolved via imports and extensions).
	 *  @param imports	The imports, if any.
	 */
	public MBpmnModel loadBpmnModel(String name, String[] imports, ClassLoader classloader, Object context) throws Exception
	{
		String ext = FILE_EXTENSION_BPMN;
		if(name.endsWith(".bpmn2"))
			ext = FILE_EXTENSION_BPMN2;
		MBpmnModel	ret	= (MBpmnModel)loadModel(name, ext, imports, classloader, classloader, context);	// Todo RID as clkey
		//ret.setClassLoader(classloader);
		return ret;
	}
	
	//-------- AbstractModelLoader methods --------
		
	/**
	 *  Load a model.
	 *  @param name	The original name (i.e. not filename).
	 *  @param info	The resource info.
	 */
	protected ICacheableModel doLoadModel(String name, Object pojo, String[] imports, 
		ClassLoader classloader, ResourceInfo info
		//Object context
		) throws Exception
	{	
//		if (name != null && name.endsWith(".bpmn2"))
//		{
			MBpmnModel model = SBpmnModelReader.readModel(info.getInputStream(), info.getFilename(), null, classloader);
			//IResourceIdentifier rid = (IResourceIdentifier)((Object[])context)[0];
			/*if(rid==null)
			{
				String src = SUtil.getCodeSource(info.getFilename(), ((ModelInfo)model.getModelInfo()).getPackage());
				URL url = SUtil.toURL(src);
				rid = new ResourceIdentifier(new LocalResourceIdentifier((IComponentIdentifier)((Object[])context)[1], url), null);
			}
			model.setResourceIdentifier(rid);*/
			model.initModelInfo(classloader);
			((ModelInfo)model.getModelInfo()).setType("bpmn2");
			return model;
//		}
//		return (ICacheableModel)BpmnXMLReader.read(info, classloader, (IResourceIdentifier)((Object[])context)[0],
//			(IComponentIdentifier)((Object[])context)[1]);
	}
}
