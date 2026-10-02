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

public class Main3
{
    @Service
    public interface IAppTools
    {
        @Tool("Send text to the user and receive the user's reply. ")
        IFuture<String> getUserReply(String text);
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
               You are a game **host** for the game "20 Questions":
               **You** choose a famous person as your secret and store the chosen person.
               The user has to guess the secret person by asking yes/no questions.
               The user has 20 questions to guess the secret person.
               The user can ask questions like "Is the person alive?", "Is the person an actor?", "Is the person a politician?", etc.
               After each question, you answer the user with "Yes" or "No" and tell the user how many questions they have left.
               If the user guesses the secret person correctly, you tell the user "Congratulations! You guessed the secret person!" and end the game.
               If the user runs out of questions, you tell the user "Sorry, you ran out of questions. The secret person was [secret person]." and end the game.
               You need to store game state in the goal's parameters, such as the secret person, the number of questions left, and a list of the user's previous questions and your answers.
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
        public IFuture<String> getUserReply(String info)
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

        
//        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.UNSLOTH, "unsloth/GLM-4.7-Flash-GGUF:Q3_K_S", false, true);
//        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.UNSLOTH, "unsloth/gemma-4-E4B-it-qat-GGUF", true, true);
//        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.UNSLOTH, "unsloth/gemma-4-26B-A4B-it-GGUF", true, true);
        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "gemma4:26b-a4b-it-q4_K_M", true, true);
//        StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA_REMOTE, "gemma4:31b", false, true);
        //StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OLLAMA, "gemma4:31b", false, true);
        //StreamingChatModel llm = LlmHelper.createChatModel(LlmHelper.Provider.OPENAI_HCI, "api-programming-preloaded-1", false, true);
        
        ComponentManager.get().create(new LlmChatAgent2(llm)).get();

        IComponentHandle ua = ComponentManager.get().create(new WhoAmIAgent()).get();

        BDIViewer viewer = new BDIViewer(ua);
        viewer.setVisible(true);

        ComponentManager.get().waitForLastComponentTerminated();
    }
}
