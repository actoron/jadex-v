package jadex.bt.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import jadex.bt.nodes.Node.NodeState;
import jadex.bt.tool.TreeNode.DecoratorInfo;
import jadex.common.SUtil;

public class BehaviorTreePanel extends JPanel 
{
    private TreeNode root;  
    private TreeNode dragnode = null;
    private int offsetx, offsety;
    
    private List<TreeNode> history = new ArrayList<>();
    private int currentstep = -1;
    private boolean replay = false;
    
    private JButton stop, forward, back;
    private JSlider slider;
    private JLabel steplabel;
    private JButton clear;
    
    public BehaviorTreePanel(TreeNode root) 
    {
    	 this.root = root==null? createSampleTree(): root;  
         
         //playButton = new JButton("Play");
         stop = new JButton("Stop");
         forward = new JButton(">");
         back = new JButton("<");
         slider = new JSlider(0, 0, 0);
         steplabel = new JLabel("Step: 0");
         
         //playButton.addActionListener(e -> playReplay());
         stop.addActionListener(e -> toggleReplayMode());
         forward.addActionListener(e -> replayStep(currentstep + 1));
         back.addActionListener(e -> replayStep(currentstep - 1));
         slider.addChangeListener(e -> replayStep(slider.getValue()));

         //playButton.setEnabled(false);
         stop.setEnabled(true);
         forward.setEnabled(false);
         back.setEnabled(false);
         slider.setEnabled(false);
         
         clear = new JButton("Clear");
         clear.addActionListener(e -> clearHistory());

         JPanel buttonPanel = new JPanel();
         buttonPanel.add(back);
         //buttonPanel.add(playButton);
         buttonPanel.add(stop);
         buttonPanel.add(clear);
         buttonPanel.add(forward);
         buttonPanel.add(slider);
         buttonPanel.add(steplabel);

         setLayout(new BorderLayout());
         add(buttonPanel, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() 
        {
            public void mousePressed(MouseEvent e) 
            {
                for (TreeNode node : getAllNodes(getRoot())) 
                {
                    if (isInsideFullNode(node, e.getX(), e.getY())) 
                    {
                        dragnode = node;
                        offsetx = e.getX() - node.getX();
                        offsety = e.getY() - node.getY();
                        System.out.println("Selected Node: " + node.getLabel());
                        return;
                    }
                }
            }

            public void mouseReleased(MouseEvent e) 
            {
                dragnode = null;
                System.out.println("Dragging stopped.");
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() 
        {
            public void mouseDragged(MouseEvent e) 
            {
                if (dragnode != null) 
                {
                    dragnode.setX(e.getX() - offsetx);
                    dragnode.setY(e.getY() - offsety);
                    //System.out.println("Dragging " + draggingNode.getLabel() + " to " + draggingNode.getX() + ", " + draggingNode.getY());
                    System.out.println("draggingNode aktuell: " + (dragnode != null ? dragnode.getLabel() : "null"));
                    repaint();
                }
            }
        });
        
        addComponentListener(new ComponentAdapter() 
        {
            @Override
            public void componentResized(ComponentEvent e) 
            {
            	if (dragnode == null) 
            	{ 
                    TreeLayout.calculatePositions(BehaviorTreePanel.this.root, getWidth());
                    repaint();
                }
            }
        });
    }
    
    public TreeNode getRoot()
    {
    	return root;
    }
    
    private void setRoot(TreeNode root)
    {
    	//System.out.println("changed root: "+root);
    	
    	if(dragnode!=null)
    		return;// false;
    	
    	this.root = root;
    	
    	/*if(force)
    	{
    		this.root = root;
    	}
    	else if(root.treeEquals(getRoot()))
    	{
    		root.copyPositions(getRoot());
    		this.root = root;
    	}
    	else
    	{
    		TreeLayout.calculatePositions(root, getWidth()); 
    		this.root = root;
    	}
    	
    	return true;*/
    }

    private boolean isInsideFullNode(TreeNode node, int x, int y) 
    {
        int totalHeight = node.getHeight() + (node.getDecorators().size() * 20);
        return x >= node.getX() && x <= node.getX() + node.getWidth() &&
               y >= node.getY() && y <= node.getY() + totalHeight;
    }

    private TreeNode createSampleTree() 
    {
        TreeNode root = new TreeNode("Root", "SEQUENCE");
        TreeNode child1 = new TreeNode("Selector", "SELECTOR");
        TreeNode child2 = new TreeNode("Parallel", "PARALLEL");
        TreeNode action1 = new TreeNode("Action1", "ACTION");
        TreeNode action2 = new TreeNode("Action2", "ACTION");

        child1.getChildren().add(action1);
        child2.getChildren().add(action2);
        root.getChildren().add(child1);
        root.getChildren().add(child2);

        child1.getDecorators().add(new DecoratorInfo("INVERTER"));
        child2.getDecorators().add(new DecoratorInfo("REPEATER"));

        return root;
    }

    private List<TreeNode> getAllNodes(TreeNode node) 
    {
        List<TreeNode> allNodes = new ArrayList<>();
        allNodes.add(node);
        for (TreeNode child : node.getChildren()) 
        {
            allNodes.addAll(getAllNodes(child)); 
        }
        return allNodes;
    }

    protected void paintComponent(Graphics g) 
    {
        super.paintComponent(g);
        drawTree(g, root);
    }
    
    private void drawTree(Graphics g, TreeNode node) 
    {
        //System.out.println("Draw: "+node.getX()+" "+node.getY()+" "+node.getWidth()+" "+node.getHeight());

        for (TreeNode child : node.getChildren()) 
        {
            g.drawLine(node.getX() + node.getWidth() / 2, node.getY() + node.getHeight(),
            	child.getX() + child.getWidth() / 2, child.getY());
            drawTree(g, child);
        }

        int yoffset = node.getY();

        for (DecoratorInfo decorator : node.getDecorators()) 
        {
            g.setColor(Color.LIGHT_GRAY);
            g.fillRect(node.getX(), yoffset, node.getWidth(), TreeLayout.decoHeight);
            g.setColor(Color.BLACK);
            g.drawRect(node.getX(), yoffset, node.getWidth(), TreeLayout.decoHeight);
            g.drawString(decorator.getDetails(), node.getX() + 5, yoffset + 15);
            yoffset += TreeLayout.decoHeight;  
        }

        g.setColor(getNodeColor(node.getType()));
        g.fillRect(node.getX(), yoffset, node.getWidth(), node.getHeight() - yoffset + node.getY()); 
        g.setColor(Color.BLACK);
        g.drawRect(node.getX(), yoffset, node.getWidth(), node.getHeight() - yoffset + node.getY()); 
        g.drawString(node.getLabel(), node.getX() + 5, yoffset + 15); 
        
        List<String> details = node.getDetailsShort();
        if(details!=null)
        {
        	for(int i=0; i<details.size(); i++)
        		g.drawString(details.get(i), node.getX() + 5, yoffset + 15*(i+1)+15);
        }
        
        int wx = 20;
        int wy = 10;
        int statusx = node.getX() + node.getWidth() - wx;  
        int statusy = node.getY() - wy;  

        g.setColor(getStatusColor(node.getState()));
        g.fillRect(statusx, statusy, wx, wy);
        g.setColor(Color.BLACK);
        g.drawRect(statusx, statusy, wx, wy);
        
        drawNodeTypeIcon(g, node);
    }
    
    private void drawNodeTypeIcon(Graphics g, TreeNode node)
    {
        int iconsize = 10;
        int iconx = node.getX() + 5; 
        int icony = node.getY() - 5;

        if ("SEQUENCE".equals(node.getType().toUpperCase())) 
        {
        	 g.setColor(Color.BLACK);
        	 drawArrowLine(g, iconx, icony, iconx+iconsize, icony, 4, 4);
        }
        else if ("SELECTOR".equals(node.getType().toUpperCase())) 
        {
        	g.setColor(Color.BLACK);
        	g.drawString("?", iconx, icony);
        }
        else if ("PARALLEL".equals(node.getType().toUpperCase())) 
        {
            g.setColor(Color.BLACK);
            drawArrowLine(g, iconx, icony-2, iconx+iconsize, icony-2, 3, 3);
       	 	drawArrowLine(g, iconx, icony+2, iconx+iconsize, icony+2, 3, 3);
        }
        else if ("ACTION".equals(node.getType().toUpperCase())) 
        {
            g.setColor(Color.BLACK);
            g.drawPolygon(new int[] {iconx, iconx + iconsize / 2, iconx + iconsize}, 
                          new int[] {icony, icony - iconsize, icony}, 3);  
        }
    }
    
    private void drawArrowLine(Graphics g, int x1, int y1, int x2, int y2, int d, int h) 
    {
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - d, xn = xm, ym = h, yn = -h, x;
        double sin = dy / D, cos = dx / D;

        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;

        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;

        int[] xpoints = {x2, (int) xm, (int) xn};
        int[] ypoints = {y2, (int) ym, (int) yn};

        g.drawLine(x1, y1, x2, y2);
        g.fillPolygon(xpoints, ypoints, 3);
    }

    private Color getStatusColor(NodeState state) 
    {
    	if(NodeState.SUCCEEDED==state)
    		return Color.GREEN;
    	else if(NodeState.RUNNING==state)
    		return Color.YELLOW;
    	else if(NodeState.FAILED==state)
    		return Color.RED;
    	else
    		return Color.GRAY;
    }

    private Color getNodeColor(String type) 
    {
        switch (type.toUpperCase()) 
        {
            case "SEQUENCE": return Color.YELLOW;
            case "SELECTOR": return Color.GREEN;
            case "PARALLEL": return Color.MAGENTA;
            case "ACTION": return Color.ORANGE;
            default: return Color.GRAY;
        }
    }
    
    public void addStep(TreeNode root) 
    {
    	if(root.getX()==-1)
    	{
    		if(history.size()>0 && root.treeEquals(history.get(history.size()-1)))
        	{
        		root.copyPositions(history.get(history.size()-1));
        	}
        	else
        	{
        		TreeLayout.calculatePositions(root, getWidth()); 
        	}
    	}
    	
        history.add(new TreeNode(root));
       
        updateStepLabel();
        if (!replay) 
        {
        	slider.setMaximum(history.size() - 1);
            //slider.setEnabled(true);
        	setRoot(root);
        	currentstep = history.size() - 1;
        	slider.setValue(currentstep);
        }
    }

    public void replayStep(int step) 
    {
        if (step >= 0 && step < history.size()) 
        {
            currentstep = step;
            setRoot(history.get(step));
            slider.setValue(step);
            updateStepLabel();
        }
    }
    
    private void toggleReplayMode() 
    {
    	setReplayMode(!replay);
        /*replay = !replay;
        //playButton.setEnabled(replay);
        forward.setEnabled(replay);
        back.setEnabled(replay);
        stop.setEnabled(true);
        stop.setText(!replay? "Stop": "Resume");*/
    }
    
    private void setReplayMode(boolean replay) 
    {
        this.replay = replay;
        //playButton.setEnabled(replay);
        forward.setEnabled(replay);
        back.setEnabled(replay);
        stop.setText(!replay? "Stop": "Resume");
        slider.setEnabled(replay);
    }
    
    /*private void playReplay() 
    {
        replay = true;
        //playButton.setEnabled(false);
        stopButton.setEnabled(true);
        stepForwardButton.setEnabled(false);
        stepBackwardButton.setEnabled(false);
        
        new Thread(() -> 
        {
            while (replay && currentstep < history.size() - 1) 
            {
                replayStep(currentstep + 1);
                SUtil.sleep(500);
            }
        }).start();
    }*/
    
    private void updateStepLabel() 
    {
        steplabel.setText("Step: " + (replay? currentstep : (history.size() - 1)));
    }
    
    private void clearHistory() 
    {
        history.clear();
        currentstep = -1;
        setReplayMode(false);
        slider.setMaximum(0);
        slider.setValue(0);
        updateStepLabel();
    }
}