package jadex.llm.maze;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Random;

import static java.lang.Math.subtractExact;

public class Maze
{

    /** Every cell has x,y coordinates */
    public class Cell
    {
        int x;
        int y;
        boolean[] walls;
        boolean visited;
        boolean current;
        int status;
        boolean agent;

        /**Constructor for maze generation*/
        private Cell(int x, int y)
        {
            this.x = x;
            this.y = y;
            this.walls = new boolean[] {true, true, true, true}; // top, right, bottom, left
            this.visited = false;
            this.current = false;
            this.status = -1;
            this.agent = false;
        }

        /**Constructor for Cell reconstruction*/
        private Cell(String jsonStringCell)
        {
            JSONTokener jt = new JSONTokener(jsonStringCell);
            JSONObject json = new JSONObject(jt);

            this.x = json.getInt("x");
            this.y = json.getInt("y");
            this.walls = new boolean[] {json.getBoolean("top"),
                                        json.getBoolean("right"),
                                        json.getBoolean("bottom"),
                                        json.getBoolean("left")};
            this.visited = json.getBoolean("visited");
            this.current = json.getBoolean("current");
            this.status = json.getInt("status");
            this.agent = json.getBoolean("agent");
        }

        /** Converts this Cell object to a JSON string for later reconstruction*/
        public String toJSONString() {
            JSONObject json = new JSONObject();
            json.put("x", x);
            json.put("y", y);
            json.put("top", walls[0]);
            json.put("right", walls[1]);
            json.put("bottom", walls[2]);
            json.put("left", walls[3]);
            json.put("visited", visited);
            json.put("current", current);
            json.put("status", status);
            json.put("agent", agent);
            return json.toString();
        }

        private Cell checkNeighbours()
        {
            ArrayList<Cell> neighbours = new ArrayList<>();
            if (y > 0)
            {
                Cell top = maze[x][y - 1];
                if (top != null && !top.visited)
                {
                    neighbours.add(top);
                }
            }

            if (x < cols - 1)
            {
                Cell right = maze[x + 1][y];
                if (right != null && !right.visited)
                {
                    neighbours.add(right);
                }
            }

            if (y < rows - 1)
            {
                Cell bottom = maze[x][y + 1];
                if (bottom != null && !bottom.visited)
                {
                    neighbours.add(bottom);
                }
            }

            if (x > 0) {
                Cell left = maze[x - 1][y];
                if (left != null && !left.visited) {
                    neighbours.add(left);
                }
            }

            if(!neighbours.isEmpty())
            {
                int rand = (int) Math.floor(Math.random() * neighbours.size());
                Cell chosen = neighbours.get(rand);
                return chosen;
            } else
            {
                return null;
            }
        }
    }

    private final int cols, rows;
    private final Cell[][] maze;
    private int currentX = 0;
    private int currentY = 0;
    private ArrayList<Cell> leafCells = new ArrayList<>();
    private final int startX;
    private final int startY;
    private Cell endCell;

    /** Constructor */
    public Maze(int cols, int rows, int startX, int startY)
    {
        this.cols = cols;
        this.rows = rows;
        this.startX = startX;
        this.startY = startY;
        this.maze = new Cell[cols][rows];

        initializeMaze();
        setInitialCell(startX, startY);

        //algorithm to generate maze
        Cell current = currentCell();
        //Mark current cell as visited and put into ArrayList
        current.visited = true;
        ArrayList<Cell> path = new ArrayList<>();
        path.add(current);

        boolean leaf = false;
        while(!path.isEmpty())
        {
            current = path.get(path.size()-1);
//            System.out.println("Current cell: " + current.x + ", " + current.y);
            Cell next = current.checkNeighbours();
            if (next != null)
            {
                //Mark the next cell as visited and put into ArrayList
                next.visited = true;
                path.add(next);
                //Remove wall between current & next
                removeWall(current, next);
                //track if found first leafCell
                leaf = false;
            } else
            {
                path.remove(path.size() - 1);
                //only add first cell where backtracking starts
                if (!leaf) {
                    leafCells.add(current);
//                    System.out.println("Leaf cell: " + current.x + ", " + current.y);
                    leaf = true;
                }
            }
        }

        Random random = new Random();
        int randomIndex = random.nextInt(leafCells.size()); // Generate a random index
        this.endCell = leafCells.get(randomIndex);         // Pick a random cell
        //set endCell status
        maze[endCell.x][endCell.y].status = 1;
    }

    private void initializeMaze()
    {
        for (int x = 0; x < cols; x++)
        {
            for (int y = 0; y < rows; y++) {
                maze[x][y] = new Cell(x, y);

                //set start cell
                if (x == startX && y == startY) {
                    maze[x][y].status = 0;
                }
            }
        }
    }

    private void setInitialCell(int x, int y)
    {
        if (x >= 0 && x < cols && y >= 0 && y < rows) {
            currentX = x;
            currentY = y;
            visitedCell(currentX, currentY);
        } else {
            System.out.println("Invalid current cell position.");
        }
    }

    private void visitedCell(int x, int y)
    {
        Cell cell = maze[x][y];
        if(!cell.visited)
        {
            cell.visited = true;
        }
    }

    private Cell currentCell()
    {
        return maze[currentX][currentY];
    }

    public void displayMaze()
    {
        //Iterate over each row (y) first for print in console
        for (int y = 0; y < rows; y++) {
            //print top walls
            for (int x = 0; x < cols; x++) {
                Cell cell = maze[x][y];
                System.out.print(cell.walls[0] ? "+ - " : "+   ");
            }
            System.out.println("+");

            //print left/right walls
            for (int x = 0; x < cols; x++) {
                Cell cell = maze[x][y];

                if (cell.agent) {
                    if (cell.status == 0)
                    {
                        System.out.print(cell.walls[3] ? "| S " : "  S ");
                    } else if (cell.status == 1)
                    {
                        System.out.print(cell.walls[3] ? "| E " : "  E ");
                    } else
                    {
                        System.out.print(cell.walls[3] ? "| A " : "  A ");
                    }
                } else {
                    if (cell.status == 0)
                    {
                        System.out.print(cell.walls[3] ? "| S " : "  S ");
                    } else if (cell.status == 1)
                    {
                        System.out.print(cell.walls[3] ? "| E " : "  E ");
                    } else
                    {
                        System.out.print(cell.walls[3] ? "|   " : "    ");
                    }
                }

            }

            //print right wall of last cell
            System.out.println("|");

            if (y == rows - 1) {
                //print bottom walls
                for (int x = 0; x < cols; x++) {
                    Cell cell = maze[x][y];
                    System.out.print(cell.walls[2] ? "+ - " : "+   ");
                }
                System.out.println("+");
            }
        }
    }

    private void removeWall(Cell current, Cell next)
    {
        int xDiff = subtractExact(current.x, next.x);
        int yDiff = subtractExact(current.y, next.y);

        if (xDiff == 1)
        {
            current.walls[3] = false;
            next.walls[1] = false;
        } else if (xDiff == -1)
        {
            current.walls[1] = false;
            next.walls[3] = false;
        }

        if (yDiff == 1)
        {
            current.walls[0] = false;
            next.walls[2] = false;
        } else if (yDiff == -1)
        {
            current.walls[2] = false;
            next.walls[0] = false;
        }
    }

    public int[] getEndPosition()
    {
        return new int[]{endCell.x, endCell.y};
    }

    public String getCurrentPosition()
    {
        return currentCell().toJSONString();
    }

    public String getCell(int x, int y)
    {
        return maze[x][y].toJSONString();
    }

    public void setAgentPosition(int x, int y, boolean agentStatus)
    {
        maze[x][y].agent = agentStatus;
    }

    //for testing
//    public static void main(String[] args)
//    {
//        int rows = 10;
//        int cols = 10;
//        int startX = 2;
//        int startY = 2;
//        Maze maze1 = new Maze(cols, rows, startX, startY);
//        System.out.println(maze1.getCurrentPosition());
//        maze1.displayMaze();
//    }
}
