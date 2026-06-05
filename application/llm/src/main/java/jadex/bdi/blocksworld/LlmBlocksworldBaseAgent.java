package jadex.bdi.blocksworld;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.image.RenderedImage;

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

import jadex.core.IComponentHandle;
import jadex.core.IComponentManager;
import jadex.core.INoCopyStep;
import jadex.future.IFuture;
import jadex.future.IIntermediateFuture;
import jadex.micro.llmcall2.ChatFragment;
import jadex.micro.llmcall2.LlmChatAgent;
import jadex.micro.llmcall2.LlmHelper;
import jadex.micro.llmcall2.LlmHelper.Provider;

/**
 *  Interact with a blocksworld via natural language prompts.
 *  Base class for agents with shared functionality, not meant to be used directly.
 */
public abstract class LlmBlocksworldBaseAgent	extends BlocksworldAgent	implements IBlocksworldBaseService
{
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
	
	/**
	 *  Helper method to find a block, table or bucket by name. Case insensitive
	 */
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
	
	/**
	 *  Helper method to append text to a JTextPane with a specific style.
	 */
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
	
	/**
	 *  Run the agent with a simple gui to send prompts and show responses.
	 */
	protected static void	runGui(LlmBlocksworldBaseAgent pojo)
	{
		IComponentHandle	bwagent	= IComponentManager.get().create(pojo).get();
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
			think.setSelected(false);
			JCheckBox	sendimage	= new JCheckBox("Image");
			sendimage.setToolTipText("Include an image of the current world state in the prompt.");
//			sendimage.setSelected(true);
			JComboBox<String>	prompt	= new JComboBox<>(new String[] {
				"Move the red block onto the green one.",
				"Put all blocks in the bucket.",
				"Where is the yellow block?",
				"How many blocks are there?",
				"What is the color of block 1?",
				"What do you think about the current block arrangement and why?",
				"Please move some blocks around and describe what you are doing and why."
			});
			prompt.setEditable(true);
			JButton		send	= new JButton("Send");
			JButton		stop	= new JButton("Stop");
			stop.setEnabled(false);
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
			options.add(model, gbc);
			gbc.gridx = 2;
			gbc.weightx = 0;
			options.add(think, gbc);
			gbc.gridx = 3;
			options.add(sendimage, gbc);

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
			options.add(stop, gbc);
			
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
			
			LlmChatAgent[] llmagentpojo = new LlmChatAgent[1];

			ActionListener	al	= e ->
			{
				if(e.getSource()==prompt && !e.getActionCommand().equals("comboBoxEdited"))
					return;// Only react to combo box edits, not selection changes
				
				append(center, "User: "+prompt.getSelectedItem()+"\n", null);
				
				
				prompt.setEnabled(false);
				send.setEnabled(false);
				
//				System.out.println("Context size: "+
//					((Provider)provider.getSelectedItem()).getContextSize((String) model.getSelectedItem()));
				
				// Generate base64 encoded image of current world state
				Container	worlds	= (Container)gui.getContentPane().getComponent(0);
				Component	world	= 	((Container) ((Container) ((Container) worlds.getComponent(1)).getComponent(0)).getComponent(0)).getComponent(0);
				RenderedImage	image	= LlmHelper.createImageFromComponent(world);
				
				IIntermediateFuture<ChatFragment> chat	= llmagentpojo[0].chat((String)prompt.getSelectedItem(),
					sendimage.isSelected() ? new RenderedImage[]{image} : new RenderedImage[0]);
				
				chat.next(fragment ->
				{
					Style style = fragment.type()==ChatFragment.Type.THINKING ? thinking
						: fragment.type()==ChatFragment.Type.TOOL_CALL || fragment.type()==ChatFragment.Type.TOOL_RESULT ? toolcall
						: null;
					append(center, fragment.text(), style);
				})
					.finished(v -> {prompt.setEnabled(true); send.setEnabled(true); append(center, "\n==============\n", null);})
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
			
			ItemListener	il	= e ->
			{
				if(e.getStateChange()==java.awt.event.ItemEvent.SELECTED || e.getSource()==think)
				{
					center.setText("");
					IComponentHandle llmagent = IComponentManager.get().create(
						new LlmChatAgent(LlmHelper.createChatModel(
							(Provider)provider.getSelectedItem(),
							(String)model.getSelectedItem(),
							think.isSelected())
						)).get();
					llmagentpojo[0] = llmagent.getPojoHandle(LlmChatAgent.class);
				}
			};
			model.addItemListener(il);
			think.addItemListener(il);
			
			SwingUtilities.invokeLater(() ->fetchmodels.actionPerformed(null));
		});
		
		bwagent.waitForTermination().get();
	}
}
