package jadex.llm.maze;

import java.util.Arrays;

public class MazeAgent {
    public static void main(String[] args) {
        Maze maze = new Maze(20, 20, 5);

        // Example of getting the start position
        int[] start = maze.getStart();
        System.out.println("Start Position: " + Arrays.toString(start));

        // Test the environment view from the start position facing "up"
        String envView = maze.getEnvironmentView(start[0], start[1], "right");
        System.out.println("Environment View (Facing Up):\n" + envView);

        // Display the maze with the agent at the start position facing "up"
        System.out.println("Maze with Agent Facing Up:");
        maze.displayMaze(start[0], start[1], "up");

        // Display the Manhattan distance from the start to the end
        int distance = maze.calculateManhattanDistance(start[0], start[1]);
        System.out.println("Manhattan Distance from Start to End: " + distance);
    }
}
