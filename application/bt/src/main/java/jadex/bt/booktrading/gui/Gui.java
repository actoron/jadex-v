package jadex.bt.booktrading.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import jadex.common.SGUI;
import jadex.core.IExternalAccess;

/**
 *  The gui allows to add and delete buy or sell orders and shows open and
 *  finished orders.
 */
@SuppressWarnings("serial")
public class Gui extends JFrame
{
	//-------- constructors --------

	/**
	 *  Shows the gui, and updates it when beliefs change.
	 */
	public Gui(final IExternalAccess agent)//, final boolean buy)
	{
		super((GuiPanel.isBuyer(agent)? "Buyer: ": "Seller: ")+agent.getId().getLocalName());
		
//		System.out.println("booktrading0: "+agent.getComponentIdentifier());
		GuiPanel gp = new GuiPanel(agent);
		
		add(gp, BorderLayout.CENTER);
		pack();
		setLocation(SGUI.calculateMiddlePosition(this));
		setVisible(true);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				agent.terminate();
			}
		});
	}
}