package jadex.bdi.blocksworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import jadex.core.ChangeEvent.Type;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.LlmResultAgent;

public class LlmBlocksworldAgent	extends BlocksworldAgent	implements IBlocksworldService
{
	@Override
	public IFuture<String> getWorldState()
	{
		StringBuilder sb = new StringBuilder();
		for(Block block : blocks)
		{
			sb
				.append("'")
				.append(block)
				.append("'")
				.append(
					block.getLower()==table ? " is on the "
					: block.getLower()==bucket ? " is in the "
					: " is on ")
				.append("'")
				.append(block.getLower())
				.append("'")
				.append("\n");
		}
		if(table.getAllBlocks().length==0)
		{
			sb.append("The '"+table+"' is empty.\n");
		}
		if(bucket.getAllBlocks().length==0)
		{
			sb.append("The '"+bucket+"' is empty.\n");
		}
		
		for(Block block : blocks)
		{
			sb
				.append("'")
				.append(block)
				.append("'")
				.append(" has color ")
				.append(approximateColorName(block.getColor()))
				.append("\n");
		}
		return new Future<>(sb.toString());
	}
	
	@Override
	public IFuture<Void> move(String block1, String block2)
	{
		Block b1 = getBlock(block1);
		Block b2 = getBlock(block2);
		if(b1.getUpper()!=null)
			throw new IllegalStateException("'"+block1+"' is not clear.");
		if(b2.getUpper()!=null && b2!=table && b2!=bucket)
			throw new IllegalStateException("'"+block2+"' is not clear.");
		if(b1.getLower()==null)	// table and bucket cannot be moved
			throw new IllegalArgumentException("'"+block1+"' can not be moved.");
		if(b2.getLower()==bucket)	// Cannot move on block in bucket
			throw new IllegalArgumentException("'"+block2+"' is in the "+bucket);
		if(b1==b2)
			throw new IllegalArgumentException("Cannot move '"+block1+"' on itself.");
		b1.getLower().removeBlock(b1);
		b1.setLower(b2);
		b2.addBlock(b1);
		
		return IFuture.DONE;
	}
	
	protected Block	getBlock(String name)
	{
		if(name.toLowerCase().equals(table.toString().toLowerCase()))
			return table;
		if(name.toLowerCase().equals(bucket.toString().toLowerCase()))
			return bucket;
		for(Block block : blocks)
		{
			if(name.toLowerCase().equals(block.toString().toLowerCase()))
				return block;
		}
		throw new IllegalArgumentException("No '"+name+"' in world state.");
	}
	
	protected static String	approximateColorName(Color color)
	{
	    float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
	    float hue = hsb[0] * 360f;
	    float sat = hsb[1];
	    float bri = hsb[2];

	    if(bri < 0.12f) return "black";
	    if(bri < 0.22f) return "dark gray";
	    if(sat < 0.10f)
	    {
	        if(bri > 0.90f) return "white";
	        if(bri > 0.70f) return "light gray";
	        if(bri > 0.35f) return "gray";
	        return "dark gray";
	    }

	    String base;
	    if(hue < 15f || hue >= 345f) base = "red";
	    else if(hue < 45f) base = "orange";
	    else if(hue < 70f) base = "yellow";
	    else if(hue < 160f) base = "green";
	    else if(hue < 200f) base = "cyan";
	    else if(hue < 255f) base = "blue";
	    else if(hue < 290f) base = "purple";
	    else if(hue < 345f) base = "magenta";
	    else base = "red";

	    String tone = bri < 0.35f ? "dark " : bri > 0.80f ? "light " : "";
	    String vivid = sat > 0.85f && bri > 0.45f ? "vivid " : sat < 0.35f ? "muted " : "";

	    return (tone + vivid + base).trim();
	}

	protected static void	append(JTextPane pane, String value, Style style)
	{
		try {
			pane.getStyledDocument().insertString(pane.getStyledDocument().getLength(), value, style);
			pane.setCaretPosition(pane.getStyledDocument().getLength());
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		IComponentHandle	bwagent	= IComponentManager.get().create(new LlmBlocksworldAgent()).get();
		BlocksworldGui	gui	= bwagent.scheduleAsyncStep(
			(INoCopyStep<IFuture<BlocksworldGui>>) comp -> ((BlocksworldAgent)comp.getPojo()).gui).get();
		
		// Create simple gui to send a prompt
		SwingUtilities.invokeLater(() ->
		{
			JTextPane	center	= new JTextPane();
			center.setEditable(false);
			JPanel	bottom	= new JPanel(new BorderLayout());
			JComboBox<String>	prompt	= new JComboBox<>(new String[] {
				"Move the red block onto the green one.",
				"Put all blocks in the bucket.",
				"Where is the yellow block?",
				"How many blocks are there?",
				"What is the color of block 1?",
				"Describe the current world state.",
				"What do you think about the current world state?",
				"Please move some blocks around and describe what you are doing and why."
			});
			prompt.setEditable(true);
			JButton		send	= new JButton("Send");
			bottom.add(prompt, BorderLayout.CENTER);
			bottom.add(send, BorderLayout.EAST);
			
			JPanel	panel	= new JPanel(new BorderLayout());
			panel.add(new JScrollPane(center), BorderLayout.CENTER);
			panel.add(bottom, BorderLayout.SOUTH);
			panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Chat with Agent"));
			
			Style thinking = center.getStyledDocument().addStyle("thinking", null);
			StyleConstants.setItalic(thinking, true);
			Style toolcall = center.getStyledDocument().addStyle("toolcall", null);
			StyleConstants.setBold(toolcall, true);
			
			ActionListener	al	= e ->
			{
				if(e.getSource()==prompt && !e.getActionCommand().equals("comboBoxEdited"))
					return;// Only react to combo box edits, not selection changes
				
				append(center, "User: "+prompt.getSelectedItem()+"\n", null);
				
				prompt.setEnabled(false);
				send.setEnabled(false);
				IComponentHandle llmagent = IComponentManager.get().create(
					new LlmResultAgent(LlmHelper.createChatModel(), (String) prompt.getSelectedItem())).get();
				
				int[] last = new int[] {0};
				llmagent.subscribeToResults().next(event ->
				{
					if(event.type()==Type.ADDED && event.name().equals("response"))
					{
						if(last[0]!=1)
						{
							append(center, "\n", null);
							last[0]=1;
						}
						append(center, ""+event.value(), null);
					}
					else if(event.type()==Type.ADDED && event.name().equals("thinking"))
					{
						if(last[0]!=2)
						{
							append(center, "\n", thinking);
							last[0]=2;
						}
						append(center, ""+event.value(), thinking);
					}
					else if(event.type()==Type.ADDED && event.name().equals("toolcalls"))
					{
						if(last[0]!=3)
						{
							append(center, "\n", toolcall);
							last[0]=3;
						}
						append(center, ""+event.value(), toolcall);
					}
					else if(event.type()==Type.ADDED && event.name().equals("toolresults"))
					{
						if(last[0]!=4)
						{
							append(center, "\n", toolcall);
							last[0]=4;
						}
						append(center, ""+event.value(), toolcall);
					}
				}).printOnEx();
				
				llmagent.waitForTermination()
					.then(v -> {prompt.setEnabled(true); send.setEnabled(true); append(center, "\n==============\n", null);})
					.catchEx(v -> {prompt.setEnabled(true); send.setEnabled(true); append(center, "\n==============\n", null);})
					.printOnEx();
			};
			prompt.addActionListener(al);
			send.addActionListener(al);
			
			Container	worlds	= (Container)gui.getContentPane().getComponent(0);
			worlds.remove(worlds.getComponent(0));
			worlds.add(panel, 0);
			worlds.validate();
			worlds.repaint();
		});
		
		bwagent.waitForTermination().get();
	}
}
