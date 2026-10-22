package jadex.micro.llmcall2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
 * A generic panel for an LLM chat interface.
 * Encapsulates the UI and logic for connecting to an LlmChatAgent.
 */
public class LlmChatPanel extends JPanel {

    private final Application app;
    private IComponentHandle agent_handle;
    private LlmChatAgent agent;

    private final JComboBox<LlmHelper.Provider> provider_combo;
    private final JComboBox<String> model_combo;
    private final JButton clear;
    private final JLabel status;
    private final JEditorPane chat;
    private ChatScrollPane chat_scroll;
    private final JTextField input;
    private final JButton send;

    private boolean busy = false;
    private ITerminableIntermediateFuture<ChatFragment> running = null;
    private boolean stopped = false;
    private final StringBuilder history = new StringBuilder();
    private final StringBuilder history_html = new StringBuilder();
    private final StringBuilder pending = new StringBuilder();
    private ChatFragment.Type pending_type = null;
    private volatile String rendered_pending = "";
    private int render_generation = 0;
    private boolean render_in_flight = false;

    private final ExecutorService render_pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-render");
        t.setDaemon(true);
        return t;
    });

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "llm-chat-executor");
        t.setDaemon(true);
        return t;
    });

    private boolean updating_models = false;

    public LlmChatPanel(Application app) {
        this.app = app;
        setLayout(new BorderLayout());

        provider_combo = new JComboBox<>(LlmHelper.Provider.values());
        provider_combo.setSelectedItem(LlmHelper.DEFAULT_PROVIDER);
        model_combo = new JComboBox<>();
        model_combo.setEditable(true);
        clear = new JButton("Clear conversation");
        status = new JLabel("Not connected");
        chat = new JEditorPane("text/html", "");
        chat.setEditable(false);
        setupChatEditor();
        input = new JTextField();
        input.setPreferredSize(new Dimension(0, 30));
        send = new JButton("Send");
        send.setEnabled(false);

        // Layout
        JPanel select_panel = createSelectPanel();
        JPanel status_panel = createStatusPanel();
        JPanel top_panel = new JPanel(new BorderLayout());
        top_panel.add(select_panel, BorderLayout.NORTH);
        top_panel.add(status_panel, BorderLayout.CENTER);

        JPanel input_panel = new JPanel(new BorderLayout(8, 0));
        JPanel send_panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        send_panel.add(send);
        input_panel.add(input, BorderLayout.CENTER);
        input_panel.add(send_panel, BorderLayout.EAST);
        input_panel.setBorder(BorderFactory.createEmptyBorder(5, 8, 8, 8));

        add(top_panel, BorderLayout.NORTH);
        chat_scroll = new ChatScrollPane(chat);
        add(chat_scroll, BorderLayout.CENTER);
        add(input_panel, BorderLayout.SOUTH);

        // Listeners
        provider_combo.addActionListener(e -> {
            setModelToDefault();
            fetchModels();
            connectAgent();
        });
        model_combo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !updating_models) {
                connectAgent();
            }
        });
        clear.addActionListener(e -> clearConversation());
        input.addActionListener(e -> sendMessage());
        send.addActionListener(e -> sendOrStop());

        // Initial
        setModelToDefault();
        fetchModels();
        connectAgent();
    }

    private void setupChatEditor() {
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet sheet = kit.getStyleSheet();
        sheet.addRule("body { font-family: 'Segoe UI', 'Helvetica Neue', Arial, sans-serif; font-size: 9px; line-height: 1.5; letter-spacing: 0; color: #1a1a2e; }");
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
        chat.setBackground(Color.WHITE);
        chat.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

        chat.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                try {
                    java.awt.Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception ex) {
                    status.setText("Could not open link: " + ex.getMessage());
                }
            }
        });
    }

    private JPanel createSelectPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gb = new GridBagConstraints();
        gb.insets = new java.awt.Insets(5,  8, 5, 8);
        gb.anchor = GridBagConstraints.WEST;

        JLabel providerLabel = new JLabel("Provider:");
        providerLabel.setAlignmentX(JLabel.RIGHT);
        panel.add(providerLabel, gb);

        gb.gridx = 1; gb.fill = GridBagConstraints.HORIZONTAL; gb.weightx = 1.0;
        panel.add(provider_combo, gb);

        JLabel modelLabel = new JLabel("Model:");
        modelLabel.setAlignmentX(JLabel.RIGHT);
        gb.gridx = 0; gb.gridy = 1; gb.fill = GridBagConstraints.NONE; gb.weightx = 0.0;
        panel.add(modelLabel, gb);

        gb.gridx = 1; gb.fill = GridBagConstraints.HORIZONTAL; gb.weightx = 1.0;
        panel.add(model_combo, gb);

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xd0, 0xd0, 0xd8)),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(status, BorderLayout.WEST);
        panel.add(clear, BorderLayout.EAST);
        panel.setBorder(BorderFactory.createEmptyBorder(3, 10, 6, 10));
        return panel;
    }

    public void setAgentHandle(IComponentHandle handle) { this.agent_handle = handle; }
    public void setAgent(LlmChatAgent agent) { this.agent = agent; }

    private void setModelToDefault() {
        LlmHelper.Provider provider = (LlmHelper.Provider) provider_combo.getSelectedItem();
        String model = provider == null ? null : LlmHelper.DEFAULT_MODELS.get(provider);
        updating_models = true;
        model_combo.setSelectedItem(model != null ? model : "");
        updating_models = false;
    }

    private void fetchModels() {
        LlmHelper.Provider provider = (LlmHelper.Provider) provider_combo.getSelectedItem();
        if (provider == null) return;
        CompletableFuture.supplyAsync(() -> provider.getModels(), executor)
            .thenAccept(models -> SwingUtilities.invokeLater(() -> {
                updating_models = true;
                model_combo.removeAllItems();
                model_combo.addItem("");
                if (models != null) models.forEach(model_combo::addItem);
                updating_models = false;
            }));
    }

    private void connectAgent() {
        LlmHelper.Provider provider = (LlmHelper.Provider) provider_combo.getSelectedItem();
        String model = getModel();
        if (provider == null) return;

        provider_combo.setEnabled(false);
        model_combo.setEnabled(false);
        status.setText("Connecting to " + provider + "/" + (model == null ? "<default>" : model) + "...");

        IComponentHandle old_handle = agent_handle;

        CompletableFuture.runAsync(() -> {
            try {
                StreamingChatModel llm = LlmHelper.createChatModel(provider, model, null, null);
                if (old_handle != null) old_handle.terminate().get();

                IComponentHandle handle = app.create(new LlmChatAgent(llm), "Chat").get();
                if (handle == null) {
                    SwingUtilities.invokeLater(() -> {
                        status.setText("Agent creation failed");
                        enableSelection();
                    });
                    return;
                }

                SwingUtilities.invokeLater(() -> switchAgent(handle, provider, model));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    status.setText("Connect failed: " + ex.getMessage());
                    enableSelection();
                });
            }
        }, executor);
    }

    private void enableSelection() {
        provider_combo.setEnabled(true);
        model_combo.setEnabled(true);
    }

    private void switchAgent(IComponentHandle handle, LlmHelper.Provider provider, String model) {
        if (running != null) {
            stopped = true;
            running.terminate();
            running = null;
            busy = false;
        }

        this.agent_handle = handle;
        this.agent = handle.getPojoHandle(LlmChatAgent.class);
        this.agent.setClearHistory(false);

        history.setLength(0);
        history_html.setLength(0);
        pending.setLength(0);
        pending_type = null;
        render_generation++;

        SwingUtilities.invokeLater(() -> {
            doUpdateView();
            enableSelection();
            setSendEnabled(true);
            clear.setEnabled(true);
            status.setText("Connected: " + provider + "/" + (model == null ? "<default>" : model));
        });
    }

    private void setSendEnabled(boolean enabled) { send.setEnabled(enabled); }
    private void setClearEnabled(boolean enabled) { clear.setEnabled(enabled); }

    private String getModel() {
        Object item = model_combo.getEditor().getItem();
        String m = item == null ? "" : item.toString().trim();
        return m.isEmpty() ? null : m;
    }

    private void sendMessage() {
        String prompt = input.getText().trim();
        if (agent == null || prompt.isEmpty() || busy) return;
        input.setText("");
        busy = true;
        stopped = false;
        appendUser(prompt);

        running = agent.chat(prompt);
        running.next(frag -> SwingUtilities.invokeLater(() -> appendFragment(frag)));
        running.done(ex -> SwingUtilities.invokeLater(() -> {
            flushPending();
            if (ex != null && !stopped) appendError(ex);
            doUpdateView();
            busy = false;
            running = null;
            setSendEnabled(false);
        }));
    }

    private void sendOrStop() {
        if (busy) {
            stopped = true;
            if (running != null) running.terminate();
        } else {
            sendMessage();
        }
    }

    private void clearConversation() {
        if (agent != null) {
            agent.resetConversation().printOnEx();
            history.setLength(0);
            history_html.setLength(0);
            pending.setLength(0);
            pending_type = null;
            render_generation++;
            doUpdateView();
        }
    }

    private void appendUser(String prompt) {
        flushPending();
        String html = "<p><b>You:</b></p>" + renderMarkdown(prompt) + "\n";
        history.append(html);
        history_html.append(html);
        doUpdateView();
    }

    private void appendFragment(ChatFragment frag) {
        if (pending_type != frag.type()) {
            flushPending();
            pending_type = frag.type();
        }
        pending.append(frag.text());
        render_generation++;
        schedulePendingRender();
        updateView();
    }

    private void appendError(Exception ex) {
        String html = "<p><font color=\"#cc0000\"><b>Error: " + ex.getMessage() + "</b></font></p>";
        history.append(html);
        history_html.append(html);
        doUpdateView();
    }

    private void flushPending() {
        if (pending.length() > 0) {
            render_generation++;
            String html = renderMarkdown(pending.toString());
            history.append(pending).append("\n");
            history_html.append(html);
            pending.setLength(0);
            pending_type = null;
        }
    }

    private void schedulePendingRender() {
        if (pending.length() == 0 || render_in_flight) return;
        render_in_flight = true;
        final int gen = render_generation;
        final String snap = pending.toString();
        final ChatFragment.Type type = pending_type;
        render_pool.submit(() -> {
            String body = renderMarkdown(snap);
            String res = switch (type) {
                case THINKING -> "<div style=\"color:#808080;font-style:italic;\">" + body + "</div>\n";
                case TOOL_CALL -> "<div style=\"color:#660099;\">" + body + "</div>\n";
                case TOOL_RESULT -> "<div style=\"color:#006633;\">" + body + "</div>\n";
                default -> body + "\n";
            };
            SwingUtilities.invokeLater(() -> {
                render_in_flight = false;
                if (gen == render_generation) {
                    rendered_pending = res;
                    doUpdateView();
                }
            });
        });
    }

    private void updateView() {
        doUpdateView();
    }

    private void doUpdateView() {
        chat.setText("<html><body style=\"margin:0; padding:0;\">" + history_html + rendered_pending + "</body></html>");
        chat_scroll.scrollToBottomResetHorizontal();
    }

    private String renderMarkdown(String markdown) {
        String processed = MathRenderer.preprocess(markdown);
        String html = SwingMarkdownRenderer.getInstance().renderMarkdown(processed);
        return CodeHighlighter.highlightBlocks(html);
    }
}
