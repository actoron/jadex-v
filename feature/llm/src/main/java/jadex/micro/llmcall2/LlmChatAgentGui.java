package jadex.micro.llmcall2;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
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

import dev.langchain4j.model.chat.StreamingChatModel;
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
		clear	= new JButton("Clear");
		clear.setEnabled(false);
		status	= new JLabel("Not connected");

		JPanel	select_panel	= new JPanel(new GridLayout(2, 2, 5, 5));
		select_panel.add(new JLabel("Provider:"));
		select_panel.add(provider_combo);
		select_panel.add(new JLabel("Model:"));
		select_panel.add(model_combo);
		select_panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

		JPanel	button_panel	= new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		button_panel.add(clear);
		JPanel	status_panel	= new JPanel(new BorderLayout());
		status_panel.add(status, BorderLayout.CENTER);
		status_panel.add(button_panel, BorderLayout.EAST);
		status_panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

		// The chat view.
		chat	= new JEditorPane("text/html", "");
		chat.setEditable(false);
		chat.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

		// The input line.
		input	= new JTextField();
		send	= new JButton("Send");
		send.setEnabled(false);
		JPanel	panel	= new JPanel(new BorderLayout());
		panel.add(input, BorderLayout.CENTER);
		panel.add(send, BorderLayout.EAST);
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

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

		setSize(700, 500);
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
		history.append("<p><b>You:</b></p>").append(Markdown.toHtml(prompt)).append("\n");
		updateView();
	}

	protected void appendFragment(ChatFragment frag)
	{
		// LLM fragments are Markdown. Consecutive fragments of the same type
		// are accumulated into a single block, so the view shows a growing
		// block instead of one line per token.
		//
		// A fragment that starts with a line break ends the current block:
		// the agent emits "\n" markers between sentences (to keep lines
		// reasonably short) and on type change / completion.
		if(frag.text().startsWith("\n"))
			flushPending();
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
	 *  Call on block boundaries: fragment type change, line break marker,
	 *  and request completion.
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
		String	body	= Markdown.toHtml(pending.toString());
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
