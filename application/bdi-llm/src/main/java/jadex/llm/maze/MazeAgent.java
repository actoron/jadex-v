package jadex.llm.maze;

import jadex.bdi.annotation.*;
import jadex.bdi.llm.impl.LlmFeature;
import jadex.bdi.llm.impl.inmemory.IPlanBody;
import jadex.bdi.runtime.Val;
import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.llm.maze.Maze;
import jadex.model.annotation.OnStart;
import jadex.bdi.runtime.Val;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.awt.Point;

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

            Point mazeEndPoint = maze.getEnd();
            // Check if the agent has reached the end position
            if (mazeEndPoint.equals(maze.jadexStringToPoint(updatedPositionString.get()))) {
                System.out.println("A: Agent reached the end position");
                return true;
            } else {
                return false;
            }
        }

        public void setUpdatedPositionString(String point)
        {
            updatedPositionString.set(point);
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
        this.beliefType = "java.awt.Point";
        this.settingsPath = "application/bdi-llm/src/main/java/jadex/llm/maze/PlanSettings.json"; //TODO: Right path?

        System.out.println("A: " + chatUrl);
        System.out.println("A: " + apiKey);

        System.out.println("A: MazeAgent class loaded");
    }

    @OnStart
    public void body()
    {
        System.out.println("A: Agent " +agent.getId()+ " active");

        Point mazeStartPoint = maze.getStart();
        System.out.println("Start position: (" + mazeStartPoint.x + ", " + mazeStartPoint.y + ")");

        // Temp Jadexhandler for Belief<String>
        mazeBeliefPositionString.set(maze.jadexPointToString(mazeStartPoint));
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
        llmFeature.generateAndCompileCode();

        IPlanBody plan = llmFeature.generateAndCompileCode();
        JSONParser parser = new JSONParser();
        //TODO: Implement plan execution
        // Set the updated position string
//        try {
//            JSONObject maze = (JSONObject) parser.parse(goal.getUpdatedPositionString());
//            JSONObject updatedPositionString = (JSONObject) plan.runCode(maze);
//            goal.setUpdatedPositionString(updatedPositionString.toString());
//        } catch (ParseException e) {
//            throw new RuntimeException(e);
//        }
    }

    /**
     *  Start Maze Agent.
     * @throws InterruptedException
     */
    public static void main(String[] args)
    {
        System.out.println("A: Maze main");

        // Create a new maze
        Maze maze = new Maze(20, 20, 5);

        // Create a new MazeAgent
        IComponent.create(new MazeAgent(
                "https://api.openai.com/v1/chat/completions",
                System.getenv("OPENAI_API_KEY"),
                maze)
        );
        IComponent.waitForLastComponentTerminated();



        // Print start & end
        Point start = maze.getStart();
        Point end = maze.getEnd();
        System.out.println("Start position: (" + start.x + ", " + start.y + ")");
        System.out.println("End position: (" + end.x + ", " + end.y + ")");

//        List<Point> foodPositions = maze.getFood();
//        for (int i = 0; i < foodPositions.size(); i++) {
//            Point food = foodPositions.get(i);
//            System.out.println("Food position " + (i + 1) + ": (" + food.x + ", " + food.y + ")");
//        }

        // Test the environment view from the start position facing "up"
        String direction = "right";
        String envView = maze.getEnvironmentView(start.x, start.y, direction);
        System.out.println("Environment View (Facing "+ direction +"):\n" + envView);

        // Display the maze with the agent at the start position facing "up"
        maze.displayMaze();

        // Display the Manhattan distance from the start to the end
        int distance = maze.calculateManhattanDistance(start.x, start.y);
        System.out.println("Manhattan Distance from Start to End: " + distance);


    }
}
