package jadex.mj.micro;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.MjComponent;
import jadex.mj.core.modelinfo.IModelInfo;
import jadex.mj.feature.lifecycle.impl.MjLifecycleFeature;

public class MjMicroAgent	extends MjComponent
{
	static protected MicroModelLoader loader = new MicroModelLoader();
	
	public static void	create(Object pojo)
	{
		loadModel(pojo.getClass().toString(), pojo, null).then(model ->
		{
			System.out.println("loaded micro model: "+model);
			MjLifecycleFeature.bootstrap(MjMicroAgent.class, () -> new MjMicroAgent(pojo, model));
		}).catchEx(e -> 
		{
			e.printStackTrace();
		});
	}
	
	protected Object	pojo;
	protected IModelInfo model;
	
	protected MjMicroAgent(Object pojo, IModelInfo model)
	{
		this.pojo	= pojo;
		this.model = model;
	}
	
	public Object getPojo() 
	{
		return pojo;
	}

	public IModelInfo getModel() 
	{
		return model;
	}

	/**
	 *  Load a  model.
	 *  @param model The model (e.g. file name).
	 *  @param The imports (if any).
	 *  @return The loaded model.
	 */
	static public IFuture<IModelInfo> loadModel(final String model, Object pojo, final String[] imports)
	{
		final Future<IModelInfo> ret = new Future<IModelInfo>();
//		System.out.println("filename: "+filename);
		
//		if(model.indexOf("HelloWorld")!=-1)
//			System.out.println("hw");
		
		try
		{
			MicroModel	cached	= (MicroModel)loader.getCachedModel(model, MicroModelLoader.FILE_EXTENSION_MICRO, imports, null);
			if(cached!=null)
			{
				ret.setResult(cached.getModelInfo());
			}
			else
			{
				try
				{
					ClassLoader cl = MjMicroAgent.class.getClassLoader();
					IModelInfo mi = loader.loadComponentModel(model, pojo, imports, cl).getModelInfo();
					ret.setResult(mi);
				}
				catch(Exception e)
				{
					ret.setException(e);
				}			
			}
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		
		return ret;
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName()+"("+pojo+")";
	}
}
