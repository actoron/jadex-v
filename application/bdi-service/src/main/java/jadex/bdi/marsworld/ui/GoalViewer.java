package jadex.bdi.marsworld.ui;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import jadex.bdi.runtime.IBDIAgentFeature;
import jadex.bdi.runtime.IGoal;
import jadex.core.IComponentHandle;

public class GoalViewer extends JFrame 
{
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final IComponentHandle agent;

    public GoalViewer(IComponentHandle agent) 
    {
        this.agent = agent;
        setTitle(""+agent.getId().getLocalName());
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        tableModel = new DefaultTableModel(new String[]{"ID", "Type", "Lifecycle State", "Processing State"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);
        
        startAutoRefresh();
    }

    private void startAutoRefresh() 
    {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() 
        {
            @Override
            public void run() 
            {
                SwingUtilities.invokeLater(GoalViewer.this::refreshTable);
            }
        }, 0, 1000);
    }

    private void refreshTable() 
    {
        tableModel.setRowCount(0);
        Collection<IGoal> goals = agent.scheduleStep(agent ->
        {
        	return agent.getFeature(IBDIAgentFeature.class).getGoals();
        }).get();
        
        for(IGoal goal : goals) 
        {
        	String[] row = new String[]{getShortedText(goal.getId()), getShortedText(goal.getType()), 
        		""+goal.getLifecycleState(), ""+goal.getProcessingState()};
            tableModel.addRow(row);
        }
    }
    
    protected String getShortedText(String text)
    {
    	int idx = text.lastIndexOf("$");
    	if(idx!=-1)
    	{
    		text = text.substring(idx+1);
    	}
    	else
    	{
	    	idx = text.lastIndexOf(".");
	    	if(idx!=-1)
	    		text = text.substring(idx+1);
    	}
    	return text;
    }
}