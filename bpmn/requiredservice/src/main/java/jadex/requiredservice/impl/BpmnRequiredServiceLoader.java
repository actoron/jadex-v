package jadex.requiredservice.impl;

import java.util.Map;

import jadex.bpmn.model.MBpmnModel;
import jadex.common.ClassInfo;
import jadex.model.modelinfo.IModelInfo;
import jadex.providedservice.ServiceScope;
import jadex.requiredservice.RequiredServiceBinding;
import jadex.requiredservice.RequiredServiceInfo;

public class BpmnRequiredServiceLoader 
{
	public static Object readFeatureModel(IModelInfo modelinfo)
	{
		MBpmnModel model = (MBpmnModel)modelinfo.getRawModel();
		RequiredServiceModel ret = null;
	
		Map<String, Object> exts = model.getExtension("requiredservice");
		
		if(exts!=null)
		{
			ret = new RequiredServiceModel();
			
			for(String name: exts.keySet())
			{
				Map<String, String> attrs = (Map<String, String>)exts.get(name); 

				//String name = attrs.get("name");
				ClassInfo itrface = attrs.get("interface") != null? new ClassInfo(attrs.get("interface")) : null;
				Boolean multi = attrs.get("multi") != null? Boolean.parseBoolean(attrs.get("multi")) : null;
				String scope = attrs.get("scope");
				//String dyn = attrs.get("dynamic");
				//String create = attrs.get("create");
				
				RequiredServiceInfo rs = new RequiredServiceInfo();
				rs.setName(name);
				rs.setType(itrface);
				if(multi != null)
					rs.setMax(RequiredServiceInfo.MANY);
					//rs.setMultiple(multi.booleanValue());
				rs.setDefaultBinding(new RequiredServiceBinding());
				rs.getDefaultBinding().setScope(ServiceScope.valueOf(scope.toUpperCase()));
				// Dropped in v4??
	//			if(dyn!=null)
	//				rs.getDefaultBinding().setDynamic(Boolean.parseBoolean(dyn));
	//			if(create!=null)
	//				rs.getDefaultBinding().setCreate(Boolean.parseBoolean(create));
				ret.addRequiredService(rs);
			}
		}
		
		return ret;
	}
}
