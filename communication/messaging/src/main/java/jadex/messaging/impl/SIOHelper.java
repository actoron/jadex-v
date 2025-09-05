package jadex.messaging.impl;

import jadex.common.SBinConv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *  Helper class for IO operations.
 */
public class SIOHelper
{
    /**
     *  Reads a size-prefixed chunk from stream.
     *
     *  @param is The input stream.
     *  @param sizebuf Buffer for storing the size value.
     *  @return The chunk.
     *  @throws IOException Thrown on IO error.
     */
    public static byte[] readChunk(InputStream is, byte[] sizebuf) throws IOException
    {
        int readbytes = 0;
        while (readbytes < sizebuf.length)
        {
            readbytes += is.read(sizebuf, readbytes, sizebuf.length - readbytes);
        }

        readbytes = 0;
        byte[] chunk = new byte[SBinConv.bytesToInt(sizebuf)];
        while (readbytes < chunk.length)
            readbytes += is.read(chunk, readbytes, chunk.length - readbytes);

        return chunk;
    }

    /**
     *  Writes a size-prefixed chunk to output stream.
     *
     *  @param os The output stream.
     *  @param chunk The chunk.
     *  @throws IOException Thrown on write error.
     */
    public static void writeChunk(OutputStream os, byte[] chunk) throws IOException
    {
        os.write(SBinConv.intToBytes(chunk.length));
        os.write(chunk);
        os.flush();
    }
}
