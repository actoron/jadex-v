package jadex.micro.mandelbrot.display;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;

import jadex.common.SGUI;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.micro.mandelbrot.ui.ColorChooserPanel;
import jadex.providedservice.annotation.ProvideService;

/**
 *  Agent offering a display service.
 */
//@Description("Agent offering a display service.")
//@ProvidedServices({
//	@ProvidedService(type=IDisplayService.class, implementation=@Implementation(DisplayService.class))//,
////	@ProvidedService(type=IAppProviderService.class, implementation=@Implementation(AppProviderService.class))
//})
//@RequiredServices({
//	@RequiredService(name="generateservice", type=IGenerateService.class),
//})
//@Agent
public class DisplayAgent
{
	//-------- attributes --------
	
	/** The agent. */
	@Inject
	protected IComponent agent;
	
	/** The service. */
	@ProvideService
	protected IDisplayService	disp	= new DisplayService();
	
	/** The GUI. */
	protected DisplayPanel panel;
	
	//-------- MicroAgent methods --------
	
	/**
	 *  Called once after agent creation.
	 */
	//@AgentCreated
	//@OnInit
	@OnStart
	public void agentCreated()
	{
		DisplayAgent.this.panel	= new DisplayPanel(agent.getComponentHandle());

//		addService(new DisplayService(this));
				
		final IComponentHandle access = agent.getComponentHandle();
		final JFrame frame = new JFrame(agent.getId().getLocalName());
		JScrollPane	scroll	= new JScrollPane(panel);

		JTextPane helptext = new JTextPane();
		helptext.setText(DisplayPanel.HELPTEXT);
		helptext.setEditable(false);
		JPanel	right	= new JPanel(new BorderLayout());
		right.add(new ColorChooserPanel(panel), BorderLayout.CENTER);
		right.add(helptext, BorderLayout.NORTH);

		JSplitPane	split	= new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, right);
		split.setResizeWeight(1);
		split.setOneTouchExpandable(true);
		split.setDividerLocation(375);
		frame.getContentPane().add(BorderLayout.CENTER, split);
		frame.setSize(500, 400);
		frame.setLocation(SGUI.calculateMiddlePosition(frame));
		frame.setVisible(true);
		
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				IComponentManager.get().terminate(access.getId());
			}
		});
		
		/*access.scheduleStep(new IComponentStep<Void>()
		{
			@Classname("dispose")
			public IFuture<Void> execute(IInternalAccess ia)
			{
//						ia.addComponentListener(new TerminationAdapter()
//						{
//							public void componentTerminated()
//							{
//								SwingUtilities.invokeLater(new Runnable()
//								{
//									public void run()
//									{
//										frame.dispose();
//									}
//								});
//							}
//						});
				
				ia.getFeature(IMonitoringComponentFeature.class).subscribeToEvents(IMonitoringEvent.TERMINATION_FILTER, false, PublishEventLevel.COARSE)
					.addResultListener(/*new SwingIntermediateResultListener<IMonitoringEvent>(* /new IntermediateDefaultResultListener<IMonitoringEvent>()
				{
					public void intermediateResultAvailable(IMonitoringEvent result)
					{
						frame.dispose();
					}
				}/*)* /);
				
				return IFuture.DONE;
			}
		});*/
	}
	
	/*@OnEnd
	public void onEnd()
	{
		frame.dispose();
	}*/
	
	//-------- methods --------
	
	/**
	 *  Get the display panel.
	 */
	public DisplayPanel	getPanel()
	{
		return this.panel;
	}
}
