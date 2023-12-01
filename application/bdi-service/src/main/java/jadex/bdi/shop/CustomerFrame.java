package jadex.bdi.shop;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import jadex.bdi.runtime.ICapability;
import jadex.common.SGUI;
import jadex.core.IComponent;
import jadex.core.IThrowingConsumer;

/**
 *  Frame for displaying of the customer gui.
 */
public class CustomerFrame extends JFrame
{
	/**
	 *  Create a new frame.
	 */
	public CustomerFrame(IComponent agent, ICapability capa)
	{
		super(agent.getExternalAccess().getId().getLocalName());
		
		add(new CustomerPanel(agent, capa));
		pack();
		setLocation(SGUI.calculateMiddlePosition(this));
		setVisible(true);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
//				agent.killComponent();
				agent.getExternalAccess().scheduleStep((IThrowingConsumer<IComponent>)ia -> ia.terminate());
			}
		});
		
		// TODO
//		// Dispose frame on exception.
//		IResultListener<Void>	dislis	= new IResultListener<Void>()
//		{
//			public void exceptionOccurred(Exception exception)
//			{
//				dispose();
//			}
//			public void resultAvailable(Void result)
//			{
//			}
//		};
//		capa.getAgent().getFeature(IExecutionFeature.class).scheduleStep(new IComponentStep<Void>()
//		{
//			@Classname("dispose")
//			public IFuture<Void> execute(IInternalAccess ia)
//			{
//				ia.getFeature(IMonitoringComponentFeature.class).subscribeToEvents(IMonitoringEvent.TERMINATION_FILTER, false, PublishEventLevel.COARSE)
//					.addResultListener(new SwingIntermediateResultListener<IMonitoringEvent>(new IntermediateDefaultResultListener<IMonitoringEvent>()
//				{
//					public void intermediateResultAvailable(IMonitoringEvent result)
//					{
//						setVisible(false);
//						dispose();
//					}
//				}));
//				return IFuture.DONE;
//			}
//		}).addResultListener(dislis);
	}
}
