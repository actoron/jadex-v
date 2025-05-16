package jadex.bdi.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.IGoal;
import jadex.bdi.impl.BDIAgentFeature;
import jadex.bdi.impl.BDIModel;
import jadex.bdi.impl.plan.RPlan;
import jadex.common.UnparsedExpression;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingFunction;

@SuppressWarnings("serial")
public class BDIViewer extends JFrame 
{
    private final IComponentHandle agent;
    private final JTable goalTable, planTable, beliefTable;
    private final DefaultTableModel goalModel, planModel, beliefModel;
    
    record BeliefInfo(Class<?> type, Object value, String preview) 
    {
    }

    public BDIViewer(IComponentHandle agent) 
    {
        this.agent = agent;
        setTitle("" + agent.getId().getLocalName());
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JSplitPane topSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setTopComponent(topSplitPane);

        goalModel = new DefaultTableModel(new String[]{"ID", /*"Type",*/ "Lifecycle State", "Processing State", "Pojo"}, 0);
        goalTable = createTable(goalModel);
        topSplitPane.setTopComponent(createBorderPanel("Goals", goalTable));

        planModel = new DefaultTableModel(new String[]{"ID", /*"Type",*/ "State", "Subgoals", "Pojo"}, 0);
        planTable = createTable(planModel);
        
        topSplitPane.setBottomComponent(createBorderPanel("Plans", planTable));
        topSplitPane.setResizeWeight(0.5);

        beliefModel = new DefaultTableModel(new String[]{"Name", "Type", "Dynamic", "Updaterate", "Value"}, 0);
        beliefTable = createTable(beliefModel);
        mainSplitPane.setBottomComponent(createBorderPanel("Beliefs", beliefTable));
        mainSplitPane.setResizeWeight(0.66);

        add(mainSplitPane);
        startAutoRefresh();
    }
    
    private JPanel createBorderPanel(String title, JTable table)
    {
        JPanel pan = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(table); 
        pan.add(BorderLayout.CENTER, scrollPane); 
        pan.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.LEADING, TitledBorder.TOP));
        return pan; 
    }

    private JTable createTable(DefaultTableModel model) 
    {
        JTable table = new JTable(model);
        PojoCellRenderer renderer = new PojoCellRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) 
        {
        	table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        
        table.addMouseListener(new MouseAdapter() 
        {
            @Override
            public void mouseClicked(MouseEvent e) 
            {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == model.getColumnCount() - 1) 
                {
                    Object pojo = model.getValueAt(row, col);
                    Object id = model.getValueAt(row, 1);
                    if (pojo != null) 
                    {
                        JFrame frame = ObjectInspectorPanel.createObjectFrame(""+id, pojo);
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);
                    }
                }
            }
        });
        return table;
    }

    private void startAutoRefresh() 
    {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() 
        {
            @Override
            public void run() 
            {
                SwingUtilities.invokeLater(BDIViewer.this::refreshTables);
            }
        }, 0, 100);
    }

    private void refreshTables() 
    {
       	refreshGoalsTable(goalModel, agent.scheduleStep((IThrowingFunction<IComponent, IGoal[]>)a -> {
       		return a.getFeature(IBDIAgentFeature.class).getGoals().toArray(new IGoal[0]);
       	}).get());
       	
       	refreshPlansTable(planModel, agent.scheduleStep((IThrowingFunction<IComponent, RPlan[]>)a -> 
       	{
       		Set<RPlan>	plans	= ((BDIAgentFeature)a.getFeature(IBDIAgentFeature.class)).getPlans();
       		return plans!=null ? plans.toArray(new RPlan[0]) : new RPlan[0];
       	}).get());
       	
       	refreshBeliefsTable(beliefModel, agent.scheduleStep((IThrowingFunction<IComponent, String[]>)a -> 
       	{
       		return ((BDIAgentFeature)a.getFeature(IBDIAgentFeature.class)).getModel().getBeliefs().toArray(new String[0]);
       	}).get());
    }

    private void refreshGoalsTable(DefaultTableModel model, IGoal[] goals) 
    {
        model.setRowCount(0);
        for (IGoal goal: goals) 
        {
            Object[] row = new Object[]
            {
                goal.getId(),
//                goal.getPojo().getClass(),
                goal.getLifecycleState(),
                goal.getProcessingState(),
                goal.getPojo()
            };
            model.addRow(row);
        }
    }
    
    private void refreshPlansTable(DefaultTableModel model, RPlan[] plans) 
    {
        model.setRowCount(0);
        for (RPlan plan: plans) 
        {
            Object[] row = new Object[]
            {
                plan.getId(),
                //plan.getType(),
                plan.getLifecycleState(),
                plan.getSubgoals(),
                plan.getPojo()
            };
            model.addRow(row);
        }
    }

    private void refreshBeliefsTable(DefaultTableModel model, String[] beliefs) 
    {
        model.setRowCount(0);
        for (String belief : beliefs) 
        {
        	BeliefInfo info = agent.scheduleStep((IThrowingFunction<IComponent, BeliefInfo>) a -> 
            {
//            	Object val= belief.getValue();
//            	return new BeliefValue(val, val.toString());
            	BDIModel	bdimodel	= ((BDIAgentFeature)a.getFeature(IBDIAgentFeature.class)).getModel();
            	Class<?>	type	= bdimodel.getBelief(belief);
            	return new BeliefInfo(type, null, null);
            }).get();

            model.addRow(new Object[]
            {
                belief.replace(".", "/"),
                info.type(),
                "",//belief.isDynamic(),
                "",//belief.getUpdateRate(),
                info
            });
        }
    }

    public static String getShortedText(String text) 
    {
        int idx = text.lastIndexOf("$");
        if (idx != -1) 
        {
            text = text.substring(idx + 1);
        } 
        else 
        {
            idx = text.lastIndexOf(".");
            if (idx != -1) text = text.substring(idx + 1);
        }
        return text;
    }

    public static class PojoCellRenderer extends DefaultTableCellRenderer 
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) 
        {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if(value instanceof String || value instanceof Class)
            	setText(getShortedText(value.toString()));
            else if(value instanceof UnparsedExpression)
            	setText(((UnparsedExpression)value).getValue());
            else if(value instanceof BeliefInfo)
            	setText(((BeliefInfo)value).preview());
            
            return this;
        }
    }
}
