package jadex.llm.maze;

import java.awt.*;
import java.util.*;
import java.util.List;
import org.json.simple.JSONObject;

/** Maze class w/ Console environment
 * - set border, walls, food, start & end
 * - agent can look around, 3 Pos in front (or next wall), 1 Pos in left, right, back
 * - Manhattan distance for calculate the distance between actual position and end*/
public class Maze
{
    private class Block
    {
        int status; // 0: free, 1: wall, 2: food, 3: start, 4: end, 5: agent

        public Block(int status)
        {
            this.status = status;
        }
    }

    private Block[][] maze;
    private int height, width;
    private int foodCount;
    private Random rand = new Random();

    private Point start;
    private Point end;

    private List<Point> foodPositions;

    /** Constructor */
    public Maze(int height, int width, int foodCount)
    {
        this.height = height;
        this.width = width;
        this.foodCount = foodCount;
        maze = new Block[height][width];
        foodPositions = new ArrayList<>();

        initializeMaze();
        placeStartFoodAndEnd();
        generateWalls();
    }

    /** Getter */
    public Point getStart()
    {
        return start;
    }

    public Point getEnd()
    {
        return end;
    }

    public List<Point> getFood()
    {
        return foodPositions;
    }

    public void setAgent(Point position)
    {
        maze[position.x][position.y].status = 5;
    }

    public void removeAgent(Point position)
    {
        maze[position.x][position.y].status = 0;
    }

    // Initialize the maze with outer walls and free space inside
    private void initializeMaze()
    {
        for (int i = 0; i < height; i++)
        {
            for (int j = 0; j < width; j++)
            {
                if (i == 0 || i == height - 1 || j == 0 || j == width - 1)
                {
                    maze[i][j] = new Block(1); // Outer walls
                } else
                {
                    maze[i][j] = new Block(0); // Free space
                }
            }
        }
    }

    // Carve a path between two points by clearing blocks
    private void carvePath(Point start, Point end)
    {
        int startX = start.x;
        int startY = start.y;
        int endX = end.x;
        int endY = end.y;

        // Greedily carve a path by alternating between horizontal and vertical moves
        while (startX != endX || startY != endY)
        {
            if (startX < endX)
            {
                startX++;
            } else if (startX > endX)
            {
                startX--;
            } else if (startY < endY)
            {
                startY++;
            } else if (startY > endY)
            {
                startY--;
            }

            //clear path without overwriting set items
            if (maze[startX][startY].status == 0)
            {
                maze[startX][startY].status = 0;
            }
        }
    }

    // Place the start, food, and end points, and carve paths between them
    private void placeStartFoodAndEnd() {
        placeStart();

        int foodPlaced = 0;
        while (foodPlaced < foodCount) {
            int x = rand.nextInt(height - 2) + 1;
            int y = rand.nextInt(width - 2) + 1;
            if (maze[x][y].status == 0) {
                maze[x][y].status = 2;
                foodPositions.add(new Point(x, y));
                foodPlaced++;
            }
        }

        Point firstFood = foodPositions.get(0);
        carvePath(start, firstFood);

        for (int i = 0; i < foodPositions.size() - 1; i++) {
            Point currentFood = foodPositions.get(i);
            Point nextFood = foodPositions.get(i + 1);
            carvePath(currentFood, nextFood);
        }

        placeEnd(foodPositions.get(foodPositions.size() - 1));
    }

    // Place the start position at a random free space
    private void placeStart()
    {
        while (true)
        {
            int x = rand.nextInt(height - 2) + 1;
            int y = rand.nextInt(width - 2) + 1;
            if (maze[x][y].status == 0) {
                maze[x][y].status = 3; // Start point
                start = new Point(x, y);
                break;
            }
        }
    }

    // Place the end position at a random free space, placed after the last food
    private void placeEnd(Point lastFood)
    {
        while (true)
        {
            int x = rand.nextInt(height - 2) + 1;
            int y = rand.nextInt(width - 2) + 1;
            if (maze[x][y].status == 0)
            {
                maze[x][y].status = 4;
                end = new Point(x, y);
                carvePath(lastFood, end);
                break;
            }
        }
    }

    // Generate random walls, ensure accessibility from all items
    private void generateWalls()
    {
        for (int i = 1; i < height - 1; i++)
        {
            for (int j = 1; j < width - 1; j++)
            {
                if (maze[i][j].status == 0 && rand.nextInt(4) == 0)
                { // 25% chance to place a wall
                    maze[i][j].status = 1;
                }
            }
        }
        ensureAccessibility();
    }

    // Simple Breadth-First-Seaarch (BFS) all items are reachable from the start
    private void ensureAccessibility()
    {
        boolean[][] visited = new boolean[height][width];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{start.x, start.y});
        visited[start.x][start.y] = true;

        while (!queue.isEmpty())
        {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            // Try all 4 directions
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] d : directions)
            {
                int newX = x + d[0];
                int newY = y + d[1];
                if (newX >= 0 && newX < height && newY >= 0 && newY < width && !visited[newX][newY])
                {
                    if (maze[newX][newY].status != 1)
                    { // Not a wall
                        queue.add(new int[]{newX, newY});
                        visited[newX][newY] = true;
                    }
                }
            }
        }

        // Fix unreachable items
        for (int i = 1; i < height - 1; i++)
        {
            for (int j = 1; j < width - 1; j++)
            {
                if ((maze[i][j].status == 2 || maze[i][j].status == 4) && !visited[i][j])
                {
                    maze[i][j].status = 0;
                }
            }
        }
    }

    // Display maze
    public void displayMaze()
    {
        for (int i = 0; i < height; i++)
        {
            for (int j = 0; j < width; j++)
            {
                if (maze[i][j].status == 1)
                {
                    System.out.print("+ "); // Wall
                } else if (maze[i][j].status == 2)
                {
                    System.out.print("F "); // Food
                } else if (maze[i][j].status == 3)
                {
                    System.out.print("S "); // Start
                } else if (maze[i][j].status == 4)
                {
                    System.out.print("E "); // End point
                } else if (maze[i][j].status == 5)
                {
                    System.out.print("A "); // End point
                } else
                {
                    System.out.print("  "); // Free space
                }
            }
            System.out.println(); // Move to the next row
        }
    }

    // Function to return a "world view" of the surrounding blocks and current block as a JSON object
    public JSONObject getEnvironmentView(int x, int y, String direction) {
        JSONObject environment = new JSONObject();
        int[][] forwardOffsets = switch (direction.toLowerCase()) {
            case "front" -> new int[][]{{-1, 0}, {0, -1}, {0, 1}, {1, 0}};  // front, left, right, back
            case "back" -> new int[][]{{1, 0}, {0, 1}, {0, -1}, {-1, 0}};
            case "left" -> new int[][]{{0, -1}, {1, 0}, {-1, 0}, {0, 1}};
            case "right" -> new int[][]{{0, 1}, {-1, 0}, {1, 0}, {0, -1}};
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };

        String[] dirNames = {"front", "left", "right", "back"};

        // Look in front, left, right, and back, considering walls
        for (int i = 0; i < 4; i++) {
            List<Integer> view = new ArrayList<>();
            int viewDistance = (i == 0) ? 3 : 1;
            for (int step = 1; step <= viewDistance; step++) {
                // Calculate the new coordinates for the current direction
                int newX = x + forwardOffsets[i][0] * step;
                int newY = y + forwardOffsets[i][1] * step;

                // Check if the new coordinates are within bounds and add the status of the block
                if (newX >= 0 && newX < height && newY >= 0 && newY < width) {
                    view.add(maze[newX][newY].status);
                    // If a wall is encountered, stop looking further
                    if (maze[newX][newY].status == 1) {
                        break;
                    }
                }
            }
            environment.put(dirNames[i], view); // Add the view for the current direction
        }

        // Convert environment map to a JSON string using JSONObject
        return environment;
    }

    // Calculate Manhattan distance from current position to the end
    public int calculateManhattanDistance(int x, int y)
    {
        return Math.abs(x - end.x) + Math.abs(y - end.y);
    }

    //Point to String function
    public String jadexPointToString(Point point)
    {
        return point.x + "," + point.y;
    }

    //String to Point function
    public Point jadexStringToPoint(String pointString)
    {
        String[] pointStringValue = pointString.split(",");
        return new Point(Integer.parseInt(pointStringValue[0]), Integer.parseInt(pointStringValue[1]));
    }

    //change env when agent reaches food
    public void consumeFood(int x, int y)
    {
        if (maze[x][y].status == 2)
        {
            maze[x][y].status = 0;
        } else
        {
            System.out.println("No food at position: " + x + ", " + y);
        }
    }
}
