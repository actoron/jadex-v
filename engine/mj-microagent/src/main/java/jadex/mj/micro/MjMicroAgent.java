package jadex.mj.micro;

import jadex.future.Future;
import jadex.future.IFuture;
import jadex.mj.core.ComponentIdentifier;
import jadex.mj.core.IComponent;
import jadex.mj.core.MjComponent;
import jadex.mj.core.modelinfo.IModelInfo;
import jadex.mj.feature.execution.IMjExecutionFeature;

public class MjMicroAgent	extends MjComponent
{
	static protected MicroModelLoader loader = new MicroModelLoader();
	
	public static void create(Object pojo)
	{
		create(pojo, null);
	}
	
	public static void	create(Object pojo, ComponentIdentifier cid)
	{
		IComponent.createComponent(MjMicroAgent.class, () -> 
		{
			// this is executed before the features are inited
			return loadModel(pojo.getClass().toString(), pojo, null).thenApply(model ->
			{
				//System.out.println("loaded micro model: "+model);
				
				return new MjMicroAgent(pojo, model, cid);
			}).get();
		});
	}
	
	protected Object pojo;
	
	public MjMicroAgent(Object pojo, IModelInfo model)
	{
		this(pojo, model, null);
	}
	
	public MjMicroAgent(Object pojo, IModelInfo model, ComponentIdentifier cid)
	{
		super(model, cid);
		this.pojo	= pojo;
		
		//ComponentIdentifier execid = getFeature(IMjExecutionFeature.class).getComponent().getId();
		//if(!execid.equals(cid))
		//	System.out.println(execid+" "+cid);
	}
	
	public Object getPojo() 
	{
		return pojo;
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
