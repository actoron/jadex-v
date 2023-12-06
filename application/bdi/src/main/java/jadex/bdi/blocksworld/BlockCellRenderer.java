package jadex.bdi.blocksworld;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;


/**
 *  Cell renderer for blocks.
 */
public class BlockCellRenderer	extends JLabel	implements ListCellRenderer
{
	//-------- constructors --------

	/**
	 *  Create a block cell renderer.
	 */
	public BlockCellRenderer()
	{
		setOpaque(true);
	}

	//-------- ListCellRenderer interface ---------

	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		Block	block	= (Block)value;
		setText(block.toString());
		setBackground(block.getColor());
		setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
		return this;
	}
}


