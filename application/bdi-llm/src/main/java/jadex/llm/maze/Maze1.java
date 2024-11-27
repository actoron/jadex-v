package jadex.llm.maze;

import java.util.ArrayList;

public class Maze1
{

    /** Every cell has x,y coordinates */
    private class Cell
    {
        int x;
        int y;
        boolean[] walls = {true, true, true, true}; // top, right, bottom, left;
        boolean visited;
        boolean current;

        public Cell(int x, int y)
        {
            this.x = x;
            this.y = y;
            this.visited = false;
            this.current = false;
        }

        public Cell checkNeighbours()
        {
            ArrayList<Cell> neighbours = new ArrayList<>();
            if (y>0)
            {
                Cell top = maze.get(index(x, y - 1));
                if (top != null && !top.visited)
                {
                    neighbours.add(top);
                }
            }

            if (x < cols - 1)
            {
                Cell right = maze.get(index(x + 1, y));
                if (right != null && !right.visited)
                {
                    neighbours.add(right);
                }
            }

            if (y < rows - 1)
            {
                Cell bottom = maze.get(index(x, y + 1));
                if (bottom != null && !bottom.visited)
                {
                    neighbours.add(bottom);
                }
            }

            if (x > 0) {
                Cell left = maze.get(index(x - 1, y));
                if (left != null && !left.visited) {
                    neighbours.add(left);
                }
            }

            if(!neighbours.isEmpty())
            {
                int rand = (int) Math.floor(Math.random() * neighbours.size());
                Cell chosen = neighbours.get(rand);
//                System.out.println("Found unvisited neighbour: " + chosen.x + ", " + chosen.y);
                return chosen;
            } else
            {
//                System.out.println("No neighbours found");
                return null;
            }
        }
    }

    private final int cols, rows;
    private final ArrayList<Cell> maze;
    private int currentX = 0;
    private int currentY = 0;

    /** Constructor */
    public Maze1(int cols, int rows)
    {
        this.cols = cols;
        this.rows = rows;
        this.maze = new ArrayList<>(rows * cols);

        initializeMaze();
    }

    private int index(int x, int y)
    {
        if (x < 0 || y < 0 || x > cols - 1 || y > rows - 1)
        {
            return -1;
        }
        return x + y * cols ;
    }

    private void initializeMaze()
    {
        for (int y = 0; y < rows; y++)
        {
            for (int x = 0; x < cols; x++)
            {
                Cell cell = new Cell(x, y);
                maze.add(cell);
            }
        }
    }

    public void setCurrentCell(int x, int y)
    {
        if (x >= 0 && x < cols && y >= 0 && y < rows) {
            currentX = x;
            currentY = y;
            visitedCell(currentX, currentY);
        } else {
            System.out.println("Invalid current cell position.");
        }
    }

    public void visitedCell(int x, int y)
    {
        Cell cell = maze.get(index(x,y));
        if(!cell.visited)
        {
            cell.visited = true;
        }
    }

    public void displayMaze()
    {
        for (int y = 0; y < rows; y++) {
            //print top walls
            for (int x = 0; x < cols; x++) {
                Cell cell = maze.get(index(x,y));
                System.out.print(cell.walls[0] ? "+ - " : "+   ");
            }
            System.out.println("+");

            //print left/right walls
            for (int x = 0; x < cols; x++) {
                Cell cell = maze.get(index(x,y));
                if (cell.visited)
                {
                    System.out.print(cell.walls[3] ? "|   " : "    ");
                } else
                {
                    System.out.print(cell.walls[3] ? "|   " : "    ");
                }
            }

            //print right wall of last cell
            System.out.println("|");

            if (y == rows - 1) {
                //print bottom walls
                for (int x = 0; x < cols; x++) {
                    Cell cell = maze.get(index(x,y));
                    System.out.print(cell.walls[2] ? "+ - " : "+   ");
                }
                System.out.println("+");
            }
        }
    }

    public void removeWall(Cell current, Cell next)
    {
        int xDiff = Math.subtractExact(current.x, next.x);
        int yDiff = Math.subtractExact(current.y, next.y);

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

    public static void main(String[] args)
    {
        Maze1 maze1 = new Maze1(10, 10);
        maze1.setCurrentCell(2,2);

        Cell current = maze1.maze.get(maze1.index(maze1.currentX, maze1.currentY));
        //Mark current cell as visited and put into ArrayList
        current.visited = true;
        ArrayList<Cell> path = new ArrayList<>();
        path.add(current);

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
                maze1.removeWall(current, next);
//                System.out.println("Moving to next cell: " + next.x + ", " + next.y);
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
            } else
            {
//                System.out.println("Backtracking from cell: " + current.x + ", " + current.y);
                path.remove(path.size() - 1);
            }
        }
        maze1.displayMaze();
    }
}
