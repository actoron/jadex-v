package jadex.llm.glasses;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.impl.inmemory.IPlanBody;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.runtime.Val;
import jadex.core.*;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Description;
import jadex.model.annotation.OnEnd;
import jadex.model.annotation.OnStart;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;


@Agent(type="bdip")
@Description("This agent uses ChatGPT to create the plan step.")
public class GlassesAgent {
    /**
     * The Glasses agent class.
     */
    @Agent
    protected IComponent agent;

    private final String chatUrl;
    private final String apiKey;
    private final String dataSetPath;
    private final String beliefType;

    // Using thread-safe ConcurrentHashMap
    private ConcurrentHashMap<String, String> agentResults = new ConcurrentHashMap<>();

    @Belief
    private Val<String> datasetString;

    @Goal
    public class AgentGoal {
        private IPlanBody goalPlan = null;

        @GoalParameter
        protected Val<String> convDataSetString; // Val<String> convDataSet;

        @GoalCreationCondition(beliefs = "datasetString")
        public AgentGoal(String convDataSetString) {
            this.convDataSetString = new Val<>(convDataSetString);
            System.out.println("A: Goal created");
        }

        @GoalFinished
        public void goalFinished() {
            System.out.println("A: Goal finished");
            agent.terminate();
        }

        @GoalTargetCondition(parameters = "convDataSetString")
        public boolean checkTarget() {
            System.out.println("--->Test Goal");
            LlmFeature llmFeature = new LlmFeature(
                    chatUrl,
                    apiKey,
                    beliefType,
                    "application/bdi-llm/src/main/java/jadex.llm/glasses/settings/GoalSettings_ollama.json");

            if (this.goalPlan == null) {
                llmFeature.connectToLLM("");

                int attempt = 0;
                while (attempt < 3) {
                    try {
                        this.goalPlan = llmFeature.generateAndCompileCode();
                        agentResults.put("generatedGoalCode1", llmFeature.generatedJavaCode);

                        if (this.goalPlan != null) {
                            break; // Exit the loop if a valid plan is generated
                        }
                    } catch (Exception e) {
                        attempt++;
                    }
                }
                agentResults.put("genGoalAttempts1", String.valueOf(attempt));
            }

            JSONParser parser = new JSONParser();
            try {
                JSONObject convDataSet = (JSONObject) parser.parse(convDataSetString.get());
                ArrayList<Object> inputList = new ArrayList<Object>();
                inputList.add(convDataSet);

                ArrayList<Object> outputList = this.goalPlan.runCode(inputList);
                Boolean checkStatus = (Boolean) outputList.get(0);

                System.out.println("A: Goal check: " + checkStatus);

                agentResults.put("goalResults", String.valueOf(checkStatus));
                return checkStatus;
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        public void setConvDataSetString(String val) {
            convDataSetString.set(val);
        }

        public String getConvDataSetString() {
            return convDataSetString.get();
        }
    }

    /**
     * Constructor
     */
    public GlassesAgent(String chatUrl, String apiKey, String dataSetPath) {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.beliefType = "java.util.ArrayList";
        this.dataSetPath = dataSetPath;

        System.out.println("A: GlassesAgent class loaded");
    }

    @OnStart
    public void body() {
        System.out.println("A: Agent " + agent.getId() + " active");
        agentResults.put("agentId", agent.getId().toString());

        //read Dateset jsonarray im constructor laden und befüllen
        String dataSetFileString = null;
        try {
            dataSetFileString = FileUtils.readFileToString(new File(dataSetPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            JSONParser parser = new JSONParser();
            JSONObject dataset = (JSONObject) parser.parse(dataSetFileString);
            datasetString.set(dataset.toString());

        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getAgentResults() {
        return this.agentResults;
    }

    @Plan(trigger = @Trigger(goals = AgentGoal.class))
    protected void generatePlan1(AgentGoal goal) {
        /** Initialize the LlmFeature */
        System.out.println("--->Test Plan 1");

        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                beliefType,
                "application/bdi-llm/src/main/java/jadex.llm/glasses/settings/Plan1Settings_ollama.json");

        long genStart = System.currentTimeMillis();
        llmFeature.connectToLLM("");
        long genTime = System.currentTimeMillis() - genStart;
        agentResults.put("genTime1", String.valueOf(genTime));


        IPlanBody plan = null;
        int attempt = 0;
        while (attempt < 3) {
            try {
                plan = llmFeature.generateAndCompileCode();
                agentResults.put("generatedPlanCode1", llmFeature.generatedJavaCode);

                if (plan != null) {
                    break; // Exit the loop if a valid plan is generated
                }
            } catch (Exception e) {
                attempt++;
            }
        }
        agentResults.put("genPlanAttempts1", String.valueOf(attempt));

        JSONParser parser = new JSONParser();
        try {
            JSONObject dataset = (JSONObject) parser.parse(goal.getConvDataSetString());
            ArrayList<Object> inputList = new ArrayList<Object>();
            inputList.add(dataset);

            long execStart = System.currentTimeMillis();
            ArrayList<Object> outputList = plan.runCode(inputList);
            long execTime = System.currentTimeMillis() - execStart;
            agentResults.put("execTime1", String.valueOf(execTime));

            JSONObject convDataSet = (JSONObject) outputList.get(0);
            System.out.println("A: Plan 1 finished");
            agentResults.put("planResults1", convDataSet.toString());
            goal.setConvDataSetString(convDataSet.toString());
        } catch (Exception e) {
            agentResults.put("planResults1", String.valueOf(e));
        }
    }

    @OnEnd
    public void end() {
        System.out.println("A: Agent " + agent.getId() + " terminated");
    }
}
