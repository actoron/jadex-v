package jadex.llm.maze;

import jadex.core.IComponent;
import jadex.micro.annotation.Agent;
import jadex.model.annotation.OnStart;

import java.awt.Point;
import java.util.List;

@Agent(type="bdip")
public class MazeAgent
{

    @Agent
    protected IComponent maze;

    private final String chatUrl;
    private final String apiKey;


    /**Constructor*/
    public MazeAgent(String chatUrl, String apiKey) {
        this.chatUrl = chatUrl;
        this.apiKey = apiKey;
    }

@OnStart
public void body()
{
    System.out.println("Agent " +maze.getId()+ " started");
}

    public static void main(String[] args)
    {
        System.out.println("A: Maze main");

        IComponent.create(new MazeAgent(
                "https://api.openai.com/v1/chat/completions",
                System.getenv("OPENAI_API_KEY")
        ));



        Maze maze = new Maze(20, 20, 5);

        // Print start & end
        Point start = maze.getStart();
        Point end = maze.getEnd();
        System.out.println("Start position: (" + start.x + ", " + start.y + ")");
        System.out.println("End position: (" + end.x + ", " + end.y + ")");

        List<Point> foodPositions = maze.getFood();
        for (int i = 0; i < foodPositions.size(); i++) {
            Point food = foodPositions.get(i);
            System.out.println("Food position " + (i + 1) + ": (" + food.x + ", " + food.y + ")");
        }

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
