package jadex.micro.mandelbrot.generate;

import javax.swing.SwingUtilities;

import jadex.common.SGUI;
import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.micro.mandelbrot.calculate.ICalculateService;
import jadex.micro.mandelbrot.display.IDisplayService;
import jadex.micro.mandelbrot.model.AreaData;
import jadex.model.annotation.OnEnd;
import jadex.providedservice.ServiceScope;
import jadex.providedservice.annotation.Implementation;
import jadex.providedservice.annotation.ProvidedService;
import jadex.providedservice.annotation.ProvidedServices;
import jadex.requiredservice.annotation.OnService;
import jadex.requiredservice.annotation.RequiredService;
import jadex.requiredservice.annotation.RequiredServices;
	
/**
 *  Agent that can process generate requests.
 */
@Description("Agent offering a generate service.")
@ProvidedServices(@ProvidedService(type=IGenerateService.class, implementation=@Implementation(GenerateService.class)))
@RequiredServices({
	@RequiredService(name="displayservice", type=IDisplayService.class),
	@RequiredService(name="calculateservice", type=ICalculateService.class, scope=ServiceScope.GLOBAL), 
	@RequiredService(name="generateservice", type=IGenerateService.class)
})
@Agent
public class GenerateAgent implements IGenerateGui 
{
	@Agent
	protected IComponent agent;
	
	/** The generate panel. */
	protected GeneratePanel panel;

	//protected List<ICalculateService> calcservices = new ArrayList<>();
	protected IDisplayService displayservice;
	
	/*@OnStart
	protected void body()
	{
		this.panel = (GeneratePanel)GeneratePanel.createGui(agent.getExternalAccess());
	}*/
	
	/**
	 *  Update the area data.
	 *  @param data The data.
	 */
	public void updateData(AreaData data)
	{
		SwingUtilities.invokeLater(() -> panel.updateProperties(data));
	}
	
	/** 
	 * Update the status.
	 * @param cnt The cnt.
	 * @param number The number.
	 */
	public void updateStatus(int cnt, int number)
	{
		SwingUtilities.invokeLater(() -> panel.getStatusBar().setText("Finished: "+cnt+"("+number+")"));
	}
	
	/**
	 *  Stop the service.
	 */
	//@ServiceShutdown
	@OnEnd
	public IFuture<Void>	shutdown()
	{
//		System.out.println("shutdown: "+agent.getAgentName());
		final Future<Void>	ret	= new Future<Void>();
		if(panel!=null)
		{
//			System.out.println("shutdown1: "+agent.getAgentName());
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
//					System.out.println("shutdown2: "+agent.getAgentName());
					SGUI.getWindowParent(panel).dispose();
					ret.setResult(null);
//					System.out.println("shutdown3: "+agent.getAgentName());
				}
			});
		}
		else
		{
//			System.out.println("shutdown4: "+agent.getAgentName());
			ret.setResult(null);
		}
		return ret;
	}
	
	/*@OnService(name="calculateservice")
	protected void calculateServiceAvailable(ICalculateService cs)
	{
		System.out.println("Found calculate service: "+cs);
		calcservices.add(cs);
		if(displayservice!=null)
			agent.getLocalService(IGenerateService.class).calcDefaultImage();
	}*/
	
	@OnService(name="displayservice")
	protected void displayServiceAvailable(IDisplayService ds)
	{
		//System.out.println("Found display service: "+cs);
		this.displayservice = ds;
		
		this.panel = (GeneratePanel)GeneratePanel.createGui(agent.getComponentHandle());
		
		//if(calcservices.size()>0)
			//agent.getLocalService(IGenerateService.class).calcDefaultImage();
		//agent.getLocalService(IGenerateService.class).generateArea(null);
		// makes it sense at all to generate on display service discovery?
		// better is when display itself initiates a generation request,
		// because it knows the displayid (generate does not).
	}

	public IDisplayService getDisplayService() 
	{
		return displayservice;
	}

	/**
	 * @return the calcservice
	 * /
	public List<ICalculateService> getCalculateServices() 
	{
		return calcservices;
	}*/
}
