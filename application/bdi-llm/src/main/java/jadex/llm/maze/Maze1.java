package jadex.llm.maze;

import java.util.ArrayList;

public class Maze1
{
    /** Every cell has x,y coordinates */
    private class Cell
    {
        int x;
        int y;

        public Cell(int x, int y)
        {
            this.x = x;
            this.y = y;
        }
    }

    private final int cols, rows;
    private final ArrayList<Cell> maze;

    /** Constructor */
    public Maze1(int cols, int rows)
    {
        this.cols = cols;
        this.rows = rows;
        this.maze = new ArrayList<>(rows * cols);

        initializeMaze();
    }


    private void initializeMaze()
    {
        for (int x = 0; x < rows; x++)
        {
            for (int y = 0; y < cols; y++)
            {
                Cell cell = new Cell(x, y);
                maze.add(cell);

            }
        }
    }

    public void displayMaze()
    {
        for (int x = 0; x < rows; x++) {
            for (int y = 0; y < cols; y++) {
                Cell cell = maze.get(x * cols + y);
                System.out.print("█"); // Print the cell
            }
            System.out.println(); // Newline after each row
        }
//        for (Cell cell : maze)
//        {
//            System.out.println("Cell: " + cell.x + " " + cell.y);
//        }
    }

    public static void main(String[] args) {
        Maze1 maze1 = new Maze1(3, 3);
        maze1.displayMaze();
    }
}
