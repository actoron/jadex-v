package jadex.bding.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jadex.bding.IPlanBody;
import jadex.bding.IPlanStep;
import jadex.bding.Intention;
import jadex.bding.Plan;
import jadex.bding.ReasoningEntry;
import jadex.bding.impl.IntentionHistory.IntentionHistoryEntry;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.ReasoningStep;
import jadex.bding.impl.planbody.SubgoalStep;
import jadex.bding.impl.planbody.ToolCallStep;
import jadex.bding.tool.BDIViewer.PlanNode;
import jadex.core.IComponentHandle;


/**
 * Runtime viewer for a BDI agent.
 *
 * The viewer periodically takes a snapshot of the BDI runtime and renders
 * the current Goal -> Intention -> Plan hierarchy.
 */
public class BDIViewer extends JFrame
{
    protected final IComponentHandle agent;

    protected final BDIInspector inspector;

    protected final GoalTreePanel treePanel;
    protected final InspectorPanel inspectorPanel;
    protected final ReasoningPanel reasoningPanel;

    protected Object selectedObject;

    protected BDISnapshot snapshot;

    protected Timer refreshTimer;


    public BDIViewer(IComponentHandle agent)
    {
        super("BDING Agent");

        this.agent = agent;
        this.inspector = new BDIInspector(agent);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(800, 500));
        setLocationByPlatform(true);

        treePanel = new GoalTreePanel(this::showDetails);
        inspectorPanel = new InspectorPanel();

        JPanel leftPanel = new JPanel(new BorderLayout());

        JScrollPane treeScroll = new JScrollPane(treePanel);
        treeScroll.setBorder(null);


        leftPanel.setLayout(new GridBagLayout());

        reasoningPanel = new ReasoningPanel(this::showDetails);

        GridBagConstraints treeConstraints = new GridBagConstraints();
        treeConstraints.gridx = 0;
        treeConstraints.gridy = 0;
        treeConstraints.weightx = 1.0;
        treeConstraints.weighty = 0.70;
        treeConstraints.fill = GridBagConstraints.BOTH;
        treeConstraints.anchor = GridBagConstraints.NORTHWEST;

        leftPanel.add(treeScroll, treeConstraints);

        GridBagConstraints reasoningConstraints = new GridBagConstraints();
        reasoningConstraints.gridx = 0;
        reasoningConstraints.gridy = 1;
        reasoningConstraints.weightx = 1.0;
        reasoningConstraints.weighty = 0.30;
        reasoningConstraints.fill = GridBagConstraints.BOTH;
        reasoningConstraints.anchor = GridBagConstraints.NORTHWEST;

        leftPanel.add(reasoningPanel, reasoningConstraints);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, inspectorPanel);

        split.setDividerLocation(650);
        split.setResizeWeight(0.65);

        add(split, BorderLayout.CENTER);

        refreshTimer = new Timer(100, e -> refresh());
        refreshTimer.setCoalesce(true);
        refreshTimer.start();

        refresh();
    }

    protected void refresh()
    {
        try
        {
            BDISnapshot newSnapshot = inspector.createSnapshot();

            //if(!Objects.equals(snapshot, newSnapshot))
            {
                snapshot = newSnapshot;

                treePanel.setSnapshot(snapshot);
                reasoningPanel.setSnapshot(snapshot);

                if(selectedObject instanceof ReasoningEntry selected)
                {
                    ReasoningEntry updated = findReasoningEntry(snapshot, selected);

                    if(updated != null)
                    {
                        selectedObject = updated;
                        inspectorPanel.setObject(updated);
                    }
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    protected ReasoningEntry findReasoningEntry(BDISnapshot snapshot, ReasoningEntry selected)
    {
        if(snapshot == null || selected == null)
            return null;

        Set<ReasoningEntry> currentEntries = snapshot.currentReasoning();

        if(currentEntries != null)
        {
            for(ReasoningEntry entry : currentEntries)
            {
                if(entry.equals(selected))
                    return entry;
            }
        }

        List<ReasoningEntry> history = snapshot.reasoningHistory();

        if(history != null)
        {
            for(ReasoningEntry entry : history)
            {
                if(entry.equals(selected))
                    return entry;
            }
        }

        return null;
    }

    protected void showDetails(Object object)
    {
        inspectorPanel.setObject(object);
    }

    @Override
    public void dispose()
    {
        if(refreshTimer != null)
            refreshTimer.stop();

        super.dispose();
    }

    public static void show(IComponentHandle agent)
    {
        SwingUtilities.invokeLater(() ->
        {
            BDIViewer viewer = new BDIViewer(agent);
            viewer.setVisible(true);
        });
    }

    protected static class CollapsibleNode extends JPanel
    {
        protected final JPanel content;
        protected final JLabel arrow;

        protected boolean expanded;

        public CollapsibleNode(
            String icon,
            String title,
            String description,
            String extra,
            String status,
            int depth,
            boolean initiallyExpanded,
            Consumer<Boolean> expandedListener)
        {
            super();

            this.expanded = initiallyExpanded;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            JPanel header = new JPanel(new BorderLayout(8, 0));
            header.setAlignmentX(LEFT_ALIGNMENT);

            int left = 12 + depth * 22;

            header.setBorder(BorderFactory.createEmptyBorder(
                7, left, 7, 12));

            arrow = new JLabel(expanded ? "▼" : "▶");

            JLabel iconLabel = new JLabel(icon);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.PLAIN, 14f));

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            center.add(titleLabel);

            if(description != null && !description.isBlank())
            {
                JLabel descriptionLabel = new JLabel(description);
                descriptionLabel.setFont(
                    descriptionLabel.getFont().deriveFont(Font.PLAIN, 11f));

                center.add(descriptionLabel);
            }

            if(extra != null && !extra.isBlank())
            {
                JLabel extraLabel = new JLabel(extra);
                extraLabel.setFont(
                    extraLabel.getFont().deriveFont(Font.PLAIN, 10f));

                center.add(extraLabel);
            }

            JLabel statusLabel = new JLabel(
                status == null ? "" : status);

            JPanel leftPanel = new JPanel(
                new BorderLayout(5, 0));

            leftPanel.setOpaque(false);
            leftPanel.add(arrow, BorderLayout.WEST);
            leftPanel.add(iconLabel, BorderLayout.CENTER);

            header.add(leftPanel, BorderLayout.WEST);
            header.add(center, BorderLayout.CENTER);
            header.add(statusLabel, BorderLayout.EAST);

            content = new JPanel();
            content.setLayout(
                new BoxLayout(content, BoxLayout.Y_AXIS));

            content.setAlignmentX(LEFT_ALIGNMENT);

            header.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mousePressed(MouseEvent e)
                {
                    if(!SwingUtilities.isLeftMouseButton(e))
                        return;

                    setExpanded(!expanded);

                    if(expandedListener != null)
                        expandedListener.accept(expanded);
                }
            });

            add(header);
            add(content);

            content.setVisible(expanded);
        }

        public JPanel getContent()
        {
            return content;
        }

        public boolean isExpanded()
        {
            return expanded;
        }

        public void setExpanded(boolean expanded)
        {
            this.expanded = expanded;

            content.setVisible(expanded);
            arrow.setText(expanded ? "▼" : "▶");

            revalidate();
            repaint();
        }
    }

    public static class GoalTreePanel extends JPanel
    {
        protected final JPanel content;

        protected final Consumer<Object> selectionListener;

        protected BDISnapshot snapshot;

        protected final Set<String> expandedObjects = new HashSet<>();


        public GoalTreePanel(Consumer<Object> selectionListener)
        {
            super(new BorderLayout());

            this.selectionListener = selectionListener;

            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);

            add(scroll, BorderLayout.CENTER);
        }


        public void setSnapshot(BDISnapshot snapshot)
        {
            this.snapshot = snapshot;

            content.removeAll();

            if(snapshot == null || snapshot.goals().isEmpty())
            {
                JLabel empty = new JLabel("No active goals");
                empty.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

                content.add(empty);
            }
            else
            {
                for(RGoal goal : snapshot.goals())
                {
                    content.add(new GoalNode(goal, 0, selectionListener, this));
                }
            }

            content.revalidate();
            content.repaint();
        }

        protected boolean isExpanded(String key)
        {
            return expandedObjects.contains(key);
        }

        protected void setExpanded(String key, boolean expanded)
        {
            if(expanded)
                expandedObjects.add(key);
            else
                expandedObjects.remove(key);
        }

    }

    protected static class GoalNode extends JPanel
    {
        protected final RGoal goal;

        public GoalNode(RGoal goal, int depth, Consumer<Object> selectionListener, GoalTreePanel treePanel)
        {
            super();

            this.goal = goal;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            add(new Header("🎯", goal.getGoal().getName(), goal.getGoal().getDescription(),
                formatParameters(goal), goal.getState().toString(), depth,
                () -> selectionListener.accept(goal)));

            Set<Intention> intentions = goal.getGoal().getIntentions();

            RIntention current = goal.getIntention();
            Intention currentIntention = current != null ? current.getIntention() : null;

            if(intentions != null && !intentions.isEmpty())
            {
                add(new Header("💡", "Possible intentions (" + intentions.size() + ")", null,
                    null, "", depth + 1, null));

                for(Intention intention : intentions)
                {
                    if(currentIntention != null && currentIntention.equals(intention))
                        continue;

                    boolean attempted = goal.getHistory() != null && goal.getHistory().isKnown(intention);

                    String status = attempted ? "ATTEMPTED": "AVAILABLE";

                    add(new IntentionNode(intention, status, depth + 2, selectionListener));
                }
            }

            if(current != null)
            {
                add(new Header("▶", "Current intention", null, null, "ACTIVE", depth + 1,null));
                add(new IntentionNode(current, depth + 2, selectionListener, treePanel));
            }

            if(goal.getHistory() != null && !goal.getHistory().getEntries().isEmpty())
            {
                add(Box.createVerticalStrut(5));

                add(new HistoryNode(goal, depth + 1, selectionListener, treePanel));
            }

            constrainHeight(this);
        }
    }

    protected static class HistoryNode extends CollapsibleNode
    {
        public HistoryNode(RGoal goal, int depth, Consumer<Object> selectionListener, GoalTreePanel treePanel)
        {
            super(
                "↻",
                "History (" + goal.getHistory().getEntries().size() + ")",
                null,
                null,
                "",
                depth,
                treePanel.isExpanded("history_"+goal.getId()),
                expanded -> treePanel.setExpanded("history_"+goal.getId(), expanded));

            for(IntentionHistoryEntry entry : goal.getHistory().getEntries())
            {
                RIntention intention = entry.getIntention();

                if(intention != null)
                {
                    getContent().add(new IntentionNode(intention, depth + 1, selectionListener, treePanel));
                }
            }
        }
    }

    protected static String formatParameters(RGoal goal)
    {
        if(goal.getParameters() == null ||goal.getParameters().isEmpty())
        {
            return null;
        }

        return goal.getParameters()
            .entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + String.valueOf(e.getValue()))
            .collect(Collectors.joining(" · "));
    }

    protected static class IntentionNode extends JPanel
    {
        protected final Object intention;

        public IntentionNode(RIntention intention, int depth, Consumer<Object> selectionListener, GoalTreePanel treePanel)
        {
            super();

            this.intention = intention;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            Intention model = intention.getIntention();

            add(new Header("💡",model.getName(), model.getDescription(), null, "ACTIVE",
                depth, () -> selectionListener.accept(intention)));

            RPlan plan = intention.getPlan();

            if(plan != null)
                add(new PlanNode(plan, depth + 1, selectionListener, treePanel));

            constrainHeight(this);
        }

        public IntentionNode(Intention intention, String status, int depth, Consumer<Object> selectionListener)
        {
            super();

            this.intention = intention;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            add(new Header("💡", intention.getName(), intention.getDescription(), null, status, depth,
                () -> selectionListener.accept(intention)));

            constrainHeight(this);
        }
    }

    protected static class PlanNode extends CollapsibleNode
    {
        public PlanNode(RPlan plan, int depth, Consumer<Object> selectionListener, GoalTreePanel treePanel)
        {
            super(
                "📋",
                plan.getPlan().getName(),
                plan.getPlan().getDescription(),
                null,
                "",
                depth,
                treePanel.isExpanded(plan.getId()),
                expanded -> treePanel.setExpanded(plan.getId(), expanded));

            IPlanBody body = plan.getPlan().getBody();

            if(body != null)
            {
                int stepno = 0;

                for(IPlanStep step : body.getSteps())
                {
                    getContent().add(new PlanStepNode(step, stepno++, depth + 1, selectionListener));
                }
            }
        }
    }

    protected static class PlanStepNode extends JPanel
    {
        public PlanStepNode(IPlanStep step, int index, int depth, Consumer<Object> selectionListener)
        {
            super();

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            String icon;
            String title;
            String description = null;

            if(step instanceof ToolCallStep tool)
            {
                icon = "🔧";
                title = tool.getToolName();
                description = "Tool call";
            }
            else if(step instanceof ReasoningStep reasoning)
            {
                icon = "🧠";
                title = "Reasoning";
                description = reasoning.getProblem();
            }
            else if(step instanceof SubgoalStep subgoal)
            {
                icon = "🎯";
                title = "Subgoal";
                description = subgoal.getGoal();
            }
            else
            {
                icon = "•";
                title = step.getClass().getSimpleName();
            }

            add(new Header(icon, (index + 1) + ". " + title,
                description, null, "", depth,
                () -> selectionListener.accept(step)));

            constrainHeight(this);
        }
    }

    protected static void constrainHeight(JComponent component)
    {
        Dimension pref = component.getPreferredSize();

        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
    }

    protected static class Header extends JPanel
    {
        protected final JLabel arrowLabel;

        public Header(
            String icon,
            String title,
            String description,
            String extra,
            String status,
            int depth,
            Runnable click)
        {
            this(
                icon,
                title,
                description,
                extra,
                status,
                depth,
                click,
                null,
                false);
        }

        public Header(
            String icon,
            String title,
            String description,
            String extra,
            String status,
            int depth,
            Runnable click,
            Runnable toggle,
            boolean expanded)
        {
            super(new BorderLayout(8, 0));

            setAlignmentX(LEFT_ALIGNMENT);

            int left = 12 + depth * 22;

            setBorder(BorderFactory.createEmptyBorder(
                7, left, 7, 12));

            arrowLabel = new JLabel(
                toggle != null
                    ? (expanded ? "▼" : "▶")
                    : "");

            JLabel iconLabel = new JLabel(icon);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(
                titleLabel.getFont().deriveFont(Font.PLAIN, 14f));

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            center.add(titleLabel);

            if(description != null && !description.isBlank())
            {
                JLabel descriptionLabel = new JLabel(description);
                descriptionLabel.setFont(
                    descriptionLabel.getFont().deriveFont(Font.PLAIN, 11f));

                center.add(descriptionLabel);
            }

            if(extra != null && !extra.isBlank())
            {
                JLabel extraLabel = new JLabel(extra);
                extraLabel.setFont(
                    extraLabel.getFont().deriveFont(Font.PLAIN, 10f));

                center.add(extraLabel);
            }

            JLabel statusLabel = new JLabel(
                status == null ? "" : status);

            JPanel leftPanel = new JPanel(
                new BorderLayout(5, 0));

            leftPanel.setOpaque(false);

            leftPanel.add(
                arrowLabel,
                BorderLayout.WEST);

            leftPanel.add(
                iconLabel,
                BorderLayout.CENTER);

            add(leftPanel, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);

            /*
            * Pfeil: expand / collapse
            */
            if(toggle != null)
            {
                arrowLabel.setCursor(
                    Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR));

                arrowLabel.addMouseListener(
                    new MouseAdapter()
                    {
                        @Override
                        public void mousePressed(MouseEvent e)
                        {
                            if(SwingUtilities.isLeftMouseButton(e))
                            {
                                toggle.run();
                            }
                        }
                    });
            }

            /*
            * Restlicher Header: Inspector
            */
            if(click != null)
            {
                setToolTipText("Click to inspect");

                MouseAdapter listener =
                    new MouseAdapter()
                    {
                        @Override
                        public void mousePressed(MouseEvent e)
                        {
                            if(SwingUtilities.isLeftMouseButton(e))
                            {
                                click.run();
                            }
                        }
                    };

                iconLabel.addMouseListener(listener);
                center.addMouseListener(listener);
                statusLabel.addMouseListener(listener);

                setCursor(
                    Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR));
            }

            constrainHeight(this);
        }

        public void setExpanded(boolean expanded)
        {
            arrowLabel.setText(
                expanded ? "▼" : "▶");
        }
    }

    protected static void addClickListener(Component component, MouseAdapter listener)
    {
        component.addMouseListener(listener);
        component.setCursor(
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if(component instanceof Container container)
        {
            for(Component child : container.getComponents())
            {
                addClickListener(child, listener);
            }
        }
    }

    
}
    