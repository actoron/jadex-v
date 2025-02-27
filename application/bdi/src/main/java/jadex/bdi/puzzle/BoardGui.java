package jadex.bdi.puzzle;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jadex.common.SGUI;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingConsumer;

/**
 *  The board gui.
 */
@SuppressWarnings("serial")
public class BoardGui extends JFrame
{
	//-------- attributes --------

	/** The board to visualize. */
	protected IBoard board;

	//-------- constructors --------

	/**
	 *  Create a new board gui.
	 */
	public BoardGui(IComponentHandle agent, final IBoard board)
	{
		this(agent, board, false);
	}

	/**
	 *  Create a new board gui.
	 */
	public BoardGui(final IComponentHandle agent, final IBoard board, boolean controls)
	{
		this.board = board;
		final BoardPanel bp = new BoardPanel(board);
		this.board.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				bp.update(evt);
			}
		});

		this.getContentPane().add("Center", bp);
		if(controls)
		{
			final BoardControlPanel bcp = new BoardControlPanel(board, bp);
			this.getContentPane().add("South", bcp);
		}
		this.setTitle("Puzzle Board");
		this.setSize(400, 400);
		this.setLocation(SGUI.calculateMiddlePosition(this));
		this.setVisible(true);

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				agent.scheduleStep( (IThrowingConsumer<IComponent>)(a -> a.terminate()));
			}
		});
		
		// close gui on agent termination
		agent.waitForTermination().then(b ->
			SwingUtilities.invokeLater(() -> BoardGui.this.dispose()));
	}
}
