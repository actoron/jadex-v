package jadex.micro.house_monitoring;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.providedservice.IService;

/**
 *  Main GUI is a component that searches available services and
 *  combines house monitoring panels in tabs.
 */
public class MainGui
{
	/** The UI for discovered services. */
	JTabbedPane tabs = new JTabbedPane();
	
	/**
	 *  Discover camera services and add corresponding panels to the UI.
	 */
	@Inject
	void cameraAdded(ICameraService camserv)
	{
		SwingUtilities.invokeLater(() ->
		{
			CameraGui cameraGui = new CameraGui(camserv);
			tabs.addTab(((IService)camserv).getServiceId().getProviderId().getLocalName(), cameraGui);
		});
	}

	/**
	 *  Discover camera services and add corresponding panels to the UI.
	 */
	@Inject
	void motionSensorAdded(IMotionSensorService motserv)
	{
		SwingUtilities.invokeLater(() ->
		{
			MotionSensorGui motGui = new MotionSensorGui(motserv);
			tabs.addTab(((IService)motserv).getServiceId().getProviderId().getLocalName(), motGui);
		});
	}

	/**
	 * Discover alarm services and add corresponding panels to the UI.
	 */
	@Inject
	void alarmAdded(IAlarmService alarmserv)
	{
		SwingUtilities.invokeLater(() ->
		{
			AlarmGui alarmGui = new AlarmGui(alarmserv);
			tabs.addTab(((IService)alarmserv).getServiceId().getProviderId().getLocalName(), alarmGui);
		});
	}

	/**
	 * Show the GUI frame.
	 */
	@OnStart
	void show()
	{
		SwingUtilities.invokeLater(() ->
		{
			JFrame frame = new JFrame("House Monitoring");
			frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			frame.setContentPane(tabs);
			frame.addWindowListener(new WindowAdapter()
			{
				@Override
				public void windowClosed(WindowEvent e)
				{
					//TODO
//					terminateComponents();
				}
			});
			frame.setSize(800, 600);
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}
}