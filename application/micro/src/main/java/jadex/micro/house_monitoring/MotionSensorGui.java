package jadex.micro.house_monitoring;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *  Simple UI for a motion sensor service.
 *  Allows triggering a motion detected event.
 */
@SuppressWarnings("serial")
public class MotionSensorGui extends JPanel
{
	/** The motion sensor service. */
	protected IMotionSensorService motionSensor;

	/** Status label. */
	protected JLabel statusLabel;

	/**
	 *  Create a new motion sensor GUI panel.
	 */
	public MotionSensorGui(IMotionSensorService motionSensor)
	{
		this.motionSensor = motionSensor;

		setLayout(new BorderLayout(4, 4));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JLabel title = new JLabel("Motion sensor control");
		add(title, BorderLayout.NORTH);

		JButton triggerBtn = new JButton("Trigger motion");
		add(triggerBtn, BorderLayout.CENTER);

		statusLabel = new JLabel("Ready.");
		add(statusLabel, BorderLayout.SOUTH);

		triggerBtn.addActionListener(e ->
		{
			setStatus("Triggering motion event...");
			motionSensor.motionDetected().then(v ->
			{
				setStatus("Motion event sent.");
			}).catchEx(ex ->
			{
				setStatus("Error triggering motion: " + ex);
			});
		});
	}

	/**
	 *  Update status text.
	 */
	protected void setStatus(String text)
	{
		statusLabel.setText(text);
	}
}