package jadex.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

public class BufferOutputStream extends OutputStream
{
	protected byte[] buffer;
	
	protected int bufsize;
	
	/**
     *  Creates a new BufferOutputStream with a default initial size.
     */
    public BufferOutputStream()
    {
        this(32);
    }
	
	/**
     *  Creates a new BufferOutputStream with the specified initial size.
     *
     *  @param capacity The initial capacity, grows as necessary.
     */
    public BufferOutputStream(int capacity)
    {
    	buffer = new byte[capacity];
    }
    
    /**
     *  Writes a byte to the stream.
     *
     *  @param b The byte to write.
     */
    public void write(int b) {
    	checkAndExpandCapacity(bufsize + 1);
    	buffer[bufsize] = (byte) b;
    	bufsize += 1;
    }

    /**
     *  Writes the specified bytes from the byte array, starting at the provided offset.
     *
     *  @param arr The array.
     *  @param off The array offset..
     *  @param len Number of bytes to write.
     */
    public void write(byte arr[], int off, int len)
    {
    	checkAndExpandCapacity(bufsize + len);
    	System.arraycopy(arr, off, buffer, bufsize, len);
    	bufsize += len;
    }

    /**
     *  Writes a byte array to the stream.
     *
     *  @param arr the data.
     */
    public void writeBytes(byte arr[])
    {
        write(arr, 0, arr.length);
    }

    /**
     *  Writes the stream content to an output stream..
     *
     *  @param os The output stream target.
     *  @throws IOException Thrown on IO issues.
     */
    public void writeTo(OutputStream os) throws IOException
    {
        os.write(buffer, 0, bufsize);
    }

    /**
     *  Resets the buffer, all data lost.
     */
    public void reset()
    {
        bufsize = 0;
    }

    /**
     *  Converts the buffer to an appropriately sizes array.
     *
     *  @return Stream content as an array.
     */
    public byte[] toByteArray()
    {
        return Arrays.copyOf(buffer, bufsize);
    }
    
    /**
     *  Converts the buffer to a ByteBuffer.
     *
     *  @return Stream content as a ByteBuffer.
     */
    public ByteBuffer toBuffer()
    {
        return ByteBuffer.wrap(Arrays.copyOf(buffer, bufsize));
    }
    
    /**
     *  Converts the buffer to a direct ByteBuffer.
     *
     *  @return Stream content as a direct ByteBuffer.
     */
    public ByteBuffer toDirectBuffer()
    {
    	ByteBuffer ret = ByteBuffer.allocateDirect(bufsize);
    	ret.put(buffer, 0, bufsize);
    	ret.rewind();
        return ret;
    }

    /**
     *  Returns the buffer size.
     *
     *  @return Buffer size.
     */
    public int size()
    {
        return bufsize;
    }

    /**
     *  Since this is backed by a buffer,
     *  this does nothing.
     */
    public void close() throws IOException
    {
    }
    
    /**
     *  Checks if there is enough capacity for an operation and expands
     *  as necessary.
     *
     *  @param reqcap The required capacity.
     */
    protected void checkAndExpandCapacity(int reqcap)
    {
        int shortage = reqcap - buffer.length;
        if (shortage > 0)
            buffer = Arrays.copyOf(buffer, Math.max((shortage + buffer.length), buffer.length << 1));
    }
}
