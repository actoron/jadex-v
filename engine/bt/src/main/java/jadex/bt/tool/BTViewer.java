package jadex.bt.tool;

import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import jadex.bt.IBTAgentFeature;
import jadex.bt.NodeListener;
import jadex.bt.impl.BTAgentFeature;
import jadex.bt.nodes.Node;
import jadex.bt.state.ExecutionContext;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.IThrowingFunction;

public class BTViewer extends JFrame 
{
	protected IComponentHandle agent;
	
	protected TreeNode root;
	
	protected BehaviorTreePanel btpanel;
	
	public BTViewer(IComponentHandle agent) 
	{
		this(agent, -1);
	}

	public BTViewer(IComponentHandle agent, long closedelay) 
	{
	    this.agent = agent;
	    setTitle("Behavior Tree Viewer " + agent.getId().getLocalName());
	    setSize(800, 600);
	    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    setLocationRelativeTo(null);

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

		Timer timer = startAutoRefresh();

		if(closedelay >= 0)
		{
			Runnable dispose = () -> 
			{
				Timer t = new Timer(3000, e -> 
				{
					if(timer!=null)
						timer.stop();
					BTViewer.this.dispose();
				});
				t.setRepeats(false);
				t.start();
			};
			agent.waitForTermination().then(found ->
			{
				dispose.run();
			}).catchEx(ex -> 
			{
				dispose.run();
			});
		}
	}
	
	private Node<?> getRoot()
	{
		return agent.scheduleStep((IThrowingFunction<IComponent, Node<IComponent>>)a -> ((BTAgentFeature)a.getFeature(IBTAgentFeature.class)).getBehaviorTree()).get();
	}
	
	private ExecutionContext<?> getExecutionContext()
	{
		return agent.scheduleStep((IThrowingFunction<IComponent, ExecutionContext<?>>)a -> ((BTAgentFeature)a.getFeature(IBTAgentFeature.class)).getExecutionContext()).get();
	}
	
	private Timer startAutoRefresh() 
    {
		Timer timer = new Timer(100, e -> 
		{
			try 
			{
				refreshView();
			} 
			catch (Exception ex) 
			{
				((Timer)e.getSource()).stop();
			}
		});
		timer.setInitialDelay(0);
		timer.start();

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


