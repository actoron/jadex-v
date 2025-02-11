package jadex.micro;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import jadex.common.SReflect;
import jadex.core.Application;
import jadex.core.ComponentIdentifier;
import jadex.core.IComponentHandle;
import jadex.core.impl.Component;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.annotation.AgentResult;
import jadex.model.IModelFeature;
import jadex.model.impl.IInternalModelFeature;
import jadex.model.modelinfo.IModelInfo;

public class MicroAgent	extends Component
{
	protected static MicroModelLoader loader = new MicroModelLoader();
	
	public static IComponentHandle create(Object pojo)
	{
		return create(pojo, null, null);
	}
	
	public static IComponentHandle create(Object pojo, ComponentIdentifier cid, Application app)
	{
		Component comp = Component.createComponent(MicroAgent.class, () -> 
		{
			// this is executed before the features are inited
			return loadModel(pojo.getClass().toString(), pojo, null).thenApply(model ->
			{
				//System.out.println("loaded micro model: "+model);
				
				return new MicroAgent(pojo, model, cid, app);
			}).get();
		});
		
		return comp.getComponentHandle();
	}
	
	public MicroAgent(Object pojo, IModelInfo model)
	{
		this(pojo, model, null, null);
	}
	
	public MicroAgent(Object pojo, IModelInfo model, ComponentIdentifier cid, Application app)
	{
		super(pojo, cid, app);
		((IInternalModelFeature)this.getFeature(IModelFeature.class)).setModel(model);
		//this.model = model;
		
		//ComponentIdentifier execid = getFeature(IMjExecutionFeature.class).getComponent().getId();
		//if(!execid.equals(cid))
		//	System.out.println(execid+" "+cid);
	}
	
	public IModelInfo getModel() 
	{
		return this.getFeature(IModelFeature.class).getModel();
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
					ClassLoader cl = MicroAgent.class.getClassLoader();
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
	
	/*public Map<String, Object> getResults()
	{
		Map<String, Object> ret = new HashMap<String, Object>();
		if(pojo!=null)
		{
			Class<?> pcl = pojo.getClass();
			Field[] fls = SReflect.getAllFields(pcl);
			
			for(int i=0; i<fls.length; i++)
			{
				if(MicroClassReader.isAnnotationPresent(fls[i], AgentResult.class, ComponentManager.get().getClassLoader()))
				{
					try
					{
						AgentResult r = MicroClassReader.getAnnotation(fls[i], AgentResult.class, ComponentManager.get().getClassLoader());
						fls[i].setAccessible(true);
						Object val = fls[i].get(pojo);
						ret.put(fls[i].getName(), val);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return ret;
	}*/
	
	@Override
	public String toString()
	{
		return this.getClass().getSimpleName()+"("+pojo+")";
	}
}
