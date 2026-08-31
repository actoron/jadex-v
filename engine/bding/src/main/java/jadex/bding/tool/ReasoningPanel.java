package jadex.bding.tool;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import jadex.bding.ReasoningEntry;

public class ReasoningPanel extends JPanel
{
    protected final JPanel content;
    protected final Consumer<Object> selectionListener;

    protected int row;

    public ReasoningPanel(Consumer<Object> selectionListener)
    {
        this.selectionListener = selectionListener;

        setLayout(new BorderLayout());

        JLabel title = new JLabel("💭 Reasoning");

        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

        title.setBorder(
            BorderFactory.createEmptyBorder(
                6, 8, 6, 8));

        add(title, BorderLayout.NORTH);

        content = new JPanel(new GridBagLayout());

        JScrollPane scroll = new JScrollPane(content);

        scroll.setBorder(null);

        scroll.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        scroll.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scroll, BorderLayout.CENTER);
    }

    public void setSnapshot(BDISnapshot snapshot)
    {
        content.removeAll();
        row = 0;

        if(snapshot != null)
        {
            Set<ReasoningEntry> currentEntries =
                snapshot.currentReasoning();

            if(currentEntries != null)
            {
                for(ReasoningEntry entry : currentEntries)
                {
                    addEntry(entry, true);
                }
            }

            List<ReasoningEntry> history =
                snapshot.reasoningHistory();

            if(history != null)
            {
                int start =
                    Math.max(0, history.size() - 5);

                for(int i = history.size() - 1;
                    i >= start;
                    i--)
                {
                    ReasoningEntry entry =
                        history.get(i);

                    /*
                     * Don't show an entry twice if it is still
                     * contained in currentReasoning.
                     */
                    if(currentEntries == null ||
                        !currentEntries.contains(entry))
                    {
                        addEntry(entry, false);
                    }
                }
            }
        }

        GridBagConstraints filler =
            new GridBagConstraints();

        filler.gridx = 0;
        filler.gridy = row;

        filler.weightx = 1.0;
        filler.weighty = 1.0;

        filler.fill = GridBagConstraints.BOTH;

        content.add(
            Box.createGlue(),
            filler);

        content.revalidate();
        content.repaint();
    }

    protected void addEntry(
        ReasoningEntry entry,
        boolean current)
    {
        JPanel panel =
            new JPanel(new BorderLayout());

        panel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                    0,
                    0,
                    1,
                    0,
                    UIManager.getColor(
                        "Separator.foreground")),
                BorderFactory.createEmptyBorder(
                    5,
                    8,
                    5,
                    8)));

        JLabel method =
            new JLabel(
                (current ? "▶ " : "● ") +
                entry.method());

        method.setFont(
            method.getFont().deriveFont(
                Font.BOLD,
                12f));

        long duration =
            entry.duration() > 0
                ? entry.duration()
                : System.currentTimeMillis()
                    - entry.timestamp();

        JLabel durationLabel =
            new JLabel(
                entry.duration() > 0
                    ? formatDuration(duration)
                    : "running: " +
                        formatDuration(duration));

        durationLabel.setFont(
            durationLabel.getFont().deriveFont(
                Font.PLAIN,
                11f));

        panel.add(
            method,
            BorderLayout.WEST);

        panel.add(
            durationLabel,
            BorderLayout.EAST);

        panel.setCursor(
            new Cursor(Cursor.HAND_CURSOR));

        MouseAdapter listener =
            new MouseAdapter()
            {
                @Override
                public void mousePressed(
                    MouseEvent e)
                {
                    selectionListener.accept(entry);
                }
            };

        panel.addMouseListener(listener);
        method.addMouseListener(listener);
        durationLabel.addMouseListener(listener);

        GridBagConstraints c =
            new GridBagConstraints();

        c.gridx = 0;
        c.gridy = row++;

        c.weightx = 1.0;
        c.weighty = 0.0;

        c.fill =
            GridBagConstraints.HORIZONTAL;

        c.anchor =
            GridBagConstraints.NORTHWEST;

        c.insets =
            new Insets(0, 0, 0, 0);

        content.add(panel, c);
    }

    protected String formatDuration(long duration)
    {
        if(duration < 1000)
            return duration + " ms";

        return String.format(
            "%.2f s",
            duration / 1000.0);
    }
}