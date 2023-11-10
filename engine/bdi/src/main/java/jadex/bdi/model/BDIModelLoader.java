package jadex.bdi.model;

import jadex.common.ResourceInfo;
import jadex.model.AbstractModelLoader;
import jadex.model.ICacheableModel;

/**
 * 
 */
public class BDIModelLoader extends AbstractModelLoader
{
	//-------- constants --------
	
	/** The component file extension. */
//	public static final String	FILE_EXTENSION_BDIV3_FIRST = "BDI";
//	public static final String	FILE_EXTENSION_BDIV3_SECOND = ".class";
//	public static final String	FILE_EXTENSION_BDIV3 = FILE_EXTENSION_BDIV3_FIRST + FILE_EXTENSION_BDIV3_SECOND;
	public static final String	FILE_EXTENSION_BDI =  ".class";
	
	//-------- attributes --------
	
	/** The xml reader. */
	protected BDIClassReader reader;
	
	//-------- constructors --------
	
	/**
	 *  Create a new BPMN model loader.
	 */
	public BDIModelLoader()
	{
		super(new String[]{FILE_EXTENSION_BDI});
		this.reader = BDIClassGeneratorFactory.getInstance().createBDIClassReader(this);
	}

	/**
	 *  Set the generator.
	 * 	@param gen the gen to set
	 */
	public void setGenerator(IBDIClassGenerator gen)
	{
		reader.setGenerator(gen);
	}
	
	//-------- methods --------
	
	/**
	 *  Load a component model.
	 *  @param name	The filename or logical name (resolved via imports and extensions).
	 *  @param imports	The imports, if any.
	 */
	public BDIModel loadComponentModel(String name, Object pojo, String[] imports, Object clkey, ClassLoader classloader, Object context) throws Exception
	{
		return (BDIModel)loadModel(name, pojo, FILE_EXTENSION_BDI, imports, clkey, classloader);
	}
	
	//-------- AbstractModelLoader methods --------
		
	/**
	 *  Load a model.
	 *  @param name	The original name (i.e. not filename).
	 *  @param info	The resource info.
	 */
	protected ICacheableModel doLoadModel(String name, Object pojo, String[] imports, ClassLoader classloader) throws Exception
	{
//		System.out.println("cache miss: "+name);
		return (ICacheableModel)reader.read(name, pojo, imports, classloader);
	}
	
	//-------- constructors --------
	
	/**
	 *  Find the file for a given name.
	 *  @param name	The filename or logical name (resolved via imports and extension).
	 *  @param extension	The required extension.
	 *  @param imports	The imports, if any.
	 *  @return The resource info identifying the file.
	 */
	protected ResourceInfo	getResourceInfo(String name, String extension, String[] imports, ClassLoader classloader) throws Exception
	{
		ResourceInfo ret = null;
		if(registered.containsKey(name))
		{
			// Hack!!! ignore file handling for registered models.
			ICacheableModel	model	= (ICacheableModel)registered.get(name);
			ret	= new ResourceInfo(name, null, model.getLastModified());
		}
		else
		{
			// Try to find directly as absolute path.
//			Class clazz = SReflect.findClass0(name, imports, classloader);
//			if(clazz!=null)
				ret = new ResourceInfo(name, null, 0L);
	
//			if(ret==null)
//				throw new IOException("File "+name+" not found in imports");//: "+SUtil.arrayToString(imports));
		}
		return ret;
	}
	
}
