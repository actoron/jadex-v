package jadex.micro.llmcall2;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 * A plain {@link JScrollPane} with the horizontal scrollbar
 * always visible at the bottom, so wide content (e.g. long
 * code lines) can be scrolled horizontally by the user.
 */
public class ChatScrollPane extends JScrollPane
{
	/**
	 * Create a scroll pane with a horizontal scrollbar always
	 * visible at the bottom.
	 * @param viewport The component to be scrolled.
	 */
	public ChatScrollPane(JComponent viewport)
	{
		super(viewport,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * Scroll the view to the bottom and reset the horizontal
	 * scrollbar handle to the far left, so the right-hand
	 * content stays visible when new text arrives.
	 */
	public void scrollToBottomResetHorizontal()
	{
		javax.swing.text.JTextComponent	tc	= (javax.swing.text.JTextComponent)getViewport().getView();
		tc.setCaretPosition(tc.getDocument().getLength());
		getHorizontalScrollBar().setValue(0);
	}
}
