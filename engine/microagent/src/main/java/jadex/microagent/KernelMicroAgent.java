package jadex.microagent;

import jadex.classreader.SClassReader;
import jadex.classreader.SClassReader.AnnotationInfo;
import jadex.common.Boolean3;
import jadex.common.IFilter;
import jadex.common.SReflect;
import jadex.enginecore.annotation.NameValue;
import jadex.enginecore.service.ServiceScope;
import jadex.enginecore.service.types.factory.IComponentFactory;
import jadex.microagent.annotation.Agent;
import jadex.microagent.annotation.Implementation;
import jadex.microagent.annotation.Imports;
import jadex.microagent.annotation.Properties;
import jadex.microagent.annotation.ProvidedService;
import jadex.microagent.annotation.ProvidedServices;

/** 
 *  Micro kernel.
 */
@Imports("jadex.commons.*")
@ProvidedServices({@ProvidedService(type=IComponentFactory.class, scope=ServiceScope.PLATFORM, implementation=@Implementation(
	expression="new MicroAgentFactory($component, SUtil.createHashMap(new String[]{\"debugger.panels\", \"debugger.panel_web\"},new Object[]{\"jadex.tools.debugger.micro.MicroDebuggerPanel\",\"jadex/tools/web/debugger/microdebugger.js\"}))"))
})
@Agent(name="kernel_micro",
	autostart=Boolean3.FALSE)
@Properties(
{
	@NameValue(name="system", value="true"), 
	@NameValue(name="kernel.types", value="new String[]{\".class\"}"),
	@NameValue(name="kernel.filter", value="jadex.micro.KernelMicroAgent.AGENTFILTER"),
	@NameValue(name="kernel.componenttypes", value="new String[]{\""+MicroAgentFactory.FILETYPE_MICROAGENT+"\"}"),
	@NameValue(name="kernel.anntypes", value="new String[]{\""+MicroAgentFactory.TYPE+"\"}") // must declare in same order as component types 1:1 mapping
})
public class KernelMicroAgent
{
	public static final IFilter<Object> AGENTFILTER = new IFilter<Object>()
	{
		public boolean filter(Object obj)
		{
			boolean ret = false;
			if(obj instanceof SClassReader.ClassFileInfo)
			{
				SClassReader.ClassFileInfo ci = (SClassReader.ClassFileInfo)obj;
				AnnotationInfo ai = ci.getClassInfo().getAnnotation(Agent.class.getName());
				String type = ai!=null? (String)ai.getValue("type"): null;
				if(type==null)
					type = SReflect.getAnnotationDefaultValue(Agent.class, "type");
				if(ai!=null && MicroAgentFactory.TYPE.equals(type))
					ret = true;
				//if(ci.getFilename().indexOf("Agent")!=-1)
				//	System.out.println("microfilter: "+ret+" "+obj);
			}
			return ret;
		}
	};
	
	/*@Agent
	protected IInternalAccess agent;
	
	@AgentArgument
	protected ServiceScope scope;
	
	@OnInit
	public void init()
	{
		MicroAgentFactory fac = new MicroAgentFactory(agent, SUtil.createHashMap(new String[]{"debugger.panels"},new Object[]{"jadex.tools.debugger.micro.MicroDebuggerPanel"}));
		agent.addService(null, IComponentFactory.class, fac, null, s);
		
		addServiceType(, str, psi.getWorkermodel(), ci, psi.getPublishInfo()).addResultListener(lis);
	}*/
}
