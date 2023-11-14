package jadex.micro.gobble;

import java.awt.Desktop;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadex.core.IComponent;
import jadex.future.Future;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;
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
		
		openInBrowser("http://localhost:8081/"+agent.getId().getLocalName());
		//openInBrowser("http://localhost:8081/"+agent.getId()+"/index2.html");
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
		return new Future<Move>(rmoves.get(0));
	}
	
	/**
	 *  Rank the moves.
	 */
	public List<Move> rankMoves(List<Move> moves)
	{
		return moves;
	}
	
	/**
	 *  Subscribe to game event.
	 *  @return Game events.
	 * /
	public ISubscriptionIntermediateFuture<GameEvent> subscribeToGame()
	{
		SubscriptionIntermediateFuture<GameEvent> ret = new SubscriptionIntermediateFuture<GameEvent>();
		ret.setTerminationCommand((ex) -> subscribers.remove(ret));
		subscribers.add(ret);
		return ret;
	}*/
	
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
