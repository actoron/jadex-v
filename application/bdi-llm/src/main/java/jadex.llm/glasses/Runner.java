package jadex.llm.glasses;

import jadex.core.IComponentManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Runner
{
    private static final int agentIterations = 50;
    private static final String datasetPath = "application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json";
    private static final String resultsDirectory = "/home/schuther/Coding/results/";
    //Ollama
    private static final String chatUrl = "http://localhost:50510/api/chat";
    private static final String apiKey = "ollama";
    //GPT
//    private final String chatUrl = "https://api.openai.com/v1/chat/completions";
//    private final String apiKey =  System.getenv("OPENAI_API_KEY");


    /**
     * Main method to run the GlassesAgent.
     * @param args
     */
    public static void main(String[] args) throws IOException {

        if (Files.exists(Paths.get(datasetPath)))
        {
//            OpticiansDataGenerator dataGenerator = new OpticiansDataGenerator(10000);
//            dataGenerator.saveToJson(datasetPath);
        }

        // Format for date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        // Get current date and time
        String timestamp = LocalDateTime.now().format(formatter);

        // Define the folder path (change "baseDirectory" to a desired path if needed)
        Path currentResultsFolder = Paths.get(resultsDirectory + "results_" + timestamp);
        Path generatedPlanCodeFolder = Paths.get(currentResultsFolder + "/generatedPlanCode");
        Path generatedGoalCodeFolder = Paths.get(currentResultsFolder + "/generatedGoalCode");
        Path planResultsFolder = Paths.get(currentResultsFolder + "/planResults");

        // Create folder for each iteration
        Files.createDirectories(currentResultsFolder);
        Files.createDirectories(generatedPlanCodeFolder);
        Files.createDirectories(generatedGoalCodeFolder);
        Files.createDirectories(planResultsFolder);

        // Create the results file with the header row
        PrintWriter csvWriter = new PrintWriter(currentResultsFolder + "/results.csv");
        csvWriter.print("iteration,agentId,staticGoalCheck,chattyGoalCheck,attemptsGoal,attemptsPlan,generationTime,executionTime\n");
        csvWriter.close();

        for(int i = 0; i < agentIterations; i++)
        {

            try {
                System.out.println("A: GlassesAgent started iteration " + i);

                //agent
                GlassesAgent currentAgent = new GlassesAgent(
                        chatUrl,
                        apiKey,
                        datasetPath);

                IComponentManager.get().create(currentAgent);
                IComponentManager.get().waitForLastComponentTerminated();

                Map<String, String> agentResults = currentAgent.getAgentResults();
                // Write data row
                String agentResultsSting = i + "," +
                        agentResults.get("agentId") + "," +
                        agentResults.get("staticGoalCheck") + "," +
                        agentResults.get("chattyGoalCheck") + "," +
                        agentResults.get("genGoalAttempts1") + "," +
                        agentResults.get("genPlanAttempts1") + "," +
                        agentResults.get("genTime1") + "," +
                        agentResults.get("execTime1");

                try (FileWriter fileWriter = new FileWriter(currentResultsFolder + "/results.csv", true);
                PrintWriter printWriter = new PrintWriter(fileWriter)) {
                    printWriter.println(agentResultsSting);
                }

                PrintWriter out = new PrintWriter(generatedPlanCodeFolder + "/generatedPlanCode_" + i + ".java");
                out.println(agentResults.get("generatedPlanCode1"));
                out.close();

                out = new PrintWriter(generatedGoalCodeFolder + "/generatedGoalCode_" + i + ".java");
                out.println(agentResults.get("generatedGoalCode1"));
                out.close();

                out = new PrintWriter(planResultsFolder + "/planResults_" + i + ".json");
                out.println(agentResults.get("planResults1"));
                out.close();

                out = new PrintWriter(currentResultsFolder + "/planSettings.json");
                out.println(agentResults.get("planSettings1"));
                out.close();

                out = new PrintWriter(currentResultsFolder + "/goalSettings.json");
                out.println(agentResults.get("goalSettings"));
                out.close();



            } catch (Exception e) {
                System.out.println("A: Agent failed");
                e.printStackTrace();
            }
        }
    }
}
