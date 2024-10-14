package jadex.llm.maze;

import java.util.Arrays;

public class MazeAgent
{
    public static void main(String[] args)
    {
        Maze maze = new Maze(20, 20, 5);
        maze.displayMaze();

        // Example of getting the start position
        int[] start = maze.getStart();
        System.out.println("Start Position: " + Arrays.toString(start));

        // Test the environment view from the start position facing "up"
        String envView = maze.getEnvironmentView(start[0], start[1], "up");
        System.out.println("Environment View from Start Position: " + envView);

        // Simulate consuming food
        // Assume we check the environment view and find food to consume
        int foodX = start[0] + 1; // Example position to consume food
        int foodY = start[1];     // Directly in front of the start
        maze.consumeFood(foodX, foodY);

        // Display maze after consuming food
        System.out.println("Maze after consuming food:");
        maze.displayMaze();
    }
}
