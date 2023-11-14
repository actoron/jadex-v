package jadex.micro.gobble;

import java.util.ArrayList;
import java.util.List;

public class Board 
{
	public record Move(int row, int col, int size, int player) 
	{
	}
	
	public record WinStatus(boolean hasPotentiallyWon, int minSize) 
	{
	}
	
	interface IChangeListener 
	{
	    public default void onChange(String message) 
	    {
	    }
	}
	
    private int rows;
    private int cols;
    private int invsize;
    private List<Inventory> inv;
    private List<Move>[][] board;
    private List<IChangeListener> listeners;
    private List<int[]> wincombi;
    private Integer winner;
    private int turn;
    private Move lastghost;
    private int invcount;

    public Board(int rows, int cols, int invsize) 
    {
        this.rows = rows;
        this.cols = cols;
        this.invsize = invsize;
        this.inv = new ArrayList<>();
        this.inv.add(new Inventory(invsize));
        this.inv.add(new Inventory(invsize));
        this.board = new List[rows][cols];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                board[row][col] = new ArrayList<>();
            }
        }
        this.listeners = new ArrayList<>();
        this.wincombi = null;
        this.winner = null;
        this.turn = 0;
        this.lastghost = null;
        this.invcount = inv.get(0).getInventoryCount() + inv.get(1).getInventoryCount();
    }

    public List<Move> getContent(int row, int col) 
    {
        return board[row][col];
    }

    public void addChangeListener(IChangeListener listener) 
    {
        this.listeners.add(listener);
    }

    public void notifyChange(String message) 
    {
        for(IChangeListener listener : this.listeners) 
        {
            listener.onChange(message);
        }
    }

    public int getPlayer() 
    {
        return turn;
    }

    public void makeMove(Move move)
    {
    	if(move.player()!=getPlayer())
    		throw new RuntimeException("Move of wrong player: "+move);
    	placeGhost(move.row(), move.col(), move.size);
    	finishMove();
    }
    
    public void placeGhost(int row, int col, int gsize) 
    {
        if(winner != null) 
            return;
 
        int player = getPlayer();
        
        if(!inv.get(player).hasGhost(gsize))
        	throw new RuntimeException("Impossible move: "+gsize);

        List<Move> cell = board[row][col];
        int size = inv.get(player).getMinGhostSize(player);
        boolean replace = false;
        int minsize = size;
        boolean place = true;

        if(lastghost != null) 
        {
            inv.get(player).addGhost(lastghost.size());
            if(lastghost.row() != row || lastghost.col() != col) 
            {
                List<Move> lastcell = getContent(lastghost.row(), lastghost.col());
                lastcell.remove(lastcell.size() - 1);
            }
        }

        if(!cell.isEmpty()) 
        {
            Move last = cell.get(cell.size() - 1);

            if(last.player() == player) 
            {
                if(lastghost == null || lastghost.row() != row || lastghost.col() != col) 
                {
                    place = false;
                } 
                else 
                {
                    replace = true;
                    if(cell.size() > 1)
                        minsize = cell.get(cell.size() - 2).size() + 1;
                }
            }
            size = inv.get(player).getMinGhostSize(last.size() + 1);

            if(replace && last.size() + 1 == 4) 
            {
                size = inv.get(player).getMinGhostSize(minsize);
            } 
            else if (size == 4) 
            {
                place = false;
            }
        }
        if(size == -1 || gsize<size) 
            place = false;
        
        if(replace) 
            cell.remove(cell.size() - 1);

        if(place) 
        {
        	int psize = gsize!=-1? gsize: size;
        	lastghost = new Move(row, col, psize, player);
            cell.add(lastghost);//new Ghost(size, player));
            inv.get(player).removeGhost(psize);
        } 
        else 
        {
            lastghost = null;
            if(gsize!=-1)
            	throw new RuntimeException("Impossible move: "+row+" "+col+" "+gsize);
        }

        checkGhostCount();

        notifyChange(null);
    }

    public List<Move>[] getCellsForCoordinates(List<int[]> coords) 
    {
        List<List<Move>> ret = new ArrayList<>();
        for(int[] coord : coords) 
        {
            ret.add(getContent(coord[0], coord[1]));
        }
        return ret.toArray(new List[ret.size()]);
    }

    public List<List<int[]>> getWinCombinations(int player) 
    {
        List<List<int[]>> ret = new ArrayList<>();
        List<Move>[] check;

        // Check rows
        for(int row = 0; row < board.length; row++) 
        {
            check = board[row];
            if(hasPotentiallyWon(check, player).hasPotentiallyWon()) 
            {
                List<int[]> wincombi = new ArrayList<>();
                for(int i = 0; i < board[row].length; i++) 
                {
                    wincombi.add(new int[]{row, i});
                }
                ret.add(wincombi);
            }
        }

        // Check columns
        for(int col = 0; col < board[0].length; col++) 
        {
            check = new List[board.length];
            for (int row = 0; row < board.length; row++) 
            {
                check[row] = board[row][col];
            }
            if(hasPotentiallyWon(check, player).hasPotentiallyWon()) 
            {
                List<int[]> wincombi = new ArrayList<>();
                for (int row = 0; row < board.length; row++) {
                    wincombi.add(new int[]{row, col});
                }
                ret.add(wincombi);
            }
        }

        // Check diagonals
        check = new List[board.length];
        for(int i = 0; i < board.length; i++) 
        {
            check[i] = board[i][i];
        }
        if(hasPotentiallyWon(check, player).hasPotentiallyWon()) 
        {
            List<int[]> wincombi = new ArrayList<>();
            for (int i = 0; i < board.length; i++) 
            {
                wincombi.add(new int[]{i, i});
            }
            ret.add(wincombi);
        }

        check = new List[board.length];
        for (int i = 0; i < board.length; i++) 
        {
            check[i] = board[i][board[0].length - i - 1];
        }

        if(hasPotentiallyWon(check, player).hasPotentiallyWon()) 
        {
            List<int[]> wincombi = new ArrayList<>();
            for(int i = 0; i < board.length; i++) 
            {
                wincombi.add(new int[]{i, board[0].length - i - 1});
            }
            ret.add(wincombi);
        }

        return ret;
    }

    public List<List<Move>> getCells(List<int[]> coords) 
    {
        List<List<Move>> ret = new ArrayList<>();
        for(int[] coord : coords) 
        {
            ret.add(getContent(coord[0], coord[1]));
        }
        return ret;
    }
     
    public WinStatus hasPotentiallyWon(List<Move>[] cells, int player) 
    {
        int minSize = 0;
        for(int i = 0; i < cells.length; i++) 
        {
            if(!cells[i].isEmpty()) 
            {
                Move g = cells[i].get(cells[i].size() - 1);
                if(g.player() != player) 
                {
                    minSize = 0;
                    break;
                } 
                else 
                {
                    if (minSize == 0 || g.size() < minSize)
                        minSize = g.size();
                }
            } 
            else 
            {
                minSize = 0;
                break;
            }
        }
        return new WinStatus(minSize > 0, minSize);
    }

    public boolean hasWon(List<Move>[] cells, int player) 
    {
        boolean ret = false;
        WinStatus status = hasPotentiallyWon(cells, player);

        if(status.minSize() == invsize) 
        {
            ret = true;
        } 
        else 
        {
            int has = 0;
            int op = (player == 0) ? 1 : 0;
            for(int i = 0; i < invsize; i++) 
            {
                if(inv.get(op).getContent(i) > 0) 
                    has = i + 1;
            }

            if(has <= status.minSize()) 
                ret = true;
        }

        return ret;
    }

    public void checkGhostCount() 
    {
        int sum = 0;
        for(Inventory i : inv) 
        {
            sum += i.getInventoryCount();
        }
        for(List<Move>[] row : board) 
        {
            for(List<Move> cell: row) 
            {
                sum += cell.size();
            }
        }
        if(sum != invcount)
            System.out.println("wrong ghost count: " + sum);
    }

    public void finishMove() 
    {
        if(winner != null)
        {
            System.out.println("todo: restart");
        	//restart();
            return;
        }

        if(lastghost == null)
            return;
        
        String message = null;

        int otherplayer = (getPlayer() == 0) ? 1 : 0;
        List<List<int[]>> combis = getWinCombinations(otherplayer);
        if(!combis.isEmpty()) 
        {
            wincombi = combis.get(0);
            winner = otherplayer;
            message = "Player " + winner + " won!";
        } 
        else 
        {
            combis = getWinCombinations(getPlayer());
            if(winner == null && !combis.isEmpty()) 
            {
                message = "Watch out, player has a combination";
            }
            for(List<int[]> combi : combis) 
            {
                if(hasWon(getCellsForCoordinates(combi), getPlayer())) 
                {
                    wincombi = combi;
                    winner = getPlayer();
                    message = "Player " + winner + " won!";
                }
            }

            lastghost = null;
            turn = (turn == 0) ? 1 : 0;
        }

        notifyChange(message);
    }
    
    public List<Move> getPossibleMoves()
    {
    	List<Move> ret = new ArrayList<Move>();
    	int p = getPlayer();
    	for(int size=1; size<=3; size++)
    	{
    		if(inv.get(p).hasGhost(size))
    		{
    			for(int row=0; row<rows; row++)
    			{
    				for(int col=0; col<cols; col++)
    				{
    					Move move = new Move(row, col, size, p);
    					if(isLegalMove(move))
    						ret.add(move);
    				}
    			}
    		}
    	}
    	return ret;
    }
    
    public boolean isLegalMove(Move move)
    {
    	boolean ret = true;
    	
    	// not his turn
    	if(getPlayer()!=move.player())
    		ret = false;
    	
    	// has no such ghost
    	if(!inv.get(getPlayer()).hasGhost(move.size))
    		ret = false;
    	
    	List<Move> cell = board[move.row()][move.col()];
    	if(cell.size()>0)
    	{
    		Move last = cell.get(cell.size() - 1);
    		
    		if(last.player() == getPlayer())
    			ret = false; // already his space
    		else if(move.size()<=last.size())
    			ret = false; // no bigger ghost
    	}

    	return ret;
    }
}

