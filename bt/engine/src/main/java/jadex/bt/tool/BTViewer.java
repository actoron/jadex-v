package jadex.bt.tool;

import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jadex.bt.impl.BTAgentFeature;
import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingFunction;
import jadex.micro.impl.MicroAgentFeature;

public class BTViewer extends JFrame 
{
	protected IComponentHandle agent;
	
	protected TreeNode root;
	
	protected BehaviorTreePanel btpanel;
	
	public BTViewer(IComponentHandle agent) 
	{
	    this.agent = agent;
	    setTitle("Behavior Tree Viewer " + agent.getId().getLocalName());
	    setSize(800, 600);
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setLocationRelativeTo(null);

	    this.root = new TreeNode(getRoot(), getExecutionContext());
	    this.btpanel = new BehaviorTreePanel(root);
        add(btpanel);
        
	    startAutoRefresh();
	}
	
	private Node<?> getRoot()
	{
		return agent.scheduleStep((IThrowingFunction<IComponent, Node<IComponent>>)a -> ((BTAgentFeature)a.getFeature(MicroAgentFeature.class)).getBehaviorTree()).get();
	}
	
	private ExecutionContext<?> getExecutionContext()
	{
		return agent.scheduleStep((IThrowingFunction<IComponent, ExecutionContext<?>>)a -> ((BTAgentFeature)a.getFeature(MicroAgentFeature.class)).getExecutionContext()).get();
	}
	
	private void startAutoRefresh() 
    {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() 
        {
            @Override
            public void run() 
            {
                SwingUtilities.invokeLater(BTViewer.this::refreshView);
            }
        }, 0, 100);
    }
	
	private void refreshView()
	{
		ExecutionContext<?> context = getExecutionContext();
		this.root = new TreeNode(getRoot(), context);
		this.btpanel.addStep(this.root);
		
		btpanel.repaint();
		//btpanel.paintComponent(getGraphics());
	}
	
    public static void main(String[] args) 
    {
        SwingUtilities.invokeLater(() -> 
        {
            JFrame frame = new JFrame("Behavior Tree Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.add(new BehaviorTreePanel(null));
            frame.setVisible(true);
        });
    }
}


