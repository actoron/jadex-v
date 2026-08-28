package jadex.bding.tool;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

public class CollapsiblePanel extends JPanel
{
    protected final JLabel arrow;
    protected final JLabel title;
    protected final JTextArea text;

    protected boolean expanded;

    public CollapsiblePanel(String titleText, String value)
    {
        super();

        setLayout(new BorderLayout());

        setAlignmentX(LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());

        arrow = new JLabel("▶");

        arrow.setPreferredSize(new Dimension(22, 25));

        title = new JLabel(titleText);

        title.setFont(
            title.getFont().deriveFont(Font.BOLD, 13f));

        header.add(arrow, BorderLayout.WEST);

        header.add(title, BorderLayout.CENTER);

        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)));

        header.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        text = new JTextArea(value == null ? "" : value);

        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);

        text.setFont(text.getFont().deriveFont(Font.PLAIN, 12f));

        text.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        text.setVisible(false);

        add(header, BorderLayout.NORTH);

        add(text, BorderLayout.CENTER);

        MouseAdapter listener = new MouseAdapter()
        {
            @Override
            public void mouseClicked(
                MouseEvent e)
            {
                setExpanded(
                    !expanded);
            }
        };

        header.addMouseListener(listener);

        arrow.addMouseListener(listener);

        title.addMouseListener(listener);
    }

    protected void setExpanded(boolean expanded)
    {
        this.expanded = expanded;

        arrow.setText(expanded? "▼": "▶");

        text.setVisible(expanded);

        revalidate();

        Container parent = getParent();

        while(parent != null)
        {
            parent.revalidate();
            parent.repaint();

            parent = parent.getParent();
        }

        repaint();
    }
}