
package jadex.bding;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.StreamingChatModel;
import jadex.bding.annotation.BDINGAgent;
import jadex.bding.tool.BDIViewer;
import jadex.core.IComponent;
import jadex.core.IComponentHandle;
import jadex.core.impl.ComponentManager;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.injection.annotation.OnStart;
import jadex.micro.llmcall2.LlmChatAgent2;
import jadex.micro.llmcall2.LlmHelper;
import jadex.providedservice.annotation.Service;

public class Main2
{
    @Service
    public interface IAppTools
    {
        @Tool("Display a message to the user.")
        IFuture<Void> displayMessageToUser(@P(name="message",description="The message to show the user.") String message);

        @Tool("Display the current number of guessing attempts and the maximum allowed attempts in the game GUI. Call this tool whenever you want to update the displayed attempt counter.")
        IFuture<Void> displayNumberOfTries(
            @P(name="tries", description="The current number of guessing attempts made by the user.") int tries,
            @P(name="maxTries", description="The maximum number of guessing attempts allowed.") int maxTries);

        @Tool("Wait for and return the user's next question, hint request, or guess.")
        IFuture<String> fetchUserQuestion();
    }

    @BDINGAgent
    public static class WhoAmIAgent
    {
        @Inject
        protected IComponent agent;

        @OnStart
        protected void onStart()
        {
            String gamegoal = """
                Play an entertaining "Who Am I?" game with a user.

                Choose a famous person as the secret person and store the chosen
                person as a parameter of the goal. The secret person is private
                state of the goal and must not be revealed to the user.

                The user may ask up to 10 yes/no questions and may request hints
                at any time. Hints do not count as questions.

                Answer questions truthfully. Use natural, friendly and varied
                language instead of replying with only "yes" or "no". Add a brief
                explanation, an interesting fact, or a playful comment when
                appropriate, but never reveal the person's identity indirectly.

                When the user requests a hint, provide a useful but not overly
                revealing clue. If the user asks for more hints, gradually make
                them more specific.

                React warmly to guesses and celebrate correct guesses. Keep the
                game engaging without giving away the answer prematurely.

                Track the number of yes/no questions asked. Hints do not count
                as questions. The user wins if they correctly identify the
                secret person within 10 questions. Otherwise, the agent wins
                and reveals the person's identity when the game ends.
                """;

            agent.getFeature(IBDINGAgentFeature.class)
                .dispatchTopLevelGoal(gamegoal)
                .then(goal ->
                {
                    goal.getFinished().then(Void ->
                    {
                        System.out.println("goal finished: " + goal.getState());
                        agent.terminate();
                    }).catchEx(ex ->
                    {
                        System.out.println(
                            "goal finished with ex: " + ex.getMessage());
                        ex.printStackTrace();
                        agent.terminate();
                    });
                })
                .catchEx(ex ->
                {
                    System.out.println(
                        "dispatch failed: " + ex.getMessage());
                    ex.printStackTrace();
                    agent.terminate();
                });
        }
    }

    @Service
    public static class UserAgent implements IAppTools
    {
        // UI colors
        protected static final Color BACKGROUND =
            new Color(245, 247, 250);
        protected static final Color SURFACE =
            Color.WHITE;
        protected static final Color AGENT_BUBBLE =
            new Color(255, 255, 255);
        protected static final Color USER_BUBBLE =
            new Color(224, 239, 255);
        protected static final Color PRIMARY =
            new Color(47, 111, 237);
        protected static final Color TEXT =
            new Color(35, 42, 52);
        protected static final Color MUTED =
            new Color(112, 122, 135);
        protected static final Color BORDER =
            new Color(224, 229, 236);

        protected JFrame frame;
        protected JPanel chatPanel;
        protected JScrollPane chatScroll;

        protected JTextField input;
        protected JButton send;

        protected JLabel inputStatus;
        protected JLabel triesStatus;
        protected JProgressBar progress;

        protected Future<String> questionFuture;

        protected int currentTries;
        protected int maxTries = 10;

        public UserAgent()
        {
            try
            {
                // Ensure the UI exists before the agent can call its tools.
                if(SwingUtilities.isEventDispatchThread())
                    createUI();
                else
                    SwingUtilities.invokeAndWait(this::createUI);
            }
            catch(Exception e)
            {
                throw new RuntimeException("Could not initialize game UI", e);
            }
        }

        protected void createUI()
        {
            try
            {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch(Exception ignored)
            {
                // Use the default Swing look and feel.
            }

            frame = new JFrame("Who Am I?");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(760, 720);
            frame.setMinimumSize(new Dimension(560, 500));
            frame.setLocationRelativeTo(null);

            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setBackground(BACKGROUND);
            root.setBorder(BorderFactory.createEmptyBorder(
                20, 24, 20, 24));

            root.add(createHeader(), BorderLayout.NORTH);
            root.add(createChatArea(), BorderLayout.CENTER);
            root.add(createInputArea(), BorderLayout.SOUTH);

            frame.setContentPane(root);
            setInputState(false);
            frame.setVisible(true);
        }

        protected JPanel createHeader()
        {
            JPanel header = new JPanel(new BorderLayout(14, 10));
            header.setBackground(SURFACE);
            header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 20, 16, 20)));

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("Who Am I?");
            title.setFont(new Font("SansSerif", Font.BOLD, 25));
            title.setForeground(TEXT);

            JLabel subtitle = new JLabel("Guess the famous person!");
            subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
            subtitle.setForeground(MUTED);
            subtitle.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

            titlePanel.add(title);
            titlePanel.add(subtitle);

            JPanel counterPanel = new JPanel();
            counterPanel.setOpaque(false);
            counterPanel.setLayout(new BoxLayout(counterPanel, BoxLayout.Y_AXIS));

            triesStatus = new JLabel("0 of 10 questions");
            triesStatus.setFont(new Font("SansSerif", Font.BOLD, 13));
            triesStatus.setForeground(TEXT);
            triesStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

            progress = new JProgressBar(0, 10);
            progress.setValue(0);
            progress.setStringPainted(false);
            progress.setForeground(PRIMARY);
            progress.setBackground(new Color(232, 236, 243));
            progress.setBorderPainted(false);
            progress.setPreferredSize(new Dimension(155, 9));
            progress.setMinimumSize(new Dimension(155, 9));
            progress.setMaximumSize(new Dimension(155, 9));
            progress.setAlignmentX(Component.CENTER_ALIGNMENT);

            counterPanel.add(triesStatus);
            counterPanel.add(Box.createVerticalStrut(9));
            counterPanel.add(progress);

            header.add(titlePanel, BorderLayout.CENTER);
            header.add(counterPanel, BorderLayout.EAST);

            return header;
        }

        protected JPanel createChatArea()
        {
            JPanel container = new JPanel(new BorderLayout());
            container.setBackground(SURFACE);
            container.setBorder(BorderFactory.createLineBorder(BORDER));

            chatPanel = new JPanel();
            chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
            chatPanel.setBackground(BACKGROUND);
            chatPanel.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 16));

            chatScroll = new JScrollPane(chatPanel);
            chatScroll.setBorder(BorderFactory.createEmptyBorder());
            chatScroll.getViewport().setBackground(BACKGROUND);
            chatScroll.getVerticalScrollBar().setUnitIncrement(16);
            chatScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            container.add(chatScroll, BorderLayout.CENTER);

            return container;
        }

        protected JPanel createInputArea()
        {
            JPanel outer = new JPanel(new BorderLayout(0, 10));
            outer.setBackground(SURFACE);
            outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

            inputStatus = new JLabel("Warte auf das Spiel …");
            inputStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
            inputStatus.setForeground(MUTED);

            input = new JTextField();
            input.setFont(new Font("SansSerif", Font.PLAIN, 14));
            input.setMargin(new Insets(10, 12, 10, 12));
            input.addActionListener(e -> submitQuestion());

            send = new JButton("Send");
            send.setFont(new Font("SansSerif", Font.BOLD, 13));
            send.setForeground(Color.WHITE);
            send.setBackground(PRIMARY);
            send.setOpaque(true);
            send.setBorderPainted(false);
            send.setFocusPainted(false);
            send.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            send.setMargin(new Insets(10, 20, 10, 20));
            send.addActionListener(e -> submitQuestion());

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.add(input, BorderLayout.CENTER);
            row.add(send, BorderLayout.EAST);

            outer.add(inputStatus, BorderLayout.NORTH);
            outer.add(row, BorderLayout.CENTER);

            return outer;
        }

        @Override
        public IFuture<Void> displayMessageToUser(String message)
        {
            Future<Void> ret = new Future<>();

            SwingUtilities.invokeLater(() ->
            {
                if(message != null && !message.isBlank())
                    addMessage(message, false);

                ret.setResult(null);
            });

            return ret;
        }

        @Override
        public IFuture<Void> displayNumberOfTries(
            int tries, int maxTries)
        {
            Future<Void> ret = new Future<>();

            SwingUtilities.invokeLater(() ->
            {
                this.currentTries = Math.max(0, tries);
                this.maxTries = Math.max(1, maxTries);

                triesStatus.setText(
                    currentTries + " of " + this.maxTries + " questions");

                progress.setMaximum(this.maxTries);
                progress.setValue(Math.min(currentTries, this.maxTries));

                ret.setResult(null);
            });

            return ret;
        }

        @Override
        public IFuture<String> fetchUserQuestion()
        {
            Future<String> ret = new Future<>();

            SwingUtilities.invokeLater(() ->
            {
                questionFuture = ret;
                setInputState(true);
                input.requestFocusInWindow();
            });

            return ret;
        }

        protected void submitQuestion()
        {
            if(!input.isEnabled())
                return;

            String question = input.getText().trim();

            if(question.isEmpty())
                return;

            Future<String> future = questionFuture;

            if(future == null)
                return;

            questionFuture = null;
            input.setText("");

            addMessage(question, true);
            setInputState(false);

            future.setResult(question);
        }

        protected void addMessage(String message, boolean fromUser)
        {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);

            JPanel bubble = new JPanel(new BorderLayout(0, 6));
            bubble.setBackground(
                fromUser ? USER_BUBBLE : AGENT_BUBBLE);
            bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    fromUser
                        ? new Color(205, 225, 252)
                        : BORDER),
                BorderFactory.createEmptyBorder(11, 14, 11, 14)));

            JLabel sender = new JLabel(
                fromUser ? "Du" : "Who Am I?");
            sender.setFont(new Font("SansSerif", Font.BOLD, 12));
            sender.setForeground(fromUser ? PRIMARY : MUTED);

            JTextArea content = new JTextArea(message);
            content.setEditable(false);
            content.setFocusable(false);
            content.setLineWrap(true);
            content.setWrapStyleWord(true);
            content.setOpaque(false);
            content.setForeground(TEXT);
            content.setFont(new Font("SansSerif", Font.PLAIN, 14));
            content.setBorder(BorderFactory.createEmptyBorder());
            content.setColumns(34);

            // Estimate the number of wrapped lines for a compact bubble.
            int lines = 0;
            for(String paragraph : message.split("\n", -1))
            {
                lines += Math.max(1,
                    (int)Math.ceil(paragraph.length() / 43.0));
            }
            content.setRows(Math.max(1, lines));

            bubble.add(sender, BorderLayout.NORTH);
            bubble.add(content, BorderLayout.CENTER);

            // Keep messages readable without stretching them across
            // the entire chat area.
            bubble.setMaximumSize(new Dimension(490, Integer.MAX_VALUE));
            bubble.setPreferredSize(null);

            if(fromUser)
            {
                row.add(bubble, BorderLayout.EAST);
                row.setBorder(BorderFactory.createEmptyBorder(
                    4, 50, 10, 0));
            }
            else
            {
                row.add(bubble, BorderLayout.WEST);
                row.setBorder(BorderFactory.createEmptyBorder(
                    4, 0, 10, 50));
            }

            chatPanel.add(row);
            chatPanel.add(Box.createVerticalStrut(2));

            chatPanel.revalidate();
            chatPanel.repaint();

            SwingUtilities.invokeLater(() ->
            {
                chatScroll.getVerticalScrollBar().setValue(
                    chatScroll.getVerticalScrollBar().getMaximum());
            });
        }

        protected void setInputState(boolean requested)
        {
            input.setEnabled(requested);
            send.setEnabled(requested);

            inputStatus.setText(requested
                ? "Your turn. Pose a question, guess or ask for a hint."
                : "Who Am I? thinking …");

            inputStatus.setForeground(
                requested ? PRIMARY : MUTED);

            send.setBackground(
                requested ? PRIMARY : new Color(170, 183, 204));
        }
    }

    public static void main(String[] args)
    {
        ComponentManager.get().create(new UserAgent()).get();

        // StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "gemma4:31b", false, true);

        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OPENAI_HCI, "api-programming-preloaded-1", false, true);

        //StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OPENAI_HCI, "Qwen3.8-27B-FP8", false, true);

        //StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.VLLM, "Qwen3.8-27B", false, true);

        ComponentManager.get().create(new LlmChatAgent2(llm)).get();

        IComponentHandle ua = ComponentManager.get().create(new WhoAmIAgent()).get();

        BDIViewer viewer = new BDIViewer(ua);
        viewer.setVisible(true);

        ComponentManager.get().waitForLastComponentTerminated();
    }
}
