package jadex.llm.maze;

import java.awt.*;
import java.util.ArrayList;

import static java.lang.Math.subtractExact;

public class Maze
{

    /** Every cell has x,y coordinates */
    class Cell
    {
        int x;
        int y;
        boolean[] walls = {true, true, true, true}; // top, right, bottom, left;
        boolean visited;
        boolean current;
        boolean agent;

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

    /** Constructor */
    public Maze(int cols, int rows, int startX, int startY)
    {
        this.cols = cols;
        this.rows = rows;
        this.maze = new Cell[rows][cols];
        this.startX = startX;
        this.startY = startY;

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
        getEndCell();
    }

    private void initializeMaze()
    {
        for (int x = 0; x < cols; x++)
        {
            for (int y = 0; y < rows; y++)
            {
                maze[x][y] = new Cell(x, y);
            }
        }
    }

    public void setInitialCell(int x, int y)
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
        Cell cell = maze[x][y];
        if(!cell.visited)
        {
            cell.visited = true;
        }
    }

    public Cell currentCell()
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
                    Cell cell = maze[x][y];
                    System.out.print(cell.walls[2] ? "+ - " : "+   ");
                }
                System.out.println("+");
            }
        }
    }

    public void removeWall(Cell current, Cell next)
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

    public Cell getEndCell()
    {
        //get endCell from leafCells
        Cell endCell = leafCells.get(leafCells.size()-1);
        System.out.println("End cell: " + endCell.x + ", " + endCell.y);
        return endCell;
    }

    public Cell getStartCell()
    {
        Cell startCell = maze[startX][startY];
        return startCell;
    }

    public void setAgent(Cell position)
    {
        maze[position.x][position.y].agent = true;
    }

    public void removeAgent(Point position)
    {
        maze[position.x][position.y].agent = false;
    }

    //Point to String function
    public String jadexCellToString(Cell cell)
    {
        return cell.x + "," + cell.y;
    }

    //String to Point function
    public Cell jadexStringToCell(String cellString)
    {
        String[] cellStringValue = cellString.split(",");
        return new Cell(Integer.parseInt(cellStringValue[0]), Integer.parseInt(cellStringValue[1]));
    }


//    public static void main(String[] args)
//    {
//        int rows = 7;
//        int cols = 7;
//        int startX = 2;
//        int startY = 2;
//        Maze maze1 = new Maze(cols, rows, startX, startY);
//
//        maze1.displayMaze();
//    }
}
