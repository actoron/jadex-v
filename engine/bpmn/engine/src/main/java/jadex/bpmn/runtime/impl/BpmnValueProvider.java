package jadex.bpmn.runtime.impl;

import jadex.bpmn.runtime.RBpmnProcess;
import jadex.core.impl.Component;

public class BpmnValueProvider extends jadex.core.impl.ValueProvider 
{
	public BpmnValueProvider(Component self)
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
		else if("$component".equals(name))
		{
			return self;
		}
		else if(proc.getArguments().containsKey(name))
		{
			return proc.getArgument(name);
		}
		// TODO: Read access to results!?
//		else if(proc.getResults().containsKey(name))
//		{
//			return proc.getResult(name);
//		}
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
}
