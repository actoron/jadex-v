package jadex.micro.llmcall2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import dev.langchain4j.model.chat.StreamingChatModel;
import io.github.raghultech.markdown.swing.integration.SwingMarkdownRenderer;
import jadex.core.Application;
import jadex.core.IComponentHandle;
import jadex.future.ITerminableIntermediateFuture;

/**
 *  Generic Swing chat GUI backed by an {@link LlmChatAgent} component.
 *  The user selects an LLM {@link LlmHelper.Provider} and a model; the chat
 *  agent connects automatically whenever the selection changes: a new
 *  LlmChatAgent component is created dynamically for the selected
 *  provider/model, while the previously connected agent (if any) is
 *  terminated.
 *  <p>
 *  Prompts are sent via the input field (or the send button / Enter key)
 *  and the LLM's streaming response fragments (response, thinking, tool calls,
 *  tool results) are rendered in the chat view, color-coded by fragment type.
 *  The conversation history is kept across prompts within one connected agent
 *  and is reset when connecting with a different provider/model.
 */
public class LlmChatAgentGui extends JFrame
{
	//-------- attributes --------

	/** The application that creates the chat agents. */
	Application	app;

	/** Handle to the currently connected LLM chat agent component. */
	IComponentHandle	agent_handle;

	/** Pojo handle of the currently connected agent. */
	LlmChatAgent	agent;

	/** Provider selection. */
	JComboBox<LlmHelper.Provider>	provider_combo;

	/** Model selection (editable, so custom model names can be typed). */
	JComboBox<String>	model_combo;

	/** The clear conversation button. */
	JButton	clear;

	/** Status label. */
	JLabel	status;

	/** The view that displays the chat history. */
	protected JEditorPane	chat;

	/** The input field for new prompts. */
	protected JTextField	input;

	/** The send button. */
	protected JButton	send;

	/** Whether a chat is currently in progress. */
	boolean	busy	= false;

	/** The currently running chat request, so it can be stopped mid-stream. */
	ITerminableIntermediateFuture<ChatFragment>	running	= null;

	/** Set when the user stops a request, so the completion handler
	 *  doesn't report the termination as an error. */
	boolean	stopped	= false;

	/** The HTML buffer of the whole conversation. */
	StringBuilder	history	= new StringBuilder();

	/** The raw text of the current open block, not yet written to the history.
	 *  Consecutive fragments of the same type are accumulated here so the
	 *  view shows a single growing block instead of one line per token. */
	StringBuilder	pending	= new StringBuilder();

	/** The fragment type of the pending block, null if no block is open. */
	ChatFragment.Type	pending_type	= null;

	/** Guard flag so programmatic model list updates don't trigger a reconnect. */
	boolean	updating_models	= false;

	/** Executor for model list fetches and agent connections to keep the EDT free.
	 *  Daemon threads, so the JVM can exit when the GUI is closed. */
	static final ExecutorService	executor	= Executors.newCachedThreadPool(r ->
	{
		Thread	t	= new Thread(r, "llm-chat-gui");
		t.setDaemon(true);
		return t;
	});

	//-------- constructors --------

	/**
	 *  Create the chat GUI.
	 *  Must be called on the EDT.
	 *  @param app The application that will create the chat agents.
	 *  @param title The window title.
	 */
	public LlmChatAgentGui(Application app, String title)
	{
		super(title);
		this.app	= app;

		// The provider / model selection.
		provider_combo	= new JComboBox<>(LlmHelper.Provider.values());
		model_combo	= new JComboBox<>();
		model_combo.setEditable(true);
		clear	= new JButton("Clear conversation");
		clear.setToolTipText("Reset the chat history so the model starts a fresh conversation");
		clear.setEnabled(false);
		status	= new JLabel("Not connected");

		// Connection bar: labels and combos laid out in a grid so the
		// combos are left-aligned below the labels, the clear button
		// sits on the right, and everything stays in a single row.
		JLabel	provider_label	= new JLabel("Provider:");
		JLabel	model_label	= new JLabel("Model:");
		provider_label.setLabelFor(provider_combo);
		model_label.setLabelFor(model_combo);
		provider_label.setAlignmentX(JLabel.RIGHT);
		model_label.setAlignmentX(JLabel.RIGHT);

		JPanel	select_panel	= new JPanel(new GridBagLayout());
		GridBagConstraints	gb	= new GridBagConstraints();
		gb.insets	= new Insets(5, 8, 5, 8);
		gb.anchor	= GridBagConstraints.WEST;

		gb.gridx	= 0; gb.gridy	= 0;
		select_panel.add(provider_label, gb);
		gb.gridx	= 1; gb.fill	= GridBagConstraints.HORIZONTAL; gb.weightx	= 1.0;
		select_panel.add(provider_combo, gb);
		gb.gridx	= 0; gb.gridy	= 1; gb.fill	= GridBagConstraints.NONE; gb.weightx	= 0.0;
		select_panel.add(model_label, gb);
		gb.gridx	= 1; gb.fill	= GridBagConstraints.HORIZONTAL; gb.weightx	= 1.0;
		select_panel.add(model_combo, gb);
		select_panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(0xd0, 0xd0, 0xd8)),
			BorderFactory.createEmptyBorder(5, 5, 5, 5)));

		// Status line below the connection bar: connection state on the
		// left, and the "Clear conversation" button on the right, so the
		// conversation control sits with the chat view it acts on instead
		// of in the connection bar (where it reads as a connection action).
		JPanel	status_panel	= new JPanel(new BorderLayout());
		status_panel.add(status, BorderLayout.WEST);
		status_panel.add(clear, BorderLayout.EAST);
		status_panel.setBorder(BorderFactory.createEmptyBorder(3, 10, 6, 10));

		// The chat view with a flat, light sans-serif theme.
		chat	= new JEditorPane("text/html", "");
		chat.setEditable(false);
		{
			HTMLEditorKit	kit	= new HTMLEditorKit();
			StyleSheet		sheet	= kit.getStyleSheet();
			// Base sans-serif, light modern palette
			sheet.addRule("body { font-family: sans-serif; color: #1a1a2e; }");
			sheet.addRule("h1 { font-size: 1.5em; font-weight: bold; color: #111; margin: 1em 0 0.5em; }");
			sheet.addRule("h2 { font-size: 1.3em; font-weight: bold; color: #222; margin: 1em 0 0.5em; }");
			sheet.addRule("h3 { font-size: 1.15em; font-weight: bold; color: #333; margin: 1em 0 0.4em; }");
			// Inline code: subtle tint, monospace override
			sheet.addRule("code { font-family: monospace; background-color: #f0f0f5; padding: 2px 4px; }");
			// Code blocks: light grey box with border
			sheet.addRule("pre { background-color: #f6f8fa; border: 1px solid #dcdcdc; padding: 8px; }");
			// Tables: clean bordered flat look
			sheet.addRule("table { border-collapse: collapse; }");
			sheet.addRule("th { background-color: #f4f4f8; border: 1px solid #c8c8d0; padding: 6px 10px; font-weight: bold; }");
			sheet.addRule("td { border: 1px solid #d0d0d8; padding: 6px 10px; }");
			// Blockquote: left accent bar
			sheet.addRule("blockquote { border-left: 4px solid #b0b0c0; background-color: #f9f9fc; padding: 0.5em 1em; }");
			// Links: modern blue
			sheet.addRule("a { color: #3572ff; }");
			chat.setEditorKit(kit);
		}
		chat.setBackground(Color.WHITE);
		chat.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));

		// Open clicked links (rendered by the markdown renderer as
		// standard <a> anchors) in the system's default browser.
		chat.addHyperlinkListener(e ->
		{
			if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED
					&& Desktop.isDesktopSupported()
					&& Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
			{
				try
				{
					Desktop.getDesktop().browse(e.getURL().toURI());
				}
				catch(Exception ex)
				{
					status.setText("Could not open link: "+ex.getMessage());
				}
			}
		});

		// The input line: a wide, comfortable text field with the
		// send button pinned to the right.
		input	= new JTextField();
		input.setPreferredSize(new Dimension(0, 30));
		send	= new JButton("Send");
		send.setEnabled(false);
		send.setMinimumSize(new Dimension(70, 28));
		JPanel	panel	= new JPanel(new BorderLayout(8, 0));
		panel.add(input, BorderLayout.CENTER);
		JPanel	send_panel	= new JPanel(new FlowLayout(FlowLayout.RIGHT));
		send_panel.add(send);
		panel.add(send_panel, BorderLayout.EAST);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 8, 8, 8));

		JPanel	top_panel	= new JPanel(new BorderLayout());
		top_panel.add(select_panel, BorderLayout.NORTH);
		top_panel.add(status_panel, BorderLayout.CENTER);

		getContentPane().add(top_panel, BorderLayout.NORTH);
		getContentPane().add(new JScrollPane(chat), BorderLayout.CENTER);
		getContentPane().add(panel, BorderLayout.SOUTH);

		// Provider changed: reset the model to the provider's default,
		// fetch the model list, and reconnect.
		provider_combo.addActionListener(e ->
		{
			setModelToDefault();
			fetchModels();
			connectAgent();
		});

		// Model changed (selected or typed): reconnect.
		model_combo.addItemListener(e ->
		{
			if(e.getStateChange()==ItemEvent.SELECTED && !updating_models)
				connectAgent();
		});

		// Clear conversation button.
		clear.addActionListener(e ->
		{
			if(agent!=null)
			{
				agent.resetConversation().printOnEx();
				history.setLength(0);
				pending.setLength(0);
				pending_type	= null;
				updateView();
			}
		});

		// Enter key sends; the send button sends while idle,
		// stops a running request when pressed as "Stop".
		input.addActionListener(e -> sendMessage());
		send.addActionListener(e -> sendOrStop());

		// Terminate the agent when the window is closed.
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if(agent_handle!=null)
					agent_handle.terminate().printOnEx();
			}
		});

		// Fetch the model list of the default provider initially,
		// so the model combo box is populated when the GUI is shown.
		// Pre-select the provider's default model (LlmHelper.DEFAULT_MODELS).
		setModelToDefault();
		fetchModels();

		// A comfortable default size; the frame can grow, but not
		// shrink so far that the connection bar gets cut off.
		setSize(700, 500);
		setMinimumSize(new Dimension(500, 380));
		setLocationRelativeTo(null);
		setVisible(true);
	}

	//-------- provider / model selection --------

	/**
	 *  Set the model combo box to the currently selected provider's
	 *  default model from {@link LlmHelper#DEFAULT_MODELS}, or to an
	 *  empty string if the provider has no configured default.
	 *  The update is suppressed so it doesn't trigger a reconnect.
	 *  Call on the EDT.
	 */
	protected void setModelToDefault()
	{
		LlmHelper.Provider	provider	= (LlmHelper.Provider)provider_combo.getSelectedItem();
		String				model		= provider==null? null: LlmHelper.DEFAULT_MODELS.get(provider);
		updating_models	= true;
		try
		{
			model_combo.setSelectedItem(model!=null? model: "");
		}
		finally
		{
			updating_models	= false;
		}
	}

	/**
	 *  Fetch the model list of the currently selected provider
	 *  in a background thread and update the model combo box.
	 *  The current selection is preserved if it is still available.
	 */
	protected void fetchModels()
	{
		LlmHelper.Provider	provider	= (LlmHelper.Provider)provider_combo.getSelectedItem();
		if(provider==null)
			return;

		CompletableFuture.supplyAsync(() -> provider.getModels(), executor)
		.thenAccept(models -> SwingUtilities.invokeLater(() ->
		{
			List<String>	items	= models==null || models.isEmpty()? List.of(): models;
			String	current	= getModel();
			updating_models	= true;
			try
			{
				model_combo.removeAllItems();
				model_combo.addItem("");	// Empty item for using the provider's default model.
				for(String model: items)
					model_combo.addItem(model);
				// Preserve the current selection if it is still in the list.
				model_combo.setSelectedItem(current!=null && (current.isEmpty() || items.contains(current))? current: "");
			}
			finally
			{
				updating_models	= false;
			}
		}))
		.exceptionally(ex ->
		{
			SwingUtilities.invokeLater(() ->
			{
				status.setText("Model fetch failed: "+ex.getCause());
			});
			return null;
		});
	}

	/**
	 *  Get the currently selected / typed model name, or null for the provider's default.
	 */
	protected String getModel()
	{
		Object	item	= model_combo.getEditor().getItem();
		String	model	= item==null? null: item.toString().trim();
		return model.isEmpty()? null: model;
	}

	/**
	 *  Connect a new LLM chat agent with the currently selected provider/model:
	 *  the LLM model is created, a new LlmChatAgent component is spawned
	 *  in the application, and the previously connected agent (if any)
	 *  is terminated on the EDT after the switch.
	 */
	protected void connectAgent()
	{
		LlmHelper.Provider	provider	= (LlmHelper.Provider)provider_combo.getSelectedItem();
		String	model	= getModel();
		if(provider==null)
			return;

		// Disable the selection controls while connecting.
		provider_combo.setEnabled(false);
		model_combo.setEnabled(false);
		status.setText("Connecting to "+provider+"/"+(model==null? "<default>": model)+"...");

		// Capture the currently connected agent on the EDT so the background
		// thread can terminate it *before* creating the new one.
		IComponentHandle	old_handle	= agent_handle;

		CompletableFuture.runAsync(() ->
		{
			StreamingChatModel	llm;
			try
			{
				llm	= LlmHelper.createChatModel(provider, model, null, null);

				// Terminate the previous agent before creating the new one.
				// Both share the fixed cid "Chat", so the application would
				// reject the new component ("component with same cid already
				// exists") unless the old one has been fully unregistered.
				if(old_handle!=null)
					old_handle.terminate().get();

				// Create the new chat agent component.
				IComponentHandle	handle	= app.create(new LlmChatAgent(llm), "Chat").get();
				if(handle==null)
				{
					SwingUtilities.invokeLater(() ->
					{
						status.setText("Agent creation failed");
						enableSelection();
					});
					return;
				}

				// Switch the GUI to the new agent.
				SwingUtilities.invokeLater(() -> switchAgent(handle, provider, model));
			}
			catch(Exception ex)
			{
				SwingUtilities.invokeLater(() ->
				{
					status.setText("Connect failed: "+ex.getMessage());
					enableSelection();
				});
			}
		}, executor);
	}

	/**
	 *  Re-enable the provider / model selection controls.
	 *  Call on the EDT.
	 */
	protected void enableSelection()
	{
		provider_combo.setEnabled(true);
		model_combo.setEnabled(true);
	}

	/**
	 *  Switch the GUI to a newly created agent.
	 *  The previous agent (if any) has already been terminated
	 *  by {@link #connectAgent()} before the new one was created.
	 *  Call on the EDT.
	 */
	protected void switchAgent(IComponentHandle handle, LlmHelper.Provider provider, String model)
	{
		// Drop any in-progress request of the previous agent.
		if(running!=null)
		{
			stopped	= true;
			running.terminate();
			running	= null;
			busy	= false;
		}

		agent_handle	= handle;
		agent	= handle.getPojoHandle(LlmChatAgent.class);

		// Keep the conversation history across prompts, so the LLM remembers the chat.
		agent.setClearHistory(false);

		// New agent -> new conversation.
		history.setLength(0);
		pending.setLength(0);
		pending_type	= null;
		updateView();

		enableSelection();
		send.setEnabled(true);
		updateSendButton();
		clear.setEnabled(true);
		status.setText("Connected: "+provider+"/"+(model==null? "<default>": model));
	}

	//-------- user actions --------

	/**
	 *  Send the current input text to the connected LLM agent
	 *  and display the streaming response fragments.
	 *  Call on the EDT.
	 */
	protected void sendMessage()
	{
		String	prompt	= input.getText().trim();
		if(agent==null || prompt.isEmpty() || busy)
			return;

		input.setText("");
		busy	= true;
		stopped	= false;
		appendUser(prompt);

		running	= agent.chat(prompt);
		running.next(frag -> SwingUtilities.invokeLater(() -> appendFragment(frag)));
		running.done(ex -> SwingUtilities.invokeLater(() ->
		{
			flushPending();
			if(ex!=null && !stopped)
				appendError(ex);
			updateView();
			busy	= false;
			running	= null;
			updateSendButton();
		}));
		updateSendButton();
	}

	/**
	 *  Action handler for the send/stop button:
	 *  sends a message while idle, stops the running request when busy.
	 *  Call on the EDT.
	 */
	protected void sendOrStop()
	{
		if(busy)
			stopMessage();
		else
			sendMessage();
	}

	/**
	 *  Stop the currently running chat request mid-stream.
	 *  The already received fragments stay in the conversation.
	 *  Call on the EDT.
	 */
	protected void stopMessage()
	{
		if(running!=null)
		{
			stopped	= true;
			running.terminate();
		}
	}

	/**
	 *  Keep the send button label in sync with the current state:
	 *  "Stop" while a request is running, "Send" otherwise.
	 *  Call on the EDT.
	 */
	protected void updateSendButton()
	{
		send.setText(busy? "Stop": "Send");
	}

	//-------- view updates (call on the EDT) --------

	protected void appendUser(String prompt)
	{
		flushPending();
		history.append("<p><b>You:</b></p>").append(renderMarkdown(prompt)).append("\n");
		updateView();
	}

	/**
	 *  LLM fragments are Markdown. Consecutive fragments of the same type
	 *  are accumulated into a single block and rendered as ONE markdown
	 *  document, so the view shows a growing block instead of one line
	 *  per token.
	 *  <p>
	 *  The agent emits "\n" markers between sentences (to keep lines
	 *  reasonably short) and on type change / completion. Those "\n"
	 *  markers are appended to the pending block as part of the markdown
	 *  source and are NOT treated as block boundaries: flushing on them
	 *  would split an open code fence and shatter its rendering. The
	 *  pending block is only flushed on a type change or on request
	 *  completion, so a whole message of one type always renders as a
	 *  single markdown document.
	 *  Call on the EDT.
	 */
	protected void appendFragment(ChatFragment frag)
	{
		if(pending_type!=frag.type())
		{
			flushPending();
			pending_type	= frag.type();
		}
		pending.append(frag.text());
		updateView();
	}

	protected void appendError(Exception ex)
	{
		history.append("<p><font color=\"#cc0000\"><b>Error: ").append(escape(ex.toString())).append("</b></font></p>");
		updateView();
	}

	/**
	 *  Write the pending block to the conversation history.
	 *  Call on block boundaries: fragment type change and
	 *  request completion. The whole accumulated block is
	 *  rendered as a single markdown document, so code fences
	 *  and other constructs that span multiple fragments stay
	 *  intact.
	 *  Call on the EDT.
	 */
	protected void flushPending()
	{
		if(pending.length()>0)
		{
			history.append(renderPending());
			pending.setLength(0);
			pending_type	= null;
		}
	}

	/**
	 *  Render the pending block with its fragment type's color,
	 *  or an empty string if no block is open.
	 *  @return The Swing HTML of the pending block.
	 */
	protected String renderPending()
	{
		if(pending.length()==0 || pending_type==null)
			return "";
		String	body	= renderMarkdown(pending.toString());
		return switch(pending_type)
		{
			case THINKING -> "<div style=\"color:#808080;font-style:italic;\">" + body + "</div>\n";
			case TOOL_CALL -> "<div style=\"color:#660099;\">" + body + "</div>\n";
			case TOOL_RESULT -> "<div style=\"color:#006633;\">" + body + "</div>\n";
			default -> body + "\n";
		};
	}

	/**
	 *  Update the chat view with the current conversation history
	 *  plus the pending block, and scroll to the bottom.
	 */
	protected void updateView()
	{
		chat.setText("<html><body>" + history + renderPending() + "</body></html>");
		chat.setCaretPosition(chat.getDocument().getLength());
	}

	/**
	 *  Render a Markdown text to Swing-compatible HTML
	 *  using the Swing-Markdown-Preview library
	 *  (Flexmark GFM renderer, including tables, task lists,
	 *  strikethrough, and emoji). TeX math in the text is
	 *  replaced by images of the rendered formula first, so
	 *  the chat view shows the math instead of the raw TeX.
	 *  @param markdown The Markdown source.
	 *  @return The Swing-compatible HTML.
	 */
	protected static String renderMarkdown(String markdown)
	{
		String	processed	= MathRenderer.preprocess(markdown);
		return SwingMarkdownRenderer.getInstance().renderMarkdown(processed);
	}

	/**
	 *  Escape text for HTML.
	 */
	protected static String escape(String text)
	{
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	//-------- main method --------

	/**
	 *  Start a standalone chat GUI.
	 *  No agent is connected initially; a chat agent is created
	 *  automatically as soon as a provider / model selection is made.
	 */
	public static void main(String[] args)
	{
		Application	app	= new Application("Llm Chat");
		SwingUtilities.invokeLater(() -> new LlmChatAgentGui(app, "Llm Chat"));
	}
}
