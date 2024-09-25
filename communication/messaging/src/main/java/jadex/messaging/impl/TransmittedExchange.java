package jadex.messaging.impl;

import jadex.core.ComponentIdentifier;

/**
 *  One message that is either a request or response in a request/response exchange.
 */
public record TransmittedExchange(ComponentIdentifier sender, String conversationid, Object message)
{
}
