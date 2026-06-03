package jadex.llm.house_monitoring;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import jadex.future.ISubscriptionIntermediateFuture;
import jadex.llm.house_monitoring.IAlarmService.AlarmState;

/**
 * Panel that visualizes the current alarm state.
 */
@SuppressWarnings("serial")
public class AlarmGui extends JPanel
{
	/** Keep the subscription alive while the panel exists. */
	protected final ISubscriptionIntermediateFuture<AlarmState> sub;

	protected final JLabel stateLabel;
	protected final JButton toggleButton;
	protected volatile AlarmState currentState;

	public AlarmGui(IAlarmService alarmService)
	{
		super(new BorderLayout());

		stateLabel = new JLabel("Alarm: unknown", SwingConstants.CENTER);
		stateLabel.setFont(stateLabel.getFont().deriveFont(Font.BOLD, 24f));
		add(stateLabel, BorderLayout.CENTER);

		toggleButton = new JButton("Toggle Alarm");
		toggleButton.addActionListener(e -> {
			AlarmState state = currentState;
			if(state == null)
				return;
			alarmService.setAlarmState(state == AlarmState.TRIGGERED ? AlarmState.SILENT : AlarmState.TRIGGERED);
		});
		add(toggleButton, BorderLayout.SOUTH);

		sub = alarmService.subcribeToAlarmState();
		sub.next(state -> SwingUtilities.invokeLater(() -> updateState(state)));
	}

	protected void updateState(AlarmState state)
	{
		currentState = state;
		if(state == AlarmState.TRIGGERED)
		{
			setBackground(new Color(255, 215, 215));
			stateLabel.setForeground(new Color(180, 0, 0));
			stateLabel.setText("Alarm: Triggered!");
			toggleButton.setText("Disable alarm");
		}
		else
		{
			setBackground(new Color(215, 255, 215));
			stateLabel.setForeground(new Color(0, 120, 0));
			stateLabel.setText("Alarm: Silent");
			toggleButton.setText("Enable alarm");
		}
	}
}