package jadex.bdi.blocksworld;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.image.RenderedImage;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import io.github.raghultech.markdown.swing.integration.SwingMarkdownRenderer;
import jadex.micro.llmcall2.CodeHighlighter;
import jadex.micro.llmcall2.ChatScrollPane;
import jadex.micro.llmcall2.MathRenderer;
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
	/**
	 *  Overridden to explicitly number the blocks
	 *  thus avoiding static counter issues when running benchmark repeatedly.
	 */
	public void agentCreated()
	{
		Block b0 = new Block(1, new Color(240, 16, 16), table);
		Block b1 = new Block(2, new Color(64, 96, 255), table);
		Block b2 = new Block(3, new Color(240, 240, 16), b0);
		blocks.add(b0);
		blocks.add(b1);
		blocks.add(b2);
		blocks.add(new Block(4, new Color(16, 240, 16), b2));
		blocks.add(new Block(5, new Color(240, 16, 240), bucket));
		blocks.add(new Block(6, new Color(16, 240, 240), bucket));
		blocks.add(new Block(7, new Color(240, 240, 240), bucket));

		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				new BlocksworldGui(agent.getComponentHandle());
			}
		});
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
	 *  Render a Markdown text to Swing-compatible HTML, same pipeline
	 *  as the LlmChatAgentGui / LlmChatPanel: TeX math is rasterized
	 *  first, then Flexmark GFM (tables, task lists, code fences) turns
	 *  it into HTML, and fenced code blocks get token-colored syntax
	 *  highlighting.
	 *  @param markdown The Markdown source.
	 *  @return The Swing-compatible HTML.
	 */
	protected static String	renderMarkdown(String markdown)
	{
		String	processed	= MathRenderer.preprocess(markdown);
		String	html		= SwingMarkdownRenderer.getInstance().renderMarkdown(processed);
		return CodeHighlighter.highlightBlocks(html);
	}
	
	/**
	 *  Escape text for HTML.
	 */
	protected static String	escapeHtml(String text)
	{
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
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
			// The chat view: a JEditorPane rendering LLM output and user input
			// as HTML/Markdown, same as the LlmChatAgentGui panel.
			JEditorPane	chat	= new JEditorPane("text/html", "");
			chat.setEditable(false);
			{
				HTMLEditorKit	kit	= new HTMLEditorKit();
				StyleSheet		sheet	= kit.getStyleSheet();
				sheet.addRule("body { font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif; font-size: 11px; line-height: 1.5; letter-spacing: 0; color: #1a1a2e; }");
				sheet.addRule("h1 { font-size: 1.5em; font-weight: bold; color: #111; margin: 1em 0 0.5em; }");
				sheet.addRule("h2 { font-size: 1.3em; font-weight: bold; color: #222; margin: 1em 0 0.5em; }");
				sheet.addRule("h3 { font-size: 1.15em; font-weight: bold; color: #333; margin: 1em 0 0.4em; }");
				sheet.addRule("code { font-family: monospace; background-color: #f0f0f5; padding: 2px 4px; }");
				sheet.addRule("pre { background-color: #f6f8fa; border: 1px solid #dcdcdc; padding: 8px; }");
				sheet.addRule("table { border-collapse: collapse; }");
				sheet.addRule("th { background-color: #f4f4f8; border: 1px solid #c8c8d0; padding: 6px 10px; font-weight: bold; }");
				sheet.addRule("td { border: 1px solid #d0d0d8; padding: 6px 10px; }");
				sheet.addRule("blockquote { border-left: 4px solid #b0b0c0; background-color: #f9f9fc; padding: 0.5em 1em; }");
				sheet.addRule("a { color: #3572ff; }");
				chat.setEditorKit(kit);
			}
			chat.setBackground(Color.WHITE);
		chat.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
			
			JComboBox<Provider>	provider	= new JComboBox<>(Provider.values());
			provider.setSelectedItem(LlmHelper.DEFAULT_PROVIDER);
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
			ChatScrollPane	chat_scroll	= new ChatScrollPane(chat);
			panel.add(chat_scroll, BorderLayout.CENTER);
			panel.add(options, BorderLayout.SOUTH);
			panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Chat with Agent"));
		
			// Conversation state, same approach as the LlmChatAgentGui:
			// finished blocks are cached as rendered HTML in history_html,
			// the open block (pending) is accumulated and rendered when
			// it flushes, so finished messages are never re-rendered.
			StringBuilder	history_html	= new StringBuilder();
			StringBuilder	pending		= new StringBuilder();
			AtomicReference<ChatFragment.Type>	pending_type	= new AtomicReference<>();

			// Write the open pending block (if any) to the conversation
			// history as rendered HTML, then reset it. The view is not
			// updated here; the caller invokes {@code update_view} next.
			Runnable	flush_pending	= () ->
			{
				if(pending.length()>0)
				{
					String	body	= renderMarkdown(pending.toString());
					String	html	= switch(pending_type.get())
					{
						case THINKING -> "<div style=\"color:#808080;font-style:italic;\">" + body + "</div>\n";
						case TOOL_CALL -> "<div style=\"color:#660099;\">" + body + "</div>\n";
						case TOOL_RESULT -> "<div style=\"color:#006633;\">" + body + "</div>\n";
						default -> body + "\n";
					};
					history_html.append(html);
					pending.setLength(0);
					pending_type.set(null);
				}
			};
			
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

			// Update the chat view from the conversation history
			// plus the open pending block, scrolling to the bottom.
			Runnable	update_view	= () ->
			{
				chat.setText("<html><body>" + history_html + renderMarkdown(pending.toString()) + "</body></html>");
				chat_scroll.scrollToBottomResetHorizontal();
			};

			// Show a user prompt: rendered as markdown with a bold
			// "You:" prefix, same as the LlmChatAgentGui panel.
			Consumer<String>	append_user	= text ->
			{
				flush_pending.run();
				history_html.append("<p><b>You:</b></p>").append(renderMarkdown(text)).append("\n");
				update_view.run();
			};

			// Append a streamed fragment: consecutive fragments of the
			// same type are accumulated into a single block and rendered
			// as ONE markdown document on every update, so the view
			// shows a growing message instead of one line per token.
			Consumer<ChatFragment>	append_fragment	= frag ->
			{
				if(pending_type.get()!=frag.type())
				{
					flush_pending.run();
					pending_type.set(frag.type());
				}
				pending.append(frag.text());
				update_view.run();
			};

			ActionListener	al	= e ->
			{
				if(e.getSource()==prompt && !e.getActionCommand().equals("comboBoxEdited"))
					return;// Only react to combo box edits, not selection changes

				append_user.accept((String)prompt.getSelectedItem());


				prompt.setEnabled(false);
				send.setEnabled(false);

				//				System.out.println("Context size: "+
				//					((Provider)provider.getSelectedItem()).getContextSize((String) model.getSelectedItem()));

				// Generate base64 encoded image of current world state
				Container	worlds	= (Container)gui.getContentPane().getComponent(0);
				Component	world	= 	((Container) ((Container) ((Container) worlds.getComponent(1)).getComponent(0)).getComponent(0)).getComponent(0);
				RenderedImage	image	= LlmHelper.createImageFromComponent(world);

				IIntermediateFuture<ChatFragment> resp	= llmagentpojo[0].chat((String)prompt.getSelectedItem(),
					sendimage.isSelected() ? new RenderedImage[]{image} : new RenderedImage[0]);

				resp.next(fragment ->
				{
					append_fragment.accept(fragment);
				})
					.finished(v -> {prompt.setEnabled(true); send.setEnabled(true);
						flush_pending.run(); update_view.run();})
					.catchEx(v -> {prompt.setEnabled(true); send.setEnabled(true);
						flush_pending.run();
						history_html.append("<p><font color=\"#cc0000\"><b>Error: ").append(escapeHtml(v.toString())).append("</b></font></p>\n");
						update_view.run();})
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
					chat.setText("");
					history_html.setLength(0);
					pending.setLength(0);
					pending_type.set(null);
					IComponentHandle llmagent = IComponentManager.get().create(
						new LlmChatAgent(LlmHelper.createChatModel(
							(Provider)provider.getSelectedItem(),
							(String)model.getSelectedItem(),
							think.isSelected(), false)
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
