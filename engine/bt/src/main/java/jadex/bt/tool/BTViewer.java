package jadex.bt.tool;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jadex.bt.IBTAgentFeature;
import jadex.bt.impl.BTAgentFeature;
import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.INoCopyStep;

@SuppressWarnings("serial")
public class BTViewer extends JFrame 
{
	protected IComponentHandle agent;
	
	protected TreeNode root;
	
	protected BehaviorTreePanel btpanel;
	
	public BTViewer(IComponentHandle agent) 
	{
		this(agent, -1);
	}

	public BTViewer(IComponentHandle agent, int closedelay) 
	{
	    this.agent = agent;
	    setTitle("Behavior Tree Viewer " + agent.getId().getLocalName());
	    setSize(1000, 750);
//	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	    setLocationRelativeTo(null);

	    this.root = new TreeNode(getRoot(), getExecutionContext());
	    this.btpanel = new BehaviorTreePanel(root);
        add(btpanel);

		/*getRoot().addNodeListener(new NodeListener()
		{
			public void onChildAdded(Node parent, Node child, ExecutionContext context)
			{
				System.out.println("child added: "+child+" to "+parent);

				SwingUtilities.invokeLater(() -> refreshView(true));
			}
			
			public void onChildRemoved(Node	parent, Node child, ExecutionContext context)
			{
				System.out.println("child removed: "+child+" from "+parent);

				SwingUtilities.invokeLater(() -> refreshView(true));
			}
		});*/

		Timer	timer	= startAutoRefresh();

		// Close window on agent kill.
		agent.waitForTermination().then(b -> 
		{
			timer.stop();
			if(closedelay > 0)
			{
				Timer t = new Timer(closedelay, e -> 
				{
					if(timer!=null)
						timer.stop();
					BTViewer.this.dispose();
				});
				t.setRepeats(false);
				t.start();
			}
			else
			{
				dispose();
			}
		});
	}
	
	private Node<?> getRoot()
	{
		return agent.scheduleStep((INoCopyStep<Node<IComponent>>)a -> ((BTAgentFeature)a.getFeature(IBTAgentFeature.class)).getBehaviorTree()).get();
	}
	
	private ExecutionContext<?> getExecutionContext()
	{
		return agent.scheduleStep((INoCopyStep<ExecutionContext<?>>)a -> ((BTAgentFeature)a.getFeature(IBTAgentFeature.class)).getExecutionContext()).get();
	}
	
	private Timer	startAutoRefresh() 
    {
        Timer timer = new Timer(100, ev -> refreshView());
        timer.start();
        
		// Kill agent on window close.
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				agent.terminate();
			}
		});
		
		return timer;
    }
	

	private void refreshView(boolean treechanged)
	{
		//if(treechanged)
		//{
		try
		{
			ExecutionContext<?> context = getExecutionContext();
			this.root = new TreeNode(getRoot(), context);
			this.btpanel.addStep(this.root);
		}
		catch(Exception e)
		{
			//System.out.println("Exception during refresh: "+e);
		}
		//}
	
		btpanel.repaint();
		//btpanel.paintComponent(getGraphics());
	}

	private void refreshView()
	{
		refreshView(false);
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


