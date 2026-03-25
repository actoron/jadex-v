package jadex.bdi.blocksworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import dev.langchain4j.data.image.Image;
import jadex.core.ChangeEvent.Type;
import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.LlmHelper.Provider;
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
			
			JComboBox<Provider>	provider	= new JComboBox<>(Provider.values());
			JComboBox<String>	model		= new JComboBox<>();
			JCheckBox	think	= new JCheckBox("Think");
			think.setToolTipText("Show the thinking process of the agent, if supported by the model.");
			think.setSelected(true);
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
			JButton		stop	= new JButton("Stop");
			JCheckBox	sendimage	= new JCheckBox("Image");
			sendimage.setToolTipText("Include an image of the current world state in the prompt.");
			sendimage.setSelected(true);
			// reduce width of buttons
			send.setMargin(new java.awt.Insets(0,0,0,0));
			stop.setMargin(new java.awt.Insets(0,0,0,0));

			JPanel	options = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new java.awt.Insets(4, 2, 4, 2);
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.WEST;

			// Row 0: Provider label + combo
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 0;
			options.add(new JLabel("Provider"), gbc);
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth	= GridBagConstraints.REMAINDER;
			options.add(provider, gbc);

			// Row 1: Model label + combo
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.weightx = 0;
			gbc.gridwidth	= 1;
			options.add(new JLabel("Model"), gbc);
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth	= 2;
			options.add(model, gbc);
			gbc.gridx = 3;
			gbc.weightx = 0;
			gbc.gridwidth	= 1;
			options.add(think, gbc);

			// Row 2: Prompt expands, buttons stay compact
			gbc.gridx = 0;
			gbc.gridy = 2;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			options.add(prompt, gbc);
			gbc.gridx = 2;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			options.add(send, gbc);
			gbc.gridx = 3;
			options.add(sendimage, gbc);
			
			JPanel	panel	= new JPanel(new BorderLayout());
			panel.add(new JScrollPane(center), BorderLayout.CENTER);
			panel.add(options, BorderLayout.SOUTH);
			panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Chat with Agent"));
			
			Style thinking = center.getStyledDocument().addStyle("thinking", null);
			StyleConstants.setItalic(thinking, true);
			Style toolcall = center.getStyledDocument().addStyle("toolcall", null);
			StyleConstants.setBold(toolcall, true);
			
			ActionListener	fetchmodels	= e ->
			{
				Cursor oldCursor = gui.getCursor();
				try
				{
					gui.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));					
					Provider selected = (Provider) provider.getSelectedItem();
					model.removeAllItems();
					for(String m : selected.getModels())
						model.addItem(m);
					
					if(LlmHelper.DEFAULT_MODELS.get(selected)!=null)
						model.setSelectedItem(LlmHelper.DEFAULT_MODELS.get(selected));
				}
				finally
				{
					gui.setCursor(oldCursor);					
				}
			};
			provider.addActionListener(fetchmodels);
			
			ActionListener	al	= e ->
			{
				if(e.getSource()==prompt && !e.getActionCommand().equals("comboBoxEdited"))
					return;// Only react to combo box edits, not selection changes
				
				append(center, "User: "+prompt.getSelectedItem()+"\n", null);
				
				// Generate base64 encoded image of current world state and add to prompt
				Container	worlds	= (Container)gui.getContentPane().getComponent(0);
				Component	world	= 	((Container) ((Container) ((Container) worlds.getComponent(1)).getComponent(0)).getComponent(0)).getComponent(0);
				String image = LlmHelper.createPngFromComponent(world);
				Image	png	= Image.builder().base64Data(image).mimeType("image/png").build();
				
				prompt.setEnabled(false);
				send.setEnabled(false);
				IComponentHandle llmagent = IComponentManager.get().create(
					new LlmResultAgent(
						LlmHelper.createChatModel(
							(Provider)provider.getSelectedItem(),
							(String)model.getSelectedItem(),
							think.isSelected()),
						(String)prompt.getSelectedItem()
						, sendimage.isSelected() ? new Image[]{png} : new Image[0]
					)).get();
				
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
			
			SwingUtilities.invokeLater(() ->fetchmodels.actionPerformed(null));
		});
		
		bwagent.waitForTermination().get();
	}
}
