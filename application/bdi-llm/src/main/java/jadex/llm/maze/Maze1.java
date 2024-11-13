package jadex.llm.maze;

import java.awt.*;
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
            Cell top = maze.get(index(x, y - 1));
            Cell right = maze.get(index(x + 1, y));
            Cell bottom = maze.get(index(x, y + 1));
            Cell left = maze.get(index(x - 1, y));

            if (top != null && !top.visited)
            {
                neighbours.add(top);
            }

            if (right != null && !right.visited)
            {
                neighbours.add(right);
            }

            if (bottom != null && !bottom.visited)
            {
                neighbours.add(bottom);
            }

            if (left != null && !left.visited)
            {
                neighbours.add(left);
            }

            if(neighbours.size() > 0)
            {
                int rand = (int) Math.floor(Math.random() * neighbours.size());
                return neighbours.get(rand);
            } else
            {
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
        return x * cols + y ;
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
                    System.out.print(cell.walls[3] ? "| █ " : "  █ ");
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

    public static void main(String[] args) {
        Maze1 maze1 = new Maze1(3, 5);
        maze1.setCurrentCell(2,1);

//        maze1.displayMaze();

        Cell current = maze1.maze.get(maze1.index(maze1.currentX, maze1.currentY));
        System.out.println(current.x + " " + current.y);
        Cell next = current.checkNeighbours();

//        while (next != null) {
//            next.visited = true;
//            current = next;
//            maze1.displayMaze();
//        }
    }
}
