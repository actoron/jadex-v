package jadex.bding.tool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.util.Tokenizer.Cursor;

import jadex.bding.Goal;
import jadex.bding.IBDINGAgentFeature;
import jadex.bding.IPlanStep;
import jadex.bding.IReasoner;
import jadex.bding.Intention;
import jadex.bding.Parameter;
import jadex.bding.ReasoningEntry;
import jadex.bding.impl.BeliefSnapshot;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.RPlan;
import jadex.core.IComponentHandle;
import jadex.core.INoCopyStep;


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

        leftPanel.add(
            treeScroll,
            BorderLayout.CENTER);

        reasoningPanel = new ReasoningPanel(this::showDetails);

        leftPanel.add(reasoningPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(
            JSplitPane.HORIZONTAL_SPLIT,
            leftPanel,
            inspectorPanel);

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

            if(!Objects.equals(snapshot, newSnapshot))
            {
                snapshot = newSnapshot;

                treePanel.setSnapshot(snapshot);
                reasoningPanel.setSnapshot(snapshot);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
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


    /*
     * ---------------------------------------------------------------------
     * Snapshot
     * ---------------------------------------------------------------------
     */

    /**
     * A snapshot contains the actual runtime objects.
     *
     * No separate GoalInfo / IntentionInfo / PlanInfo objects are necessary.
     */
    public record BDISnapshot(
        Set<RGoal> goals,
        BeliefSnapshot beliefs,
        ReasoningEntry currentReasoning,
        List<ReasoningEntry> reasoningHistory)
    {
    }


    /*
     * ---------------------------------------------------------------------
     * Inspector
     * ---------------------------------------------------------------------
     */

    /**
     * Reads the BDI runtime from the agent.
     *
     * The snapshot is created inside the agent thread so that all runtime
     * objects are read consistently.
     */
    protected static class BDIInspector
    {
        protected final IComponentHandle agent;

        public BDIInspector(IComponentHandle agent)
        {
            this.agent = agent;
        }


        public BDISnapshot createSnapshot()
        {
            return agent.scheduleStep((INoCopyStep<BDISnapshot>)ag ->
            {
                IBDINGAgentFeature feature =
                    ag.getFeature(IBDINGAgentFeature.class);

                Set<RGoal> goals = feature.getGoals();
                BeliefSnapshot beliefs = feature.getBeliefs();

                IReasoner reasoner = feature.getReasoner();

                ReasoningEntry current =
                    reasoner.getCurrentReasoning().get();

                List<ReasoningEntry> history =
                    reasoner.getReasoningHistory().get();

                return new BDISnapshot(
                    goals,
                    beliefs,
                    current,
                    history);
            }).get();
        }
    }


    /*
     * ---------------------------------------------------------------------
     * Goal tree
     * ---------------------------------------------------------------------
     */

    public static class GoalTreePanel extends JPanel
    {
        protected final JPanel content;

        protected final Consumer<Object> selectionListener;

        protected BDISnapshot snapshot;


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
                empty.setBorder(
                    BorderFactory.createEmptyBorder(20, 20, 20, 20));

                content.add(empty);
            }
            else
            {
                for(RGoal goal : snapshot.goals())
                {
                    content.add(new GoalNode(
                        goal,
                        0,
                        selectionListener));
                }
            }

            content.revalidate();
            content.repaint();
        }
    }


    /*
     * ---------------------------------------------------------------------
     * Goal node
     * ---------------------------------------------------------------------
     */

    protected static class GoalNode extends JPanel
    {
        protected final RGoal goal;

        public GoalNode(RGoal goal, int depth, Consumer<Object> selectionListener)
        {
            super();

            this.goal = goal;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            /*
            * Goal itself
            */
            add(new Header("🎯", goal.getGoal().getName(), goal.getGoal().getDescription(), formatParameters(goal),
                goal.getState().toString(),
                depth, () -> selectionListener.accept(goal)));

            /*
            * Possible intentions
            *
            * These are the alternatives generated for this goal.
            * The currently active intention is rendered separately below.
            */
            Set<Intention> intentions = goal.getGoal().getIntentions();

            RIntention current = goal.getIntention();
            Intention currentIntention = current != null ? current.getIntention() : null;

            if(intentions != null && !intentions.isEmpty())
            {
                add(new Header(
                    "💡",
                    "Possible intentions (" + intentions.size() + ")",
                    null,
                    null,
                    "",
                    depth + 1,
                    null));

                for(Intention intention : intentions)
                {
                    /*
                    * Don't show the current intention a second time here.
                    */
                    if(currentIntention != null && currentIntention.equals(intention))
                        continue;

                    boolean attempted = goal.getHistory() != null && goal.getHistory().isKnown(intention);

                    String status = attempted ? "ATTEMPTED": "AVAILABLE";

                    add(new Header(attempted ? "↻" : "○", intention.getName(), intention.getDescription(), null, status, depth + 2,
                        () -> selectionListener.accept(intention)));
                }
            }

            /*
            * Current intention
            */
            if(current != null)
            {
                add(new Header(
                    "▶",
                    "Current intention",
                    null,
                    null, 
                    "ACTIVE",
                    depth + 1,
                    null));

                add(new IntentionNode(
                    current,
                    depth + 2,
                    selectionListener));
            }

            /*
            * History
            */
            if(goal.getHistory() != null &&
                !goal.getHistory().getEntries().isEmpty())
            {
                add(Box.createVerticalStrut(5));

                add(new Header(
                    "↻",
                    "History (" +
                        goal.getHistory().getEntries().size() +
                        ")",
                    null,
                    null, 
                    "",
                    depth + 1,
                    null));

                /*
                * History nodes can be added here once the
                * IntentionHistoryEntry API is exposed.
                */
            }

            constrainHeight(this);
        }
    }

    protected static String formatParameters(RGoal goal)
    {
        if(goal.getParameters() == null ||
            goal.getParameters().isEmpty())
        {
            return null;
        }

        return goal.getParameters()
            .entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + String.valueOf(e.getValue()))
            .collect(Collectors.joining(" · "));
    }

    /*
     * ---------------------------------------------------------------------
     * Intention node
     * ---------------------------------------------------------------------
     */

    protected static class IntentionNode extends JPanel
    {
        protected final RIntention intention;


        public IntentionNode(
            RIntention intention,
            int depth,
            Consumer<Object> selectionListener)
        {
            super();

            this.intention = intention;

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            Intention model = intention.getIntention();

            add(new Header(
                "💡",
                model.getName(),
                model.getDescription(),
                null,
                "",
                depth,
                () -> selectionListener.accept(intention)));

            /*
             * Adapt this to the actual RIntention API.
             *
             * For example, if RIntention has:
             *
             *     getPlan()
             *
             * then:
             *
             * Plan plan = intention.getPlan();
             *
             *     if(plan != null)
             *         add(new PlanNode(...));
             */

            constrainHeight(this);
        }
    }


    /*
     * ---------------------------------------------------------------------
     * Plan node
     * ---------------------------------------------------------------------
     */

    protected static class PlanNode extends JPanel
    {
        public PlanNode(
            Object plan,
            int depth,
            Consumer<Object> selectionListener)
        {
            super();

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setAlignmentX(LEFT_ALIGNMENT);

            /*
             * This is deliberately kept as Object in this first version
             * because the exact Plan API is not shown here.
             *
             * Once Plan's getters are known this becomes:
             *
             *     Plan p = (Plan)plan;
             *
             *     p.getName()
             *     p.getDescription()
             *     p.getPlanBody()
             */

            constrainHeight(this);
        }
    }

    protected static void constrainHeight(JComponent component)
    {
        Dimension pref = component.getPreferredSize();

        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
    }

    /*
     * ---------------------------------------------------------------------
     * Header
     * ---------------------------------------------------------------------
     */

    protected static class Header extends JPanel
    {
        public Header(
            String icon,
            String title,
            String description,
            String extra,
            String status,
            int depth,
            Runnable click)
        {
            super(new BorderLayout(8, 0));

            setAlignmentX(LEFT_ALIGNMENT);

            int left = 12 + depth * 22;

            setBorder(BorderFactory.createEmptyBorder(
                7,
                left,
                7,
                12));

            JLabel iconLabel = new JLabel(icon);

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(
                titleLabel.getFont().deriveFont(
                    Font.PLAIN, 14f));

            JPanel center = new JPanel();
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setOpaque(false);

            center.add(titleLabel);

            if(description != null && !description.isBlank())
            {
                JLabel descriptionLabel = new JLabel(description);
                descriptionLabel.setFont(
                    descriptionLabel.getFont().deriveFont(
                        Font.PLAIN, 11f));

                center.add(descriptionLabel);
            }

            if(extra != null && !extra.isBlank())
            {
                JLabel extraLabel = new JLabel(extra);
                extraLabel.setFont(
                    extraLabel.getFont().deriveFont(
                        Font.PLAIN, 10f));

                center.add(extraLabel);
            }

            JLabel statusLabel = new JLabel(
                status == null ? "" : status);

            add(iconLabel, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
            add(statusLabel, BorderLayout.EAST);

            if(click != null)
            {
                setToolTipText("Click to inspect");
                setCursor(new java.awt.Cursor(
                    java.awt.Cursor.HAND_CURSOR));

                MouseAdapter listener = new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        click.run();
                    }
                };

                addMouseListener(listener);

                addMouseListenerToChildren(this, listener);
            }

            Dimension pref = getPreferredSize();

            setMaximumSize(
                new Dimension(
                    Integer.MAX_VALUE,
                    pref.height));
        }
    }

    protected static void addMouseListenerToChildren(
        java.awt.Container container,
        MouseAdapter listener)
    {
        for(java.awt.Component component :
            container.getComponents())
        {
            component.addMouseListener(listener);

            if(component instanceof java.awt.Container child)
            {
                addMouseListenerToChildren(child, listener);
            }
        }
    }

    /*
     * ---------------------------------------------------------------------
     * Inspector panel
     * ---------------------------------------------------------------------
     */

    protected static class InspectorPanel extends JPanel
    {
        protected final JPanel content;

        public InspectorPanel()
        {
            setLayout(new BorderLayout());

            content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(
                BorderFactory.createEmptyBorder(15, 18, 15, 18));

            JScrollPane scroll = new JScrollPane(content);
            scroll.setBorder(null);

            add(scroll, BorderLayout.CENTER);
        }

        public void setObject(Object object)
        {
            content.removeAll();

            if(object == null)
            {
                addTitle("Select an element");
            }
            else if(object instanceof RGoal goal)
            {
                showGoal(goal);
            }
            else if(object instanceof RIntention intention)
            {
                showIntention(intention);
            }
            else if(object instanceof RPlan plan)
            {
                showPlan(plan);
            }
            /*else if(object instanceof IPlanStep step)
            {
                showStep(step);
            }*/

            content.revalidate();
            content.repaint();
        }


        /*
        * -------------------------------------------------------------
        * Goal
        * -------------------------------------------------------------
        */

        protected void showGoal(RGoal rgoal)
        {
            Goal goal = rgoal.getGoal();

            addTitle("🎯 " + goal.getName());

            addField(
                "Description",
                goal.getDescription());

            addSection("Runtime");

            addField(
                "State",
                rgoal.getState().toString());

            if(rgoal.getIntention() != null)
            {
                addField(
                    "Current intention",
                    rgoal.getIntention()
                        .getIntention()
                        .getName());
            }
            else
            {
                addField(
                    "Current intention",
                    "None");
            }

            addSection("Goal model");

            addField(
                "Importance",
                goal.getImportance() != null
                    ? goal.getImportance().toString()
                    : "Not specified");

            addField(
                "Keep on success",
                Boolean.toString(
                    goal.isKeepOnSuccess()));

            addSection("Conditions");

            addField(
                "Activation",
                goal.getActivationWhen());

            addField(
                "Success",
                goal.getSuccessWhen());

            addField(
                "Failure",
                goal.getFailureWhen());

            addSection("Parameters");

            if(goal.getParameters().isEmpty())
            {
                addField(
                    "Parameters",
                    "None");
            }
            else
            {
                for(Parameter parameter : goal.getParameters().values())
                {
                    Object value = rgoal.getParameters().get(parameter.getName());

                    addParameter(
                        parameter,
                        value);
                }
            }

            addSection("Generated intentions");

            if(goal.getIntentions().isEmpty())
            {
                addField(
                    "Intentions",
                    "None");
            }
            else
            {
                for(Intention intention :
                    goal.getIntentions())
                {
                    addField(
                        intention.getName(),
                        intention.getDescription());
                }
            }
        }


        /*
        * -------------------------------------------------------------
        * Helper methods
        * -------------------------------------------------------------
        */

        protected void addTitle(String text)
        {
            JLabel label = new JLabel(text);

            label.setFont(
                label.getFont().deriveFont(
                    Font.BOLD, 20f));

            label.setAlignmentX(LEFT_ALIGNMENT);

            content.add(label);
            content.add(Box.createVerticalStrut(12));
        }


        protected void addSection(String text)
        {
            content.add(Box.createVerticalStrut(10));

            JLabel label = new JLabel(text);

            label.setFont(
                label.getFont().deriveFont(
                    Font.BOLD, 14f));

            label.setAlignmentX(LEFT_ALIGNMENT);

            label.setBorder(
                BorderFactory.createEmptyBorder(
                    5, 0, 5, 0));

            content.add(label);
        }


        protected void addField(
            String name,
            String value)
        {
            if(value == null || value.isBlank())
                value = "—";

            JPanel row = new JPanel(
                new BorderLayout(10, 0));

            row.setAlignmentX(LEFT_ALIGNMENT);

            JLabel nameLabel =
                new JLabel(name);

            nameLabel.setFont(
                nameLabel.getFont().deriveFont(
                    Font.BOLD, 12f));

            JLabel valueLabel =
                new JLabel(
                    "<html>" +
                    escapeHtml(value) +
                    "</html>");

            valueLabel.setFont(
                valueLabel.getFont().deriveFont(
                    Font.PLAIN, 12f));

            row.add(
                nameLabel,
                BorderLayout.WEST);

            row.add(
                valueLabel,
                BorderLayout.CENTER);

            content.add(row);
        }


        protected void addParameter(Parameter parameter, Object value)
        {
            JLabel nameLabel =
                new JLabel(parameter.getName());

            nameLabel.setFont(
                nameLabel.getFont().deriveFont(
                    Font.BOLD, 13f));

            nameLabel.setAlignmentX(
                LEFT_ALIGNMENT);

            content.add(nameLabel);

            /*
            * Adjust these getters if your Parameter class
            * uses different names.
            */
            addField(
                "Type",
                parameter.getType() != null
                    ? parameter.getType().toString()
                    : "Unknown");

            addField(
                "Description",
                parameter.getDescription());

            addField(
                "Current value",
                String.valueOf(value));

            content.add(
                Box.createVerticalStrut(8));
        }


        protected String escapeHtml(String text)
        {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        }


        /*
        * -------------------------------------------------------------
        * TODO: other inspectors
        * -------------------------------------------------------------
        */

        protected void showIntention(RIntention intention)
        {
            addTitle(
                "💡 " +
                intention.getIntention().getName());

            addField(
                "Description",
                intention.getIntention().getDescription());
        }


        protected void showPlan(RPlan plan)
        {
            addTitle("📋 " + plan.getPlan().getName());

            /*addField(
                "Status",
                plan.);*/

            addField(
                "Description",
                plan.getPlan().getDescription());
        }


        /*protected void showStep(IPlanStep step)
        {
            addTitle(
                (step.type() == StepType.TOOL
                    ? "🔧 "
                    : "🎯 ") +
                step.name());

            addField(
                "Type",
                step.type().toString());

            addField(
                "Status",
                step.status().toString());

            addField(
                "Description",
                step.description());
        }*/
    }

    protected static class ReasoningPanel extends JPanel
    {
        protected final JPanel content;

        protected final Consumer<Object> selectionListener;

        protected BDISnapshot snapshot;

        public ReasoningPanel(Consumer<Object> selectionListener)
        {
            super(new BorderLayout());

            this.selectionListener =
                selectionListener;

            setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(
                        1, 0, 0, 0,
                        UIManager.getColor(
                            "Component.borderColor")),
                    BorderFactory.createEmptyBorder(
                        6, 8, 6, 8)));

            JPanel header = new JPanel(
                new BorderLayout());

            JLabel title =
                new JLabel("🧠 Reasoning");

            title.setFont(
                title.getFont().deriveFont(
                    Font.BOLD,
                    13f));

            header.add(
                title,
                BorderLayout.WEST);

            add(
                header,
                BorderLayout.NORTH);

            content = new JPanel();

            content.setLayout(
                new BoxLayout(
                    content,
                    BoxLayout.Y_AXIS));

            JScrollPane scroll =
                new JScrollPane(content);

            scroll.setBorder(null);

            add(
                scroll,
                BorderLayout.CENTER);

            /*
            * Keep the reasoning section compact.
            */
            setPreferredSize(
                new Dimension(
                    0,
                    220));
        }


        public void setSnapshot(BDISnapshot snapshot)
        {
            this.snapshot = snapshot;

            rebuild();
        }


        protected void rebuild()
        {
            content.removeAll();

            if(snapshot == null)
            {
                content.revalidate();
                content.repaint();
                return;
            }

            /*
            * Current reasoning
            */
            ReasoningEntry current = snapshot.currentReasoning();

            if(current != null)
            {
                addSectionLabel(
                    "CURRENT");

                addEntry(
                    current,
                    true);
            }

            /*
            * History
            */
            List<ReasoningEntry> history =
                snapshot.reasoningHistory();

            if(history != null &&
                !history.isEmpty())
            {
                addSectionLabel("RECENT REASONING");

                int start =
                    Math.max(
                        0,
                        history.size() - 5);

                /*
                * Newest first.
                */
                for(int i = history.size() - 1;
                    i >= start;
                    i--)
                {
                    addEntry(
                        history.get(i),
                        false);
                }

                if(history.size() > 5)
                {
                    JButton more =
                        new JButton(
                            "Show all (" +
                            history.size() +
                            ")");

                    more.setAlignmentX(
                        LEFT_ALIGNMENT);

                    more.addActionListener(e ->
                    {
                        showAllHistory();
                    });

                    content.add(
                        Box.createVerticalStrut(4));

                    content.add(more);
                }
            }

            content.revalidate();
            content.repaint();
        }


        protected void addSectionLabel(String text)
        {
            JLabel label =
                new JLabel(text);

            label.setFont(
                label.getFont().deriveFont(
                    Font.BOLD,
                    10f));

            label.setBorder(
                BorderFactory.createEmptyBorder(
                    5, 4, 2, 4));

            label.setAlignmentX(
                LEFT_ALIGNMENT);

            content.add(label);
        }


        protected void addEntry(
            ReasoningEntry entry,
            boolean current)
        {
            String icon =
                current ? "→" : "✓";

            String status =
                current
                    ? "RUNNING"
                    : "DONE";

            String duration =
                formatDuration(entry, current);

            Header header =
                new Header(
                    icon,
                    getMethod(entry),
                    null,
                    null,
                    duration,
                    0,
                    () ->
                        selectionListener.accept(entry));

            header.setAlignmentX(
                LEFT_ALIGNMENT);

            content.add(header);
        }


        public void updateDuration()
        {
            if(snapshot == null)
                return;

            /*
            * Only the current entry's duration
            * needs to be updated.
            *
            * Do not rebuild the whole panel here.
            */
            ReasoningEntry current =
                snapshot.currentReasoning();

            if(current == null)
                return;

            /*
            * For the first implementation the
            * complete panel can simply be rebuilt.
            *
            * Later we can keep a reference to
            * the current duration label.
            */
            rebuild();
        }


        protected void showAllHistory()
        {
            if(snapshot == null)
                return;

            /*
            * Simple first implementation:
            * display all entries in a dialog.
            */
            JPanel panel = new JPanel();

            panel.setLayout(
                new BoxLayout(
                    panel,
                    BoxLayout.Y_AXIS));

            List<ReasoningEntry> history =
                snapshot.reasoningHistory();

            for(int i = history.size() - 1;
                i >= 0;
                i--)
            {
                ReasoningEntry entry =
                    history.get(i);

                Header header =
                    new Header(
                        "✓",
                        getMethod(entry),
                        null,
                        null,
                        formatDuration(
                            entry,
                            false),
                        0,
                        () ->
                            selectionListener
                                .accept(entry));

                panel.add(header);
            }

            JScrollPane scroll =
                new JScrollPane(panel);

            scroll.setPreferredSize(
                new Dimension(
                    600,
                    500));

            javax.swing.JOptionPane.showMessageDialog(
                this,
                scroll,
                "Reasoning history",
                javax.swing.JOptionPane
                    .PLAIN_MESSAGE);
        }

        /*
        * These three methods isolate the exact
        * ReasoningEntry API from the viewer.
        *
        * Replace the getter names with the
        * actual methods of your class.
        */

        protected static String getMethod(ReasoningEntry entry)
        {
            return entry.method();
        }


        protected static long getTimestamp(ReasoningEntry entry)
        {
            return entry.timestamp();
        }


        protected static long getDuration(ReasoningEntry entry)
        {
            return entry.duration();
        }

        protected static String formatDuration(
            ReasoningEntry entry,
            boolean current)
        {
            long duration;

            if(current)
            {
                duration =
                    System.currentTimeMillis()
                    - entry.timestamp();
            }
            else
            {
                duration = entry.duration();
            }

            duration = Math.max(0, duration);

            if(duration < 1000)
                return duration + " ms";

            return String.format(
                "%.1f s",
                duration / 1000.0);
        }
    }
}