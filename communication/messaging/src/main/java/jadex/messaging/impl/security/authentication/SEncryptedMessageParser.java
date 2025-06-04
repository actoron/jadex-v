package jadex.messaging.impl.security.authentication;

import jadex.common.SBinConv;

/**
 *  Parser class that allows convenient extraction of fields
 *  of an encrypted message.
 */
public class SEncryptedMessageParser
{
    /** Offset for the message ID */
    private int MESSAGE_ID_OFFSET = 0;

    /**
     *  Extracts the message ID from the message.
     *  @param message The message.
     *  @return The message iD.
     */
    public long getMessageId(byte[] message)
    {
        return SBinConv.bytesToLong(message, MESSAGE_ID_OFFSET);
    }

    /**
     *  Writes the message ID.
     *  @param message The message
     *  @param messageid The message ID.
     */
    public void setMessageId(byte[] message, long messageid)
    {
        SBinConv.longIntoBytes(messageid, message, MESSAGE_ID_OFFSET);
    }
}
