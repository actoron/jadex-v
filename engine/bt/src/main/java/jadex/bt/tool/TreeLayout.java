package jadex.bt.tool;

import java.util.ArrayList;
import java.util.List;

public class TreeLayout 
{
    public static final int levelGap = 75;    
    public static final int defaultWidth = 150; 
    public static final int defaultHeight = 75; 
    public static final int decoHeight = 20; 

    public static void calculatePositions(TreeNode root, int width) 
    {
    	//System.out.println("calculatePositions: "+root+" "+width);
        calculateNodeSize(root);
        distributeNodesByLevel(root, width);
        //System.out.println("calculatePositions end: "+root);
    }

    private static void calculateNodeSize(TreeNode node) 
    {
        int maxheight = defaultHeight;  
        int width = defaultWidth; 

        if (!node.getDecorators().isEmpty()) 
            maxheight += node.getDecorators().size() * decoHeight; 

        node.setWidth(width);
        node.setHeight(maxheight);

        for (TreeNode child : node.getChildren()) 
            calculateNodeSize(child);
    }

    private static void distributeNodesByLevel(TreeNode root, int width) 
    {
        List<List<TreeNode>> levels = new ArrayList<>();
        collectNodesByLevel(root, 0, levels);

        int yoffset = 20;
        for (List<TreeNode> level : levels) 
        {
            int numnodes = level.size();
            if (numnodes == 0) continue;

            int nodewidth = numnodes * defaultWidth;
            int gapwidth = (width - nodewidth) / (numnodes + 1);
            int xoffset = gapwidth;

            for (TreeNode node : level) 
            {
                node.setX(xoffset);
                node.setY(yoffset);
                xoffset += defaultWidth + gapwidth;
            }

            yoffset += getMaxHeightForLevel(level) + levelGap;
        }
    }

    private static void collectNodesByLevel(TreeNode node, int level, List<List<TreeNode>> levels) 
    {
        if (levels.size() <= level) 
            levels.add(new ArrayList<>());

        levels.get(level).add(node);

        for (TreeNode child : node.getChildren()) 
            collectNodesByLevel(child, level + 1, levels);
    }

    private static int getMaxHeightForLevel(List<TreeNode> levelNodes) 
    {
        int maxHeight = 0;
        for (TreeNode node : levelNodes) 
            maxHeight = Math.max(maxHeight, node.getHeight());
        return maxHeight;
    }
}
