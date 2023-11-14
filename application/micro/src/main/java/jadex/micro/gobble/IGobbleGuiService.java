package jadex.micro.gobble;

import jadex.future.IFuture;
import jadex.micro.gobble.Board.Move;
import jadex.providedservice.annotation.Service;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;

@Service
public interface IGobbleGuiService 
{
	/**
	 *  Start a new game.
	 *  @param row The row.
	 *  @param col The col.
	 *  @param invsize The inventory size.
	 */
	@POST
	public void informNewGame(int rows, int cols, int invsize);
	
	/**
	 *  Inform about a move.
	 *  @param move The move.
	 */
	@POST
	public void informMove(Move move);

	/**
	 *  Make a move.
	 *  @return The move.
	 */
	@GET
	public IFuture<Move> makeMove();
	
	/**
	 *  Make a move.
	 *  @return The move.
	 * /
	public IFuture<Move> makeMove(Move lastmove);*/
	
	/**
	 *  Subscribe to game.
	 *  @return events.
	 * /
	public ISubscriptionIntermediateFuture<GameEvent> subscribeToGame();*/
}
