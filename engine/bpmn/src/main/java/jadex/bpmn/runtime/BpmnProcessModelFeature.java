package jadex.bpmn.runtime;

import java.util.Collections;

import jadex.common.IParameterGuesser;
import jadex.common.IValueFetcher;
import jadex.common.SimpleParameterGuesser;
import jadex.micro.MicroAgent;
import jadex.micro.impl.MicroModelFeature;
import jadex.model.IModelFeature;
import jadex.model.IParameterGuesserProvider;
import jadex.model.impl.IInternalModelFeature;
import jadex.model.modelinfo.IModelInfo;

public class BpmnProcessModelFeature extends MicroModelFeature
{
	public BpmnProcessModelFeature(BpmnProcess self)
	{
		super(self);
	}

	/**
	 *  Add $pojoagent to fetcher.
	 */
	public Object fetchValue(String name)
	{
		RBpmnProcess proc = (RBpmnProcess)self.getPojo();
		
		if("$pojoagent".equals(name))
		{
			return self.getPojo();
		}
		else if(proc.getArguments().containsKey(name))
		{
			return proc.getArgument(name);
		}
		else if(proc.getResults().containsKey(name))
		{
			return proc.getResult(name);
		}
		/*else if(name!=null && name.startsWith(IPlatformConfiguration.PLATFORMARGS))
		{
			String valname = name.length()>13? name.substring(14): null;
			return valname==null? Starter.getPlatformValue(getComponent().getId(), IPlatformConfiguration.PLATFORMARGS): Starter.getPlatformValue(getComponent().getId(), valname);
		}*/
		/*else if(Starter.hasPlatformValue(getComponent().getId(), name))
		{
			return Starter.getPlatformValue(getComponent().getId(), name);
		}*/
		else
		{
			throw new RuntimeException("Value not found: "+name);
		}
	}

	@Override
	public IModelInfo getModel()
	{
		return model;
	}
	
	@Override
	public void setModel(IModelInfo model)
	{
		this.model = model;
	}

	@Override
	public IValueFetcher getFetcher()
	{
		return this;
	}

}
