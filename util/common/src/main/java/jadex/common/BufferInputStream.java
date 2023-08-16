package jadex.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 *  Class for wrapping an InputStream around a ByteBuffer.
 *  TODO: Optimize.
 *
 */
public class BufferInputStream extends InputStream
{
	/** The buffer. */
	protected ByteBuffer buffer;
	
	/**
	 *  Creates a new InputStream from buffer.
	 *  @param buffer The buffer.
	 */
	public BufferInputStream(ByteBuffer buffer)
	{
		this.buffer = buffer;
	}
	
	public int read() throws IOException
	{
		try
		{
			return buffer.get();
		}
		catch (BufferOverflowException e)
		{
			throw new IOException("End of buffer.");
		}
	}
	
	/**
	 *  Reads an array from the buffer.
	 *  
	 *  @param b The array.
	 *  @return Number of bytes read.
	 */
	public int read(byte[] b) throws IOException
	{
		return read(b, 0, b.length);
	}
	
	/**
	 *  Reads an array from the buffer.
	 *  
	 *  @param b The array.
	 *  @param off The offset in the array.
	 *  @param len Number of bytes to read.
	 *  @return Number of bytes read.
	 */
	public int read(byte[] b, int off, int len) throws IOException
	{
		int rem = buffer.remaining();
		if (rem == 0)
			return -1;
		
		int maxread = Math.min(len, rem);
		
		buffer.get(b, off, maxread);
		
		return maxread;
	}
	
	/**
	 *  Wraps a byte buffer.
	 * 
	 *  @param buffer The buffer.
	 *  @return InputStream based on the buffer.
	 */
	public static final BufferInputStream wrap(ByteBuffer buffer)
	{
		return new BufferInputStream(buffer);
	}
}
