package jadex.micro.house_monitoring;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jadex.future.ISubscriptionIntermediateFuture;
import jadex.micro.house_monitoring.IAlarmService.AlarmState;

/**
 * Panel that visualizes the current alarm state.
 */
@SuppressWarnings("serial")
public class AlarmGui extends JPanel
{
	/** Keep the subscription alive while the panel exists. */
	protected final ISubscriptionIntermediateFuture<AlarmState> sub;

	protected final JLabel stateLabel;

	public AlarmGui(IAlarmService alarmService)
	{
		super(new BorderLayout());

		stateLabel = new JLabel("Alarm: unknown", SwingConstants.CENTER);
		stateLabel.setFont(stateLabel.getFont().deriveFont(Font.BOLD, 24f));
		add(stateLabel, BorderLayout.CENTER);

		sub = alarmService.subSubcribeToAlarmState();
		sub.next(state -> SwingUtilities.invokeLater(() -> updateState(state)));
	}

	protected void updateState(AlarmState state)
	{
		if(state == AlarmState.ON)
		{
			setBackground(new Color(255, 215, 215));
			stateLabel.setForeground(new Color(180, 0, 0));
			stateLabel.setText("Alarm: ON");
		}
		else
		{
			setBackground(new Color(215, 255, 215));
			stateLabel.setForeground(new Color(0, 120, 0));
			stateLabel.setText("Alarm: OFF");
		}
	}
}
