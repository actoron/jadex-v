package jadex.serialization.codecs;

import jadex.serialization.ICodec;

public abstract class AbstractCodec implements ICodec
{
	/**
	 *  Decode data with the codec.
	 *  @param bytes The value bytes as byte array or input stream.
	 *  @return The decoded object or byte array (for intermediate codecs).
	 */
	public byte[] decode(byte[] bytes)
	{
		return decode(bytes, 0, bytes.length);
	}
}
