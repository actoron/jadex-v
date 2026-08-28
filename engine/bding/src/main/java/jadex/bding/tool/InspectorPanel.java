package jadex.bding.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jadex.bding.Goal;
import jadex.bding.IPlanBody;
import jadex.bding.IPlanStep;
import jadex.bding.Intention;
import jadex.bding.Parameter;
import jadex.bding.Plan;
import jadex.bding.ReasoningEntry;
import jadex.bding.impl.RGoal;
import jadex.bding.impl.RIntention;
import jadex.bding.impl.RPlan;
import jadex.bding.impl.planbody.ReasoningStep;
import jadex.bding.impl.planbody.SubgoalStep;
import jadex.bding.impl.planbody.ToolCallStep;

public class InspectorPanel extends JPanel
{
    protected final JPanel content;

    protected int row;

    public InspectorPanel()
    {
        setLayout(new BorderLayout());
        content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(15, 18, 15, 18));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    public void setObject(Object object)
    {
        content.removeAll();
        row = 0;

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
        else if(object instanceof Intention intention)
        {
            showIntention(intention);
        }
        else if(object instanceof RPlan plan)
        {
            showPlan(plan);
        }
        else if(object instanceof IPlanStep step)
        {
            showPlanStep(step);
        }
        else if(object instanceof ReasoningEntry entry)
        {
            showReasoning(entry);
        }
        else
        {
            addTitle(object.getClass().getSimpleName());
            addField("Value", String.valueOf(object));
        }

        GridBagConstraints filler = new GridBagConstraints();

        filler.gridx = 0;
        filler.gridy = row++;

        filler.weightx = 1.0;
        filler.weighty = 1.0;

        filler.fill = GridBagConstraints.VERTICAL;
        filler.anchor = GridBagConstraints.NORTHWEST;

        content.add(Box.createGlue(), filler);

        content.revalidate();
        content.repaint();
    }

    protected GridBagConstraints constraints()
    {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = row++;

        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.anchor = GridBagConstraints.NORTHWEST;

        gbc.insets = new Insets(0, 0, 0, 0);

        return gbc;
    }

    protected void addComponent(Component component)
    {
        addComponent(component, 0.0);
    }

    protected void addComponent(Component component, double weighty)
    {
        GridBagConstraints gbc = constraints();
        gbc.weighty = weighty;
        content.add(component, gbc);
    }

    protected void addComponent(Component component, Insets insets)
    {
        GridBagConstraints gbc = constraints();
        gbc.insets = insets;
        content.add(component, gbc);
    }

    protected void addComponent(Component component, double weighty, int fill)
    {
        GridBagConstraints gbc = constraints();

        gbc.weighty = weighty;
        gbc.fill = fill;

        content.add(component, gbc);
    }

    protected void addVerticalSpace(int pixels)
    {
        addComponent(Box.createVerticalStrut(pixels));
    }

    protected void showGoal(RGoal rgoal)
    {
        Goal goal = rgoal.getGoal();

        addTitle("🎯 " + goal.getName());

        addField("Description", goal.getDescription());

        addSection("Runtime");

        addField("State", rgoal.getState().toString());

        if(rgoal.getIntention() != null)
        {
            addField("Current intention", rgoal.getIntention().getIntention().getName());
        }
        else
        {
            addField("Current intention", "None");
        }

        addSection("Goal model");

        addField("Importance", goal.getImportance() != null ? goal.getImportance().toString() : "Not specified");

        addField("Keep on success", Boolean.toString(goal.isKeepOnSuccess()));

        addSection("Conditions");

        addField("Activation", goal.getActivationWhen());

        addField("Success", goal.getSuccessWhen());

        addField("Failure", goal.getFailureWhen());

        addSection("Parameters");

        if(goal.getParameters().isEmpty())
        {
            addField("Parameters", "None");
        }
        else
        {
            for(Parameter parameter : goal.getParameters().values())
            {
                Object value = rgoal.getParameters().get(parameter.getName());

                addParameter(parameter, value);
            }
        }

        addSection("Generated intentions");

        if(goal.getIntentions().isEmpty())
        {
            addField("Intentions", "None");
        }
        else
        {
            for(Intention intention : goal.getIntentions())
            {
                addField(intention.getName(), intention.getDescription());
            }
        }
    }

    protected void showIntention(RIntention rintention)
    {
        Intention intention = rintention.getIntention();

        addTitle("💡 " + intention.getName());

        addField("Description", intention.getDescription());

        addSection("Runtime");

        addField("Intention", intention.getName());

        if(rintention.getPlan() != null)
        {
            addField(
                "Current plan",
                rintention.getPlan().getPlan().getName());
        }
        else
        {
            addField("Current plan", "None");
        }

        addSection("Intention model");

        addField("Name", intention.getName());
        addField("Description", intention.getDescription());
    }

    protected void showIntention(Intention intention)
    {
        addTitle("💡 " + intention.getName());

        addField("Description", intention.getDescription());

        addSection("Intention model");

        addField("Name", intention.getName());
        addField("Description", intention.getDescription());
    }

    protected void showPlan(RPlan rplan)
    {
        Plan plan = rplan.getPlan();

        addTitle("📋 " + plan.getName());

        addField("Description", plan.getDescription());

        addSection("Runtime");

        //addField("State", rplan.getState().toString());

        addSection("Plan model");

        addField("Name", plan.getName());
        addField("Description", plan.getDescription());

        IPlanBody body = plan.getBody();

        if(body != null)
        {
            addSection("Plan body");

            addField("Steps", Integer.toString(body.getSteps().size()));
        }
    }

    protected void showPlanStep(IPlanStep step)
    {
        if(step instanceof ToolCallStep tool)
        {
            showToolCallStep(tool);
        }
        else if(step instanceof ReasoningStep reasoning)
        {
            showReasoningStep(reasoning);
        }
        else if(step instanceof SubgoalStep subgoal)
        {
            showSubgoalStep(subgoal);
        }
        else
        {
            addTitle("• " + step.getClass().getSimpleName());
        }
    }

    protected void showToolCallStep(ToolCallStep step)
    {
        addTitle("🔧 " + step.getToolName());

        addSection("Tool");

        addField("Name", step.getToolName());

        addField(
            "Result mapping",
            step.getResultMapping());

        addSection("Parameter mapping");

        if(step.getMapping() == null || step.getMapping().isEmpty())
        {
            addField("Mapping", "None");
        }
        else
        {
            for(Map.Entry<String, String> entry : step.getMapping().entrySet())
            {
                addField(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void showReasoningStep(ReasoningStep step)
    {
        addTitle("🧠 Reasoning");

        addField("Type", step.getReasoningType().toString());

        addSection("Problem");

        addCollapsibleSection("Problem", step.getProblem());

        addSection("Result");

        addField("Result mapping", step.getResultMapping());
    }

    protected void showSubgoalStep(SubgoalStep step)
    {
        addTitle("🎯 Subgoal");

        addField("Goal", step.getGoal());
    }

    protected void showReasoning(ReasoningEntry entry)
    {
        addTitle("💭 " + entry.method());

        addField("Status", entry.duration() > 0 ? "Completed": "Running");

        if(entry.duration() > 0)
        {
            addField("Duration", formatDuration(entry.duration()));
        }

        addSection("Reasoning");

        addCollapsibleSection("Prompt", entry.prompt());

        addCollapsibleSection("Response", entry.response());
    }

    protected String formatDuration(long duration)
    {
        if(duration < 1000)
            return duration + " ms";

        return String.format(
            "%.2f s",
            duration / 1000.0);
    }

    protected void addTitle(String text)
    {
        JLabel label = new JLabel(text);

        label.setFont(label.getFont().deriveFont(Font.BOLD, 20f));

        label.setAlignmentX(LEFT_ALIGNMENT);

        addComponent(label, new Insets(0, 0, 12, 0));
    }

    protected void addSection(String text)
    {
        addVerticalSpace(10);

        JLabel label = new JLabel(text);

        label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));

        label.setAlignmentX(LEFT_ALIGNMENT);

        label.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        addComponent(label);
    }

    protected void addField(String name, String value)
    {
        if(value == null || value.isBlank())
            value = "—";

        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));

        JLabel nameLabel = new JLabel(name);

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));

        JLabel valueLabel = new JLabel("<html>" + escapeHtml(value) + "</html>");

        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN, 12f));

        rowPanel.add(nameLabel, BorderLayout.WEST);

        rowPanel.add(valueLabel, BorderLayout.CENTER);

        addComponent(rowPanel);
    }

    protected void addParameter(Parameter parameter, Object value)
    {
        JLabel nameLabel = new JLabel(parameter.getName());

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));

        addComponent(nameLabel);

        addField("Type", parameter.getType() != null ? parameter.getType().toString() : "Unknown");

        addField("Description", parameter.getDescription());

        addField("Current value", String.valueOf(value));

        addVerticalSpace(8);
    }

    protected void addCollapsibleSection(String title, String text)
    {
        CollapsiblePanel panel = new CollapsiblePanel(title, text);

        addComponent(panel, new Insets(0, 0, 8, 0));
    }

    protected String escapeHtml(String text)
    {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br>");
    }
}