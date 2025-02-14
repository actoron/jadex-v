package jadex.bdi.marsworld.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
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
        setLocationRelativeTo(null);
        
        tableModel = new DefaultTableModel(new String[]{"ID", "Type", "Lifecycle State", "Processing State", "Pojo"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.getColumnModel().getColumn(4).setCellRenderer(new PojoCellRenderer());
        
        table.addMouseListener(new MouseAdapter() 
        {
        	@Override
            public void mouseClicked(MouseEvent e) 
        	{
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());

                if (col == 4) 
                { 
                    Object pojo = tableModel.getValueAt(row, col);
                    String id = (String)tableModel.getValueAt(row, 1);
                    if (pojo != null) 
                    {
                        JFrame frame = ObjectInspectorPanel.createObjectFrame(id, pojo);
                        frame.setLocationRelativeTo(null);
                        frame.setVisible(true);
                    }
                }
            }
        });
        
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
        }, 0, 100);
    }

    private void refreshTable() 
    {
        tableModel.setRowCount(0);
        IGoal[] goals = agent.scheduleStep(agent ->
        {
        	return agent.getFeature(IBDIAgentFeature.class).getGoals().toArray(new IGoal[0]);
        }).get();
        
        for(IGoal goal : goals) 
        {
        	Object[] row = new Object[]
        	{
	            getShortedText(goal.getId()), 
	            getShortedText(goal.getType()), 
	            goal.getLifecycleState(), 
	            goal.getProcessingState(), 
	            goal.getPojo()
            };
            tableModel.addRow(row);
        }
    }
    
    public static String getShortedText(String text)
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
    
    public static class PojoCellRenderer extends DefaultTableCellRenderer 
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (value != null) 
            {
                setText(getShortedText(""+value));  
            } 
            else 
            {
                setText("");
            }
            return this;
        }

    }
}