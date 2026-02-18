package jadex.bt.tool;

import java.util.ArrayList;
import java.util.List;

import jadex.bt.decorators.Decorator;
import jadex.bt.decorators.IDecorator;
import jadex.bt.nodes.CompositeNode;
import jadex.bt.nodes.Node;
import jadex.bt.nodes.Node.NodeState;
import jadex.bt.state.ExecutionContext;
import jadex.bt.state.NodeContext;

public class TreeNode 
{
	protected int x = -1, y = -1, width = 100, height = 50;

	protected String name;

	protected int id;

	protected String type;

	protected List<TreeNode> children = new ArrayList<>();

	protected List<DecoratorInfo> decorators = new ArrayList<>();

	protected NodeState state;

	protected List<String> details;

	public record DecoratorInfo(String name, Decorator<?> deco) 
	{
		public DecoratorInfo(String name) 
		{
			this(name, null);
		}

		public String getDetails() 
		{
			if (deco != null && deco.getDetails() != null)
				return name + " " + deco.getDetails();
			else
				return name;
		}
	}

	public TreeNode(String name, String type) 
	{
		this.name = name;
		this.type = type;
	}

	public TreeNode(Node<?> node, ExecutionContext<?> context) 
	{
		this.name = node.getName();
		this.type = node.getType();
		this.id = node.getId();

		if (node instanceof CompositeNode<?>) 
		{
			List<Node<?>> childs = new ArrayList<Node<?>>();
			childs.addAll(((CompositeNode<?>) node).getChildren((ExecutionContext)context));
			for(Node<?> child : childs) 
			{
				addChild(new TreeNode(child, context));
			}
		}
		//System.out.println("nodes size: "+this.getNodeCount()+" "+node+" context: "+context.hashCode());

		List<IDecorator<?>> decos = (List) node.getDecorators();
		for (IDecorator<?> deco : decos) 
		{
			if (!(deco instanceof Node))
				addDecorator(new DecoratorInfo(deco.getType(), (Decorator<?>) deco));
		}

		if (context != null) 
		{
			NodeContext<?> ct = (NodeContext<?>) context.getNodeContext((Node)node);
			if (ct != null) 
			{
				this.state = ct.getState();
				this.details = node.getDetailsShort((ExecutionContext)context);
			}
		}

	}

	public TreeNode(TreeNode node) 
	{
		this.name = node.getName();
		this.type = node.getType();
		this.id = node.getId();
		this.x = node.getX();
		this.y = node.getY();
		this.width = node.getWidth();
		this.height = node.getHeight();
		this.details = node.getDetailsShort();
		this.state = node.getState();

		for (TreeNode child : node.getChildren()) 
		{
			addChild(new TreeNode(child));
		}

		List<DecoratorInfo> decos = node.getDecorators();
		if (decos != null && decos.size() > 0)
			setDecorators(new ArrayList<>(decos));
	}

	public int getX() 
	{
		return x;
	}

	public TreeNode setX(int x) 
	{
		// System.out.println("x: "+x+" "+this);
		this.x = x;
		return this;
	}

	public int getY() 
	{
		return y;
	}

	public TreeNode setY(int y) 
	{
		this.y = y;
		return this;
	}

	public int getWidth() 
	{
		return width;
	}

	public TreeNode setWidth(int width) 
	{
		this.width = width;
		return this;
	}

	public int getHeight() 
	{
		return height;
	}

	public TreeNode setHeight(int height) 
	{
		this.height = height;
		return this;
	}

	public String getName() 
	{
		return name;
	}

	public TreeNode setName(String label) 
	{
		this.name = label;
		return this;
	}

	public String getType() 
	{
		return type;
	}

	public TreeNode setType(String type) 
	{
		this.type = type;
		return this;
	}

	public List<TreeNode> getChildren() 
	{
		return children;
	}

	public TreeNode setChildren(List<TreeNode> children) 
	{
		this.children = children;
		return this;
	}

	public void addChild(TreeNode child) 
	{
		children.add(child);
	}

	public TreeNode getChild(int idx) 
	{
		return children.get(idx);
	}

	public int getNodeCount() 
	{
		int cnt = 1;
		for (TreeNode child : children) 
		{
			cnt += child.getNodeCount();
		}
		return cnt;
	}

	public List<DecoratorInfo> getDecorators() 
	{
		return decorators;
	}

	public TreeNode setDecorators(List<DecoratorInfo> decorators) 
	{
		this.decorators = decorators;
		return this;
	}

	public void addDecorator(DecoratorInfo decorator) 
	{
		decorators.add(decorator);
	}

	public int getId() 
	{
		return id;
	}

	public TreeNode setId(int id) 
	{
		this.id = id;
		return this;
	}

	public NodeState getState() 
	{
		return state;
	}

	/*
	 * public NodeContext<?> getContext()
	 * {
	 * return context;
	 * }
	 */

	public String getLabel() 
	{
		String ret = getName();
		if (getId() != -1)
			ret += " (" + getId() + ")";
		return ret;
	}

	public List<String> getDetailsShort() 
	{
		return details;
	}

	public boolean treeEquals(TreeNode other) 
	{
		if (this == other)
			return true;

		if (other == null)
			return false;

		if (this.id != other.id)
			return false;

		if (this.children.size() != other.children.size())
			return false;

		for (int i = 0; i < this.children.size(); i++) 
		{
			if (!this.children.get(i).treeEquals(other.children.get(i)))
				return false;
		}

		return true;
	}

	public void copyPositions(TreeNode old) 
	{
		if (old.getId() != getId())
			throw new RuntimeException("Cannot copy position when node/tree differs: " + old + " " + this);

		setX(old.getX());
		setY(old.getY());
		setWidth(old.getWidth());
		setHeight(old.getHeight());

		for (int i = 0; i < children.size(); i++) {
			children.get(i).copyPositions(old.getChild(i));
		}

		// System.out.println("copied: "+this+" "+x+" "+y);
	}
}
