package jadex.micro.gobble;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.SubscriptionIntermediateFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.gobble.Board.Move;
import jadex.model.annotation.OnStart;
import jadex.providedservice.annotation.Service;
import jadex.publishservice.IPublishServiceFeature;
import jadex.publishservice.publish.annotation.Publish;

@Agent
@Service
@Publish(publishid="http://localhost:8081/${cid}/gobbleapi", publishtarget = IGobbleGuiService.class)
public class GobbleAgent implements IGobbleGuiService
{
	@Agent
	protected IComponent agent;
	
	protected Board board;
	
	protected Set<SubscriptionIntermediateFuture<GameEvent>> subscribers = new HashSet<SubscriptionIntermediateFuture<GameEvent>>();
	
	@OnStart
	protected void onStart()
	{
		System.out.println("agent started: "+agent.getId().getLocalName());
				
		IPublishServiceFeature ps = agent.getFeature(IPublishServiceFeature.class);
		ps.publishResources("http://localhost:8081/${cid}", "jadex/micro/gobble");
		
		System.out.println("open in browser");
		openInBrowser("http://localhost:8081/"+agent.getId().getLocalName());
	}
	
	/**
	 *  Start a new game.
	 *  @param row The row.
	 *  @param col The col.
	 *  @param invsize The inventory size.
	 */
	public void informNewGame(int rows, int cols, int invsize)
	{
		this.board = new Board(rows, cols, invsize);
	}
	
	/**
	 *  Inform about a move.
	 *  @param move The move.
	 */
	public void informMove(Move move)
	{
		if(board==null)
			throw new RuntimeException("No game running");
		board.makeMove(move);
	}
	
	/**
	 *  Make a move.
	 *  @return The move.
	 */
	public IFuture<Move> makeMove()
	{
		List<Move> moves = board.getPossibleMoves();
		List<Move> rmoves = rankMoves(moves);
		Move move = rmoves.get(0);
		
		board.makeMove(move);
		
		return new Future<Move>(move);
	}
	
	/**
	 *  Rank the moves.
	 */
	public List<Move> rankMoves(List<Move> moves)
	{
		int player = moves.get(0).player();
		int oplayer = player==1? 0: 1;
		
		// avoid winning of opponent
		List<List<int[]>> combis = board.getWinCombinations(oplayer, board::hasPotentiallyWon);
		if(combis.size()>0)
		{
			int mysize = board.getInventory(player).getMaxGhostSize();
			List<int[]> combi = combis.get(0);
			List<List<Move>> cells = board.getCells(combi);

			int found = -1;
			for(int i=0; i<cells.size(); i++)
			{
				List<Move> cell = cells.get(i);
				Move m = cell.get(cell.size()-1);
				if(mysize>m.size())
				{
					found = i;
					break;
				}
			}
			
			if(found!=-1)
			{
				int[] coord = combi.get(found);
				Move move = new Move(coord[0], coord[1], mysize, player);
				System.out.println("found prohibit move: "+move);
				
				moves.remove(move);
				moves.add(0, move);
			}
		}
		else // try connect three
		{
			combis = board.getWinCombinations(player, board::hasCompletionMove);
			if(combis.size()>0)
			{
				Move move = null;
				int mysize = board.getInventory(player).getMaxGhostSize();
				List<int[]> combi = combis.get(0);
				List<List<Move>> cells = board.getCells(combi);
				
				for(int i=0; i<cells.size(); i++)
				{
					List<Move> cell = cells.get(i);
					if(cell.size()==0)
					{
						int[] coord = combi.get(i);
						move = new Move(coord[0], coord[1], mysize, player);
						break;
					}
					else
					{
						Move m = cell.get(cell.size()-1);
						if(m.player()!=player && mysize>m.size())
						{
							int[] coord = combi.get(i);
							move = new Move(coord[0], coord[1], mysize, player);
							break;
						}
					}
				}
				
				if(move!=null)
				{
					System.out.println("found connect move: "+move);
					moves.remove(move);
					moves.add(0, move);
				}
			}
		}
		
		
		
		return moves;
	}
	
	/**
	 *  Open the url in the browser.
	 *  @param url The url.
	 */
	protected void openInBrowser(String url)
	{
		try 
		{
			URI uri = new URI(url);
			Desktop.getDesktop().browse(uri);
		}	
		catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 *  Start the example.
	 */
	public static void main(String[] args) throws InterruptedException 
	{
		IComponent.create(new GobbleAgent());
		
		IComponent.waitForLastComponentTerminated();
	}
}
