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

public class Runner {
    /**
     * Main method to run the GlassesAgent.
     * @param args
     */
    public static void main(String[] args) throws IOException {
        int agentIterations = 2;
        String resultsDirectory = "/home/schuther/Coding/results/";

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
        csvWriter.print("iteration,agentId,goalStatus,attemptsPlan,attemptsGoal,generationTime,executionTime,planCode,goalCode,planResults\n");
        csvWriter.close();

        for(int i = 0; i < agentIterations; i++)
        {

            try {
                System.out.println("A: GlassesAgent started iteration " + i);

                // chatgpt agent
//                GlassesAgent currentAgent = new GlassesAgent(
//                        "https://api.openai.com/v1/chat/completions",
//                        System.getenv("OPENAI_API_KEY"),
//                        "application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json");

                //ollama agent
                GlassesAgent currentAgent = new GlassesAgent(
                        "http://localhost:50510/api/generate",
                        "ollama",
                        "application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json");

                IComponentManager.get().create(currentAgent);
                IComponentManager.get().waitForLastComponentTerminated();

                Map<String, String> agentResults = currentAgent.getAgentResults();
                // Write data row
                String agentResultsSting = i + "," +
                        agentResults.get("agentId") + "," +
                        agentResults.get("goalResults") + "," +
                        agentResults.get("genPlanAttempts1") + "," +
                        agentResults.get("genGoalAttempts1") + "," +
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

            } catch (Exception e) {
                System.out.println("A: Agent failed");
                e.printStackTrace();
            }
        }
    }
}
