package jadex.bding;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

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
        @Tool("Get a user question regarding the secret person you have in mind. "
            + "The info parameter contains information to show to the user first.")
        IFuture<String> getUserQuestion(String info);
    }

    @BDINGAgent
    public static class WhoAmIAgent
    {
        @Inject
        protected IComponent agent;

        public WhoAmIAgent()
        {
        }
        
        @OnStart
        protected void onStart()
        {
            String gamegoal = """
                Play a "Who Am I?" game with a user.

                Choose a famous person as the secret person and store the chosen person
                as a parameter of the goal. The secret person is private state of the goal
                and must not be revealed to the user.

                The user may ask up to 10 yes/no questions.
                Answer each question briefly with yes or no and, if useful, a small hint.

                Choose a famous person and keep the chosen person as private state of
                the goal. The user must never be told who the person is unless they
                correctly guess the person.

                The user wins if they correctly identify the secret person within 10
                questions. Otherwise, the agent wins.
                """;

            agent.getFeature(IBDINGAgentFeature.class).dispatchTopLevelGoal(gamegoal)
            .then(goal ->
            {
                goal.getFinished().then(Void ->
                {
                    System.out.println("goal finished: " + goal.getState());
                    agent.terminate();
                }).catchEx(ex ->
                {
                    System.out.println("goal finished with ex: " + ex.getMessage());
                    ex.printStackTrace();
                    agent.terminate();
                });
            })
            .catchEx(ex ->
            {
                System.out.println("dispatch failed: " + ex.getMessage());
                ex.printStackTrace();
                agent.terminate();
            });
        }
    }

    @Service
    public static class UserAgent implements IAppTools
    {
        protected JTextArea dialog;
        protected JTextField input;
        protected Future<String> questionFuture;

        public UserAgent()
        {
            SwingUtilities.invokeLater(() ->
            {
                JFrame frame = new JFrame("Who Am I?");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setSize(600, 500);

                dialog = new JTextArea();
                dialog.setEditable(false);
                dialog.setLineWrap(true);
                dialog.setWrapStyleWord(true);

                input = new JTextField();
                input.addActionListener(e -> submitQuestion());

                JButton send = new JButton("Ask");
                send.addActionListener(e -> submitQuestion());

                JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
                inputPanel.add(input, BorderLayout.CENTER);
                inputPanel.add(send, BorderLayout.EAST);

                frame.add(new JScrollPane(dialog), BorderLayout.CENTER);
                frame.add(inputPanel, BorderLayout.SOUTH);

                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                input.requestFocusInWindow();
            });
        }

        @Override
        public IFuture<String> getUserQuestion(String info)
        {
            Future<String> ret = new Future<>();
            questionFuture = ret;

            SwingUtilities.invokeLater(() ->
            {
                dialog.append("Agent: " + info + "\n\n");
                input.setEnabled(true);
                input.requestFocusInWindow();
            });

            return ret;
        }

        protected void submitQuestion()
        {
            String question = input.getText().trim();

            if(question.isEmpty())
                return;

            Future<String> future = questionFuture;
            if(future == null)
                return;

            questionFuture = null;
            input.setText("");
            input.setEnabled(false);

            dialog.append("You: " + question + "\n\n");

            future.setResult(question);
        }
    }


    public static void main(String[] args) 
    {
        ComponentManager.get().create(new UserAgent()).get();

        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "gemma4:31b", false, true);
        //StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA, "gemma4:31b", false, true);
        //StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OPENAI_HCI, "api-programming-preloaded-1", false, true);
        
        ComponentManager.get().create(new LlmChatAgent2(llm)).get();

        IComponentHandle ua = ComponentManager.get().create(new WhoAmIAgent()).get();

        BDIViewer viewer = new BDIViewer(ua);
        viewer.setVisible(true);

        ComponentManager.get().waitForLastComponentTerminated();
    }
}
