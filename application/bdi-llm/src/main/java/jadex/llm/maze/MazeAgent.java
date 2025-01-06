package jadex.llm.maze;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.llm.impl.inmemory.IPlanBody;
import jadex.bdi.runtime.Val;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Agent(type="bdip")
public class MazeAgent
{

    @Agent
    protected IComponent agent;

    private final String chatUrl;
    private final String apiKey;
    private Maze maze;
    private final String settingsPath;
    private final String beliefType;

    @Belief
    private Val<String> mazeBeliefPositionString;

    @Goal
    public class AgentGoal {
        @GoalParameter
        protected Val<String> updatedCellJSONString;

        @GoalCreationCondition(beliefs = "mazeBeliefPositionString")
        public AgentGoal(String updatedCellJSONString) {
            this.updatedCellJSONString = new Val<>(updatedCellJSONString);
            System.out.println("A: Goal created");
        }

        @GoalFinished
        public void goalFinished() {
            System.out.println("A: Goal finished");
            System.out.printf("Data: " + updatedCellJSONString.get());
        }

        @GoalTargetCondition(parameters = "updatedCellJSONString")
        public boolean checkTarget() {
            System.out.println("-->Test Goal");

            int mazeEndX = maze.getEndPosition()[0];
            int mazeEndY = maze.getEndPosition()[1];

            JSONTokener jt = new JSONTokener(updatedCellJSONString.get());
            JSONObject json = new JSONObject(jt);

            int currentX = json.getInt("x");
            int currentY = json.getInt("y");

            // Check if the agent has reached the end position
            if (mazeEndX == currentX && mazeEndY == currentY) {
                System.out.println("A: Agent reached the end position");
                return true;
            } else {
                return false;
            }
        }

        public void setUpdatedCellJSONString(String cell)
        {
            updatedCellJSONString.set(cell);
        }

        public String getUpdatedCellJSONString()
        {
            return updatedCellJSONString.get();
        }
    }

    /**Constructor*/
    public MazeAgent(String chatUrl, String apiKey, Maze maze) {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
        this.maze = maze;
        this.beliefType = "java.util.ArrayList";
        this.settingsPath = "application/bdi-llm/src/main/java/jadex/llm/maze/settings/PlanSettings.json";

        System.out.println("A: " + chatUrl);
        System.out.println("A: " + apiKey);

        System.out.println("A: MazeAgent class loaded");
    }

    @OnStart
    public void body()
    {
        System.out.println("A: Agent " +agent.getId()+ " active");

        // Temp Jadexhandler for Belief<String>
        mazeBeliefPositionString.set(maze.getCurrentPosition());

        System.out.println("StartCell: " + mazeBeliefPositionString.get());
    }

    @Plan(trigger=@Trigger(goals=AgentGoal.class))
    protected void generatePlan(AgentGoal goal)
    {
        /** Initialize the LlmFeature */
        System.out.println("--->Test Plan 1");
        LlmFeature llmFeature = new LlmFeature(
                chatUrl,
                apiKey,
                beliefType,
                settingsPath);

        llmFeature.connectToLLM("");

        System.out.println(llmFeature.generatedJavaCode);

        IPlanBody plan = llmFeature.generateAndCompileCode();

        System.out.println(goal.getUpdatedCellJSONString());

        for (int i = 0; i < 50; i++) {
            System.out.println("Step " + i);

            // 1. get current position from goal in mazePos (agent)
            String CellJSONString = goal.getUpdatedCellJSONString();

            // put mazePos, envView & brain into ArrayList
            ArrayList<Object> inputList = new ArrayList<Object>();
            inputList.add(CellJSONString);

            // chatty run on given List, return List and extract Objects
            ArrayList<Object> outputList = plan.runCode(inputList);
            Integer[] newPositions = (Integer[]) outputList.get(0);

            System.out.println("newPositions: " + newPositions[0] + ", " + newPositions[1]);

            // set updated mazePos from chatty and return to console
            goal.setUpdatedCellJSONString(maze.getCell(newPositions[0], newPositions[1]));
            System.out.println(goal.getUpdatedCellJSONString());

            // display maze
            maze.setAgentPosition(newPositions[0], newPositions[1], true);
            maze.displayMaze();
            maze.setAgentPosition(newPositions[0], newPositions[1], false);
            System.out.println("###################################################################################");

            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *  Start Maze Agent.
     * @throws InterruptedException
     */
    public static void main(String[] args)
    {
        System.out.println("A: Maze main");

        // Create a new maze
        Maze maze = new Maze(10, 10, 5, 5);
        maze.displayMaze();

        // Create a new MazeAgent
        IComponentManager.get().create(new MazeAgent(
                "https://api.openai.com/v1/chat/completions",
                System.getenv("OPENAI_API_KEY"),
                maze)
        );

        IComponentManager.get().waitForLastComponentTerminated();
    }
}
