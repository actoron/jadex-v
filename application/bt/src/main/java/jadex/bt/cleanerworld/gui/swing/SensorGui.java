package jadex.bt.cleanerworld.gui.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jadex.bt.cleanerworld.BTCleanerAgent;
import jadex.common.SGUI;
import jadex.core.IComponentHandle;
import jadex.execution.IExecutionFeature;


/**
 *  The GUI for the cleaner world example.
 *  Shows the world from the viewpoint of a single agent.
 */
public class SensorGui
{
	//-------- attributes --------

	// The window
	private JFrame	frame;
	
	// The repaint timer
	private Timer	timer;

	//-------- constructors --------

	/**
	 *  Creates a GUI that updates itself when beliefs change.
	 */
	public SensorGui(IComponentHandle handle)
	{
		String id = handle.getPojoHandle(BTCleanerAgent.class).getCleaner().get().getId();
		
		// Open window on swing thread
		SwingUtilities.invokeLater(()->
		{
			this.frame	= new JFrame(id);
			final JPanel map = new SensorPanel(handle);

			frame.getContentPane().add(BorderLayout.CENTER, map);
			frame.setSize(300, 300);
			frame.setLocation(SGUI.calculateMiddlePosition(frame));
			frame.setVisible(true);
			
			// Repaint every 50 ms.
			timer	= new Timer(50, new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					map.invalidate();
					map.repaint();
				}
			});
			timer.start();
			
			// Kill agent on window close.
			frame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					// todo!
					//lifecycle.terminate();
				}
			});
		});
		
//		// Close window on agent kill.
//		SComponentManagementService.listenToComponent(cid, agent)
//			//.addIntermediateResultListener(cse ->
//			.next(cse ->
//		{
//			if(cse instanceof CMSTerminatedEvent)
//			{
//				SwingUtilities.invokeLater(new Runnable()
//				{
//					public void run()
//					{
//						timer.stop();
//						frame.dispose();
//					}
//				});
//			}
//		});
	}
	
	//-------- methods --------
	
	/**
	 *  Show/hide the GUI.
	 */
	public void	setVisible(boolean visible)
	{
		SwingUtilities.invokeLater(()->frame.setVisible(visible));
	}		
}