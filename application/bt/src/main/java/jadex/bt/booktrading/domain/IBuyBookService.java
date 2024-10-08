package jadex.bt.booktrading.domain;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 *  The buy book service is provided by the seller and used by the buyer.
 */
@Service
public interface IBuyBookService
{
	/**
	 *  Ask the seller for a a quote on a book.
	 *  @param title	The book title.
	 *  @return The price.
	 */
	public IFuture<Integer>	callForProposal(String title);

	/**
	 *  Buy a book
	 *  @param title	The book title.
	 *  @param price	The price to pay.
	 *  @return A future indicating if the transaction was successful.
	 */
	public IFuture<Void> acceptProposal(String title, int price);
	
//	/**
//	 *  Refuse to buy a book
//	 *  @param title	The book title.
//	 *  @param price	The requested price.
//	 */
//	public void	rejectProposal(String title, int price);
}
