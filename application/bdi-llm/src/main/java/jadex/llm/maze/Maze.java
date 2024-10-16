package jadex.llm.maze;

import java.util.*;

public class Maze {
    // Nested class representing each block in the maze
    private class Block {
        int status; // 0: free, 1: wall, 2: food, 3: start, 4: end

        public Block(int status) {
            this.status = status;
        }
    }

    private Block[][] maze;
    private int height, width;
    private int foodCount;
    private Random rand = new Random();
    private int startX, startY;
    private int endX, endY;

    // Constructor initializes the maze and places start, food, and end
    public Maze(int height, int width, int foodCount) {
        this.height = height;
        this.width = width;
        this.foodCount = foodCount;
        maze = new Block[height][width];

        initializeMaze();            // Set up the outer walls and free space inside
        placeStartFoodAndEnd();      // Ensure start, food, and end are reachable
        generateWalls();             // Randomly place walls while keeping maze accessible
    }

    // Initialize the maze with outer walls and free space inside
    private void initializeMaze() {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (i == 0 || i == height - 1 || j == 0 || j == width - 1) {
                    maze[i][j] = new Block(1); // Outer walls
                } else {
                    maze[i][j] = new Block(0); // Free space
                }
            }
        }
    }

    // Carve a path between two points by clearing blocks
    private void carvePath(int startX, int startY, int endX, int endY) {
        int currentX = startX;
        int currentY = startY;

        // Greedily carve a path by alternating between horizontal and vertical moves
        while (currentX != endX || currentY != endY) {
            if (currentX < endX) {
                currentX++;
            } else if (currentX > endX) {
                currentX--;
            } else if (currentY < endY) {
                currentY++;
            } else if (currentY > endY) {
                currentY--;
            }

            // Ensure the path is clear (free space), but don't overwrite start, food, or end
            if (maze[currentX][currentY].status == 0) {
                maze[currentX][currentY].status = 0;
            }
        }
    }

    // Place the start, food, and end points, and carve paths between them
    private void placeStartFoodAndEnd() {
        // Place the start and end point
        placeStart();

        // List to store food coordinates
        List<int[]> foodPositions = new ArrayList<>();

        // Place food at random positions
        int foodPlaced = 0;
        while (foodPlaced < foodCount) {
            int x = rand.nextInt(height - 2) + 1;
            int y = rand.nextInt(width - 2) + 1;
            if (maze[x][y].status == 0) {
                maze[x][y].status = 2; // Place food
                foodPositions.add(new int[]{x, y});
                foodPlaced++;
            }
        }

        // Carve a path from the start to the first food block
        int[] firstFood = foodPositions.get(0);
        carvePath(startX, startY, firstFood[0], firstFood[1]);

        // Carve paths between all subsequent food blocks
        for (int i = 0; i < foodPositions.size() - 1; i++) {
            int[] currentFood = foodPositions.get(i);
            int[] nextFood = foodPositions.get(i + 1);
            carvePath(currentFood[0], currentFood[1], nextFood[0], nextFood[1]);
        }

        // Place a single end point after the last food
        placeEnd(foodPositions.get(foodPositions.size() - 1));
    }

    // Place the start position at a random free space
    private void placeStart() {
        while (true) {
            int x = rand.nextInt(height - 2) + 1;
            int y = rand.nextInt(width - 2) + 1;
            if (maze[x][y].status == 0) {
                maze[x][y].status = 3; // Start point
                startX = x;
                startY = y;
                break;
            }
        }
    }

    // Place the end position at a random free space, placed after the last food
    private void placeEnd(int[] lastFood) {
        int foodX = lastFood[0];
        int foodY = lastFood[1];
        while (true) {
            int x = rand.nextInt(height - 2) + 1;
            int y = rand.nextInt(width - 2) + 1;
            if (maze[x][y].status == 0) {
                maze[x][y].status = 4; // End point
                endX = x;
                endY = y;
                carvePath(foodX, foodY, endX, endY); // Carve path from last food to end
                break;
            }
        }
    }

    // Method to change the environment when the agent reaches a food block
    public void consumeFood(int x, int y) {
        if (maze[x][y].status == 2) { // If the block is food
            maze[x][y].status = 0; // Change to free space
        } else {
            System.out.println("No food at the specified location (" + x + ", " + y + ")");
        }
    }

    // Generate random walls but ensure accessibility from start to all food and end
    private void generateWalls() {
        for (int i = 1; i < height - 1; i++) {
            for (int j = 1; j < width - 1; j++) {
                if (maze[i][j].status == 0 && rand.nextInt(4) == 0) { // 25% chance to place a wall
                    maze[i][j].status = 1;
                }
            }
        }
        ensureAccessibility(); // Ensure paths remain accessible
    }

    // Simple BFS to ensure all food and end blocks are reachable from the start
    private void ensureAccessibility() {
        boolean[][] visited = new boolean[height][width];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int x = current[0];
            int y = current[1];

            // Try all 4 directions
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] d : directions) {
                int newX = x + d[0];
                int newY = y + d[1];
                if (newX >= 0 && newX < height && newY >= 0 && newY < width && !visited[newX][newY]) {
                    if (maze[newX][newY].status != 1) { // Not a wall
                        queue.add(new int[]{newX, newY});
                        visited[newX][newY] = true;
                    }
                }
            }
        }

        // Fix any unreachable food or end blocks
        for (int i = 1; i < height - 1; i++) {
            for (int j = 1; j < width - 1; j++) {
                if ((maze[i][j].status == 2 || maze[i][j].status == 4) && !visited[i][j]) {
                    maze[i][j].status = 0; // Convert to free space if unreachable
                }
            }
        }
    }

    // Display the maze with enhanced visual clarity
    public void displayMaze() {

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (maze[i][j].status == 1) {
                    System.out.print("+ "); // Wall
                } else if (maze[i][j].status == 2) {
                    System.out.print("F "); // Food
                } else if (maze[i][j].status == 3) {
                    System.out.print("S "); // Start
                } else if (maze[i][j].status == 4) {
                    System.out.print("E "); // End point
                } else {
                    System.out.print("  "); // Free space
                }
            }
            System.out.println(); // Move to the next row
        }
    }

    // Function to return a "world view" of the surrounding blocks and current block as JSON
    public String getEnvironmentView(int x, int y, String direction) {
        Map<String, Object> environment = new HashMap<>();
        int[][] forwardOffsets = switch (direction.toLowerCase()) {
            case "up" -> new int[][]{{-1, 0}, {0, -1}, {0, 1}, {1, 0}};  // front, left, right, back
            case "down" -> new int[][]{{1, 0}, {0, 1}, {0, -1}, {-1, 0}};
            case "left" -> new int[][]{{0, -1}, {1, 0}, {-1, 0}, {0, 1}};
            case "right" -> new int[][]{{0, 1}, {-1, 0}, {1, 0}, {0, -1}};
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };

        String[] dirNames = {"front", "left", "right", "back"};

        // Look in front, left, right, and back, considering walls
        for (int i = 0; i < 4; i++) {
            List<Integer> view = new ArrayList<>();
            int viewDistance = (i == 0) ? 3 : 1; // 3 positions in front, 1 in left, right, back
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

        // Convert environment map to JSON-like string
        return environment.toString();
    }

    // Calculate Manhattan distance from current position to the end
    public int calculateManhattanDistance(int x, int y) {
        return Math.abs(x - endX) + Math.abs(y - endY);
    }

    // Method to get the start position
    public int[] getStart() {
        return new int[]{startX, startY}; // Return the start coordinates
    }
}
