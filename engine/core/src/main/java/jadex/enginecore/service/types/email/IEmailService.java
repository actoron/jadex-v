package jadex.enginecore.service.types.email;

import jadex.common.IFilter;
import jadex.enginecore.service.annotation.CheckNotNull;
import jadex.enginecore.service.annotation.Service;
import jadex.future.IFuture;
import jadex.future.ISubscriptionIntermediateFuture;

/**
 *  The email service allows for sending and receiving emails.
 */
@Service(system=true)
public interface IEmailService
{
	/**
	 *  Send an email.
	 *  @param email The email.
	 *  @param account The email account.
	 */
	public IFuture<Void> sendEmail(@CheckNotNull Email email, EmailAccount account);

	/**
	 *  Subscribe for email.
	 *  @param filter The filter.
	 *  @param account The email account.
	 */
//	@Timeout(Timeout.NONE) // replaced by internal timeout avoidance mechanism via future
	public ISubscriptionIntermediateFuture<Email> subscribeForEmail(IFilter<Email> filter, EmailAccount account);
	
	/**
	 *  Subscribe for email.
	 *  @param filter The filter.
	 *  @param account The email account.
	 */
	public ISubscriptionIntermediateFuture<Email> subscribeForEmail(IFilter<Email> filter, EmailAccount account, boolean fullconv);
	
}
