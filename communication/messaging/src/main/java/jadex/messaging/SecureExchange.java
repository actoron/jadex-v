package jadex.messaging;

import jadex.core.ComponentIdentifier;

/**
 *  One message that is either a request or response in a request/response exchange with security metainfos.
 */
public record SecureExchange(ComponentIdentifier sender, ISecurityInfo secinfo, String conversationid, Object message)
{
}
