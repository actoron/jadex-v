package jadex.collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  A file-based map that can be shared across the operating system
 *  using file locks.
 *  
 *  KeyNode Layout:
 *  [Prev. Node offset 8 byte]
 *  [Next Node offset 8 byte]
 *  [Value Node offset 8 byte]
 *  [Key size 8 byte]
 *  [Key data variable]
 *  
 *  ValueNode Layout:
 *  [Value size 8 byte]
 *  [Value data variable]
 */
public class SharedPersistentMap<K, V>
{
	/** 
	 *  Magic word starting the file.
	 */
	protected static final short MAGIC_WORD = (short) 0xbd15;
	
	/** Type of map - normal shared map. */
	protected static final byte MAP_TYPE = 0;
	
	/**
	 *  Size of the header:
	 *  
	 *  [2 bytes Magic Word]
	 *  [1 byte Map Type = 00]
	 *  [1 byte Index Exponent]
	 *  [8 bytes Map Size/Elements]
	 *  [116 bytes reserved]
	 *  
	 */
	protected static final int HEADER_SIZE = 128;
	
	/** Initial index exponent, nearest prime < 2^8 = 251. */
	protected static final int INITIAL_INDEX_EXP = 8;
	
	/** The file backing the map */
	protected File file;
	
	/** Current file access mode. */
	protected String mode = "rw";
	
	/** The memory-mapped file when opened. */
	protected RandomAccessFile rafile;
	
	/** The serialization/encoder functionality. */
	protected Function<Object, byte[]> encoder;
	
	/** The deserialization/decoder functionality. */
	protected Function<byte[], Object> decoder;
	
	/**
	 *  Creates a new map, configure with builder pattern.
	 */
	public SharedPersistentMap()
	{
	}
	
	public static void main(String[] args)
	{
		int m = 0;
		int f = 0;
		int exp = 8;
		int tests1 = 0;
		int tests2 = 0;
		long t1;
		long t2;
		for (int i = 0; i < 24; ++i)
		{
			long b = (1L << (exp + i));
			t1 = b - 1;
			while (true)
			{
				tests1++;
				if (BigInteger.valueOf(t1).isProbablePrime(100))
					break;
				t1 -= 2;
			}
			
			t2 = b + 1;
			while (true)
			{
				tests2++;
				if (BigInteger.valueOf(t2).isProbablePrime(100))
					break;
				t2 += 2;
			}
			
			System.out.print("[");
			//if (Math.abs(b-t1) < Math.abs(b-t2))
			{
				++m;
				System.out.print("M: " + t1 + ",");
			}
			System.out.print("b " + b + ",");
			//else
			{
				++f;
				System.out.print("F: " + t2);// + ", ");
			}
			System.out.print("]");
		}
		System.out.println();
		System.out.println("m " + m + " f " + f);
		
		System.out.println(tests1);
		System.out.println(tests2);
		System.out.println("AAAA " + (Integer.MAX_VALUE) + " AAAA");
		
//		System.out.println(t);
	}
	
	// Configuration
	
	/**
	 *  Sets the file backing the map.
	 *  
	 *  @param path P
	 *  @return 
	 */
	public SharedPersistentMap<K, V> setFile(String path)
	{
		return setFile(new File(path));
	}
	
	public SharedPersistentMap<K, V> setFile(File file)
	{
		this.file = file;
		return this;
	}
	
	/**
	 *  Configures the map for synchronized/non-synchronized writes.
	 *  If true, writes to the map are immediately written to
	 *  persistent storage
	 * @param sync
	 * @return
	 */
	public SharedPersistentMap<K, V> setSynchronized(boolean sync)
	{
		mode = sync ? "rwd" : "rw";
		return this;
	}
	
	/**
	 *  Sets the encoder for encoding/serializing objects.
	 *  
	 *  @param encoder Encoder for encoding/serializing objects.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> setEncoder(Function<Object, byte[]> encoder)
	{
		this.encoder = encoder;
		return this;
	}
	
	/**
	 *  Sets the encoder for decoding/deserializing objects.
	 *  
	 *  @param decoder Encoder for decoding/deserializing objects.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> setDecoder(Function<byte[], Object> decoder)
	{
		this.decoder = decoder;
		return this;
	}
	
	/**
	 *  Opens the backing file, after this operation the map is ready for use.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> open()
	{
		try
		{
			file.createNewFile();
			this.rafile = new RandomAccessFile(file, mode);
		}
		catch (FileNotFoundException e)
		{
			throw new RuntimeException(e);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		return this;
	}
	
	/**
	 *  Closes the backing file, after this operation the map must be opened again before use.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> close()
	{
		try
		{
			rafile.close();
			rafile = null;
		}
		catch (IOException e)
		{
		}
		return this;
	}
	
	// Map API
	
	
	public V get(K key)
	{
		V ret = null;
		byte[] enckey = encoder.apply(key);
		long hash = key.hashCode() & 0xFFFFFFFFL;
		try (FileLock l = readLock())
		{
			
			long isize = readIndexSize();
			long modhash = hash % isize;
			rafile.seek(HEADER_SIZE + modhash);
			long next = rafile.readLong();
			K currentkey = null;
			long valpos = 0;
			while (valpos == 0 && next != 0)
			{
				long curr = next;
				rafile.seek(curr + 8);
				
				next = rafile.readLong();
				long readvalpos = rafile.readLong();
				
				@SuppressWarnings("unchecked")
				K readkey = (K) readObject();
				currentkey = readkey;
				
				if (key.equals(currentkey))
					valpos = readvalpos;
				else
					currentkey = null;
			}
			
			if (valpos != 0)
			{
				@SuppressWarnings("unchecked")
				V val = (V) readObject(valpos);
				ret = val;
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return ret;
	}
	
	/*public V put(K key, V value)
	{
		
	}*/
	
	/**
	 *  Performs a read transaction on the map.
	 *  Engages read lock and automatically releases on success or
	 *  exception.
	 *  
	 *  @param transaction Transaction to perform.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> readTransaction(Consumer<SharedPersistentMap<K, V>> transaction)
	{
		try(FileLock l = readLock())
		{
			transaction.accept(this);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return this;
	}
	
	/**
	 *  Performs a write transaction on the map.
	 *  Engages write (exclusive( lock and automatically releases on
	 *  success or exception.
	 *  
	 *  @param transaction Transaction to perform.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> writeTransaction(Consumer<SharedPersistentMap<K, V>> transaction)
	{
		try(FileLock l = writeLock())
		{
			transaction.accept(this);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		return this;
	}
	
	/**
	 *  Engages the read lock over the whole file.
	 *  @return The lock, close to release.
	 *  
	 *  @throws IOException Thrown on IO issues.
	 */
	public FileLock readLock() throws IOException
	{
		return rafile.getChannel().lock(0, Long.MAX_VALUE, true);
	}
	
	/**
	 *  Engages the write lock over the whole file.
	 *  @return The lock, close to release.
	 *  
	 *  @throws IOException Thrown on IO issues.
	 */
	public FileLock writeLock() throws IOException
	{
		return rafile.getChannel().lock(0, Long.MAX_VALUE, false);
	}
	
	// Internal methods
	
	/**
	 *  Reads the current index size.
	 *  NOTE: Called MUST lock the file with, at minimum, a read lock.
	 *  
	 *  @return Index size or -1, if the file is invalid or empty.
	 */
	protected long readIndexSize()
	{
		long ret = -1;
		try
		{
			rafile.seek(0);
			int fword = rafile.readInt();
			int maptype = fword & 0x0000FF00;
			int exp = fword & 0x000000FF;
			if ((fword & 0xFFFF0000) == MAGIC_WORD && exp < 63 && maptype == MAP_TYPE)
			{
				ret = getIndexSizeFromExp(exp);
			}
		}
		catch (IOException e)
		{
		}
		return ret;
	}
	
	/**
	 *  Reads object using the decoder at position offset.
	 *  Assument layout:
	 *  [size 4 bytes]
	 *  [Object data variable]
	 *  
	 *  @param offset Offset where the object is stored.
	 *  @return The object.
	 */
	protected Object readObject(long offset) throws IOException
	{
		rafile.seek(offset);
		return readObject();
	}
	
	/**
	 *  Reads object using the decoder at current file position.
	 *  Assument layout:
	 *  [size 4 bytes]
	 *  [Object data variable]
	 *  
	 *  @return The object.
	 */
	protected Object readObject() throws IOException
	{
		Object ret = null;
		long longsize = rafile.readLong();
		if (longsize <= Integer.MAX_VALUE)
		{
			int size = (int) longsize;
			byte[] objbytes = new byte[size];
			rafile.readFully(objbytes);
			ret = decoder.apply(objbytes);
		}
		else
		{
			// TODO: Implement Huge Object Support, also possibly user memory mapping for large objects in general
			throw new UnsupportedOperationException("Huge Objects not supported by this version of SharedPersistentMap");
		}
		return ret;
	}
	
	/**
	 *  Determines the index size based on the exponent by
	 *  calculating the Mersenne prime or if it does not exist
	 *  the nearest prime smaller than 2^exp.
	 *  This also happens to line up nearly with Integer.MAX_VALUE,
	 *  which is a Mersenne prime. The map supports more object than
	 *  Integer.MAX_VALUE, however, functionality degrades (size() can
	 *  only return Integer_MAX_VALUE)
	 *  
	 *  @param exp The exponent.
	 *  @return Size of the index.
	 */
	protected long getIndexSizeFromExp(int exp)
	{
		long size = (1L << exp) - 1;
		while (true)
		{
			if (BigInteger.valueOf(size).isProbablePrime(100))
				break;
			size -= 2;
		}
		return (int) size;
	}
}
