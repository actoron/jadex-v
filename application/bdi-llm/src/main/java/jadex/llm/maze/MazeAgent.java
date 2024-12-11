package jadex.llm.maze;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.llm.impl.inmemory.IPlanBody;
import jadex.bdi.runtime.Val;
import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

import java.util.ArrayList;
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
        protected Val<String> updatedPositionString;

        @GoalCreationCondition(beliefs = "mazeBeliefPositionString")
        public AgentGoal(String updatedPositionString) {
            this.updatedPositionString = new Val<>(updatedPositionString);
            System.out.println("A: Goal created");
        }

        @GoalFinished
        public void goalFinished() {
            System.out.println("A: Goal finished");
            System.out.printf("Data: " + updatedPositionString.get());
        }

        @GoalTargetCondition(parameters = "updatedPositionString")
        public boolean checkTarget() {
            System.out.println("-->Test Goal");

            Maze.Cell mazeEnd = maze.getEndCell();
            // Check if the agent has reached the end position
            if (mazeEnd.equals(maze.jadexStringToCell(updatedPositionString.get()))) {
                System.out.println("A: Agent reached the end position");
                return true;
            } else {
                return false;
            }
        }

        public void setUpdatedPositionString(String cell)
        {
            updatedPositionString.set(cell);
        }

        public String getUpdatedPositionString()
        {
            return updatedPositionString.get();
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

        Maze.Cell mazeStart = maze.getStartCell();
        System.out.println("Start position: (" + mazeStart.x + ", " + mazeStart.y + ")");

        // Temp Jadexhandler for Belief<String>
        mazeBeliefPositionString.set(maze.jadexCellToString(mazeStart));
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

        llmFeature.generateAndCompileCode();

        IPlanBody plan = llmFeature.generateAndCompileCode();

        Object brain = new Object();

        System.out.println(goal.getUpdatedPositionString());

        for (int i = 0; i < 50; i++) {
            System.out.println("Step " + i);

            // 1. get current position from goal in mazePos (agent)
            Maze.Cell mazePosition = maze.jadexStringToCell(goal.getUpdatedPositionString());

            // put mazePos, envView & brain into ArrayList
            ArrayList<Object> inputList = new ArrayList<Object>();
            inputList.add(mazePosition);
//            inputList.add(brain);

            // chatty run on given List, return List and extract Objects
            ArrayList<Object> outputList = plan.runCode(inputList);
            Maze.Cell retMazePos = (Maze.Cell) outputList.get(0);
//            brain = outputList.get(2);

            // set updated mazePos from chatty and return to console
            goal.setUpdatedPositionString(maze.jadexCellToString(retMazePos));
            System.out.println(mazePosition);
            maze.setAgent(retMazePos);
//            maze.removeAgent(mazePosition);

            // display maze, delete retMazePos from console output and wait oyne second
            maze.displayMaze();
            System.out.println(retMazePos);
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

        // Create a new MazeAgent
        IComponentManager.get().create(new MazeAgent(
                "https://api.openai.com/v1/chat/completions",
                System.getenv("OPENAI_API_KEY"),
                maze)
        );
        IComponentManager.get().waitForLastComponentTerminated();
    }
}
