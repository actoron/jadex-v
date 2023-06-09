package jadex.collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.channels.FileLock;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *  A file-based map that can be shared across the operating system
 *  using file locks.
 *  
 *  KVNode Layout:
 *  [Prev. Node offset 8 byte]
 *  [Next Node offset 8 byte]
 *  [Key Object Node offset 8 byte]
 *  [Value Object Node offset 8 byte]
 *  
 *  Object Node Layout:
 *  [Object size 8 byte]
 *  [Object data variable]
 */
public class SharedPersistentMap<K, V>
{
	/** Magic word starting the file. */
	protected static final short MAGIC_WORD = (short) 0xbd15;
	
	/** Type of map - normal shared map. */
	protected static final byte MAP_TYPE = 0;
	
	/**
	 *  Size of the header:
	 *  
	 *  [2 bytes Magic Word]
	 *  [1 byte Map Type = 00]
	 *  [1 byte Index Exponent]
	 *  [4 bytes reserved]
	 *  [8 bytes Map Size/Elements]
	 *  [8 bytes garbage size]
	 *  [104 bytes reserved]
	 *  
	 */
	protected static final int HEADER_SIZE = 128;
	
	/** Position of the map size in the header. */
	protected static final long HDR_POS_MAP_SIZE = 8;
	
	/** Position of the garbage size in the header. */
	protected static final long HDR_POS_GARBAGE_SIZE = 16; 
	
	/** Initial index exponent, nearest prime < 2^8 = 251. */
	protected static final int INITIAL_INDEX_EXP = 8;
	
	/** Maximum index exponent, limit: hashCode() returns int. */
	protected static final int MAX_INDEX_EXP = 32;
	
	/** Minimum file size (header size + initial index size). */
	protected static final long MIN_FILE_SIZE = HEADER_SIZE + getPaddedIndexSize(INITIAL_INDEX_EXP);
	
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
	
	// ****************************** Configuration methods ******************************
	
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
	
	// ****************************** java.util.Map API methods ******************************
	
	/**
	 *  Gets the value for a given key.
	 *  
	 *  @param key The key to be found.
	 *  @return The associated value or null if not found.
	 */
	public V get(K key)
	{
		V ret = readTransaction((m) ->
		{
			V lret = null;
			KVNode keynode = findKVPair(key);
			if (keynode != null)
				lret = keynode.getValue();
			return lret;
		});
		return ret;
	}
	
	/**
	 *  Checks if a key is contained in the map.
	 *  
	 *  @param key The key.
	 *  @return True, if the key is contained, even if the associated value is null,
	 *  		false otherwise.
	 */
	public boolean containsKey(K key)
	{
		boolean ret = readTransaction((m) ->
		{
			boolean lret = false;
			KVNode keynode = findKVPair(key);
			if (keynode != null)
				lret = true;
			return lret;
		});
		return ret;
	}
	
	/**
	 *  Adds a new key-value pair to the map, if the key already exists,
	 *  the associated value is overwritten.
	 *  
	 *  @param key The key.
	 *  @param value The value.
	 *  @return The old value, if it exists, null otherwise.
	 */
	public V put(K key, V value)
	{
		V ret = writeTransaction((m) ->
		{
			V lret = null;
			
			KVNode keynode = findKVPair(key);
			if (keynode == null)
				keynode = appendNewKVPair(key);
			lret = keynode.setValue(value);
			
			return lret;
		});
		return ret;
	}
	
	/**
	 *  Clears the map, deleting all elements.
	 */
	public void clear() 
	{
		writeTransaction((m) -> 
		{
			initializeMap();
			return null;
		});
	}
	
	// ****************************** Lock methods ******************************
	
	/**
	 *  Performs a read transaction on the map.
	 *  Engages read lock and automatically releases on success or
	 *  exception.
	 *  
	 *  @param transaction Transaction to perform.
	 *  @return Return value of lambda expression.
	 */
	public <R> R readTransaction(IOFunction<SharedPersistentMap<K, V>, R> transaction)
	{
		try(FileLock l = readLock())
		{
			return transaction.apply(this);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Performs a write transaction on the map.
	 *  Engages write lock and automatically releases on success or
	 *  exception.
	 *  
	 *  @param transaction Transaction to perform.
	 *  @return Return value of lambda expression.
	 */
	public <R> R writeTransaction(IOFunction<SharedPersistentMap<K, V>, R> transaction)
	{
		try(FileLock l = writeLock())
		{
			return transaction.apply(this);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Engages the read lock over the whole file.
	 *  
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
	 *  
	 *  @return The lock, close to release.
	 *  
	 *  @throws IOException Thrown on IO issues.
	 */
	public FileLock writeLock() throws IOException
	{
		return rafile.getChannel().lock(0, Long.MAX_VALUE, false);
	}
	
	// ****************************** Internal methods ******************************
	
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
	 * Initializes the map file as an empty map.
	 * 
	 * @throws IOException Thrown on IO issues. 
	 */
	protected void initializeMap() throws IOException
	{
		rafile.setLength(MIN_FILE_SIZE);
		rafile.write(new byte[(int) MIN_FILE_SIZE]);
		
		rafile.seek(0);
		rafile.writeInt((MAGIC_WORD << 16) | INITIAL_INDEX_EXP);
	}
	
	/**
	 *  Finds the key-value pair that contains the given key.
	 * 
	 *  @param key The key to look up.
	 *  @return The KVNode containing the key or null if not found.
	 *  @throws IOException Thrown on IO issues.
	 */
	protected KVNode findKVPair(K key) throws IOException
	{
		long isize = readIndexSize();
		long modhash = key.hashCode() % isize;
		rafile.seek(HEADER_SIZE + modhash);
		long nodepos = rafile.readLong();
		if (nodepos != 0)
		{
			KVNode kvnode = new KVNode(nodepos);
			do
			{
				K currentkey = kvnode.getKey();
				if (Objects.equals(key, currentkey))
					return kvnode;
				
				kvnode = kvnode.next();
			}
			while (kvnode != null);
		}
		
		return null;
	}
	
	/**
	 *  Appends a new KV node.
	 *  
	 *  Note: This only adds the node, not the key.
	 *  There is also no check if the key is already in the map.
	 *  
	 *  @param key The key that needs the KVNode.
	 *  @return KVNode object for the newly created node.
	 *  @throws IOException Thrown on IO issues.
	 */
	protected KVNode appendNewKVPair(K key) throws IOException
	{
		// 8 byte alignment
		long start = getNextAlignedPosition();
		int padding = (int) (start - rafile.length());
		
		rafile.setLength(rafile.length() + padding + KVNode.NODE_SIZE);
		rafile.seek(start);
		rafile.write(new byte[KVNode.NODE_SIZE]);
		
		long isize = readIndexSize();
		long modhash = key.hashCode() % isize;
		long refpos = HEADER_SIZE + modhash;
		rafile.seek(refpos);
		long nodepos = rafile.readLong();
		if (nodepos != 0)
		{
			KVNode prevnode = new KVNode(nodepos);
			while(prevnode.getNext() != 0)
				prevnode = prevnode.next();
			refpos = prevnode.getNextReferencePosition();
		}
		
		rafile.seek(refpos);
		rafile.writeLong(start);
		
		KVNode ret = new KVNode(start);
		return ret;
	}
	
	// ****************************** Static methods ******************************
	
	/**
	 *  Determines the index size based on the exponent by
	 *  calculating the Mersenne prime or if it does not exist
	 *  the nearest prime smaller than 2^exp.
	 *  This also happens to line up neatly with Integer.MAX_VALUE,
	 *  which is a Mersenne prime. The map supports more object than
	 *  Integer.MAX_VALUE, however, functionality degrades (size() can
	 *  only return Integer_MAX_VALUE)
	 *  
	 *  @param exp The exponent.
	 *  @return Size of the index.
	 */
	private static final long getIndexSizeFromExp(int exp)
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
	
	/**
	 *  Returns the next 8byte-aligned starting position after the current end of file.
	 *  
	 *  @return The next aligned starting position after the current end of file.
	 */
	private long getNextAlignedPosition() throws IOException
	{
		return (rafile.length() + 7) & ~7L;
	}
	
	/**
	 *  Returns the index size with padding.
	 *  HEADER_SIZE + padded index size = start of data
	 *  
	 *  @param exp The exponent.
	 *  @return Size of the padded index.
	 */
	private static long getPaddedIndexSize(int exp)
	{
		return 1L << exp;
	}
	
	// ****************************** Helper classes and types ******************************
	
	/**
	 *  View on a KVNode.
	 *  KVNode Layout:
	 *  [Prev. Node offset 8 byte]
	 *  [Next Node offset 8 byte]
	 *  [Key Object Node offset 8 byte]
	 *  [Value Object Node offset 8 byte]
	 *
	 */
	protected class KVNode
	{
		/** Size of a KVNode */
		private static final int NODE_SIZE = 32;
		
		/** Offset for previous location. */
		private static final long PREV_OFFSET = 0;
		
		/** Offset for previous location. */
		private static final long NEXT_OFFSET = 8;
		
		/** Offset for key object location. */
		private static final long KEY_OFFSET = 16;
		
		/** Offset for value object location. */
		private static final long VAL_OFFSET = 24;
		
		/** Position of the KV node within the file. */
		private long pos;
		
		/**
		 *  Creates a view on a KV node.
		 *  @param position Position of the KV node.
		 */
		public KVNode(long position)
		{
			this.pos = position;
		}
		
		/**
		 *  Selects the previous node.
		 *  Note: This operation changes the node object,
		 *  current object returned for convenience.
		 *  
		 *  @return This node.
		 *  @throws IOException Thrown on IO issues.
		 */
		public KVNode previous() throws IOException
		{
			goToPos(PREV_OFFSET);
			pos = rafile.readLong();
			return this;
		}
		/**
		 *  Checks if a previous node exists.
		 * 
		 *  @return True, if there is a previous node.
		 *  @throws IOException Thrown on IO issues.
		 */
		public boolean hasPrevious() throws IOException
		{
			goToPos(PREV_OFFSET);
			return rafile.readLong() != 0;
		}
		
		/**
		 *  Selects the next node.
		 *  Note: This operation changes the node object,
		 *  current object / null returned for convenience.
		 *  
		 *  @return This node or null if there is no more node.
		 *  @throws IOException Thrown on IO issues.
		 */
		public KVNode next() throws IOException
		{
			goToPos(NEXT_OFFSET);
			pos = rafile.readLong();
			if (pos == 0)
				return null;
			return this;
		}
		
		/**
		 *  Checks if a next node exists.
		 * 
		 *  @return True, if there is a next node.
		 *  @throws IOException Thrown on IO issues.
		 */
		public boolean hasNext() throws IOException
		{
			goToPos(NEXT_OFFSET);
			return rafile.readLong() != 0;
		}
		
		/**
		 *  Gets the key object.
		 *  
		 *  @return Key object.
		 *  @throws IOException Thrown on IO issues.
		 */
		public K getKey() throws IOException
		{
			goToPos(KEY_OFFSET);
			rafile.seek(rafile.readLong());
			@SuppressWarnings("unchecked")
			K ret = (K) readObject();
			return ret;
		}
		
		/**
		 *  Sets the key object.
		 *  TODO: needed?
		 *  @param key Key object.
		 */
		/*public void setKey(K key)
		{
			writeObject(KEY_OFFSET, key);
		}*/
		
		/**
		 *  Gets the value object.
		 *  
		 *  @return Value object.
		 *  @throws IOException Thrown on IO issues.
		 */
		public V getValue() throws IOException
		{
			goToPos(VAL_OFFSET);
			rafile.seek(rafile.readLong());
			@SuppressWarnings("unchecked")
			V ret = (V) readObject();
			return ret;
		}
		
		/**
		 *  Sets the value object.
		 *  
		 *  @param value Value object.
		 *  @return The previous value.
		 *  @throws IOException Thrown on IO issues.
		 */
		public V setValue(V value) throws IOException
		{
			goToPos(VAL_OFFSET);
			long oldpos = rafile.readLong();
			rafile.seek(oldpos);
			@SuppressWarnings("unchecked")
			V oldval = (V) readObject();
			if (!Objects.equals(value, oldval))
				writeObject(VAL_OFFSET, oldval);
			return oldval;
		}
		
		// Low-level methods
		
		/**
		 *  Gets the position of the previous KV node, 0 if start of chain.
		 * 
		 *  @return Position of the previous KV node, 0 if start of chain.
		 *  @throws IOException Thrown on IO issues.
		 */
		public long getPrevious() throws IOException
		{
			goToPos(PREV_OFFSET);
			return rafile.readLong();
		}
		
		/**
		 *  Sets a new previous KV node position.
		 * 
		 *  @param prev New previous KV node position
		 *  @throws IOException Thrown on IO issues.
		 */
		protected void setPrevious(long prev) throws IOException
		{
			goToPos(PREV_OFFSET);
			rafile.writeLong(prev);
		}
		
		/**
		 *  Gets the position of the next KV node, 0 if end of chain.
		 * 
		 *  @return Position of the next KV node, 0 if end of chain.
		 *  @throws IOException Thrown on IO issues.
		 */
		public long getNext() throws IOException
		{
			goToPos(NEXT_OFFSET);
			return rafile.readLong();
		}
		
		/**
		 *  Sets a new next KV node position.
		 * 
		 *  @param next New next KV node position
		 *  @throws IOException Thrown on IO issues.
		 */
		public void setNext(long next) throws IOException
		{
			goToPos(NEXT_OFFSET);
			rafile.writeLong(next);
		}
		
		/**
		 *  Returns the position of the next node reference.
		 *  
		 *  @return Next node reference.
		 */
		public long getNextReferencePosition()
		{
			return pos + NEXT_OFFSET;
		}
		
		/**
		 *  Writes an object (key or value), replacing the existing one.
		 *  Private and final to maximize in-lining potential.
		 *  
		 *  @param offset Offset in the KV pair KEY_OFFSET or VALUE_OFFSET
		 *  @param o Object to write.
		 */
		private final void writeObject(long offset, Object o) throws IOException
		{
			goToPos(offset);
			long oldpos = rafile.readLong();
			if (oldpos != 0)
			{
				// Old object lost, update garbage
				rafile.seek(rafile.readLong());
				long garbage = rafile.readLong();
				rafile.seek(HDR_POS_GARBAGE_SIZE);
				garbage += rafile.readLong();
				rafile.seek(HDR_POS_GARBAGE_SIZE);
				rafile.writeLong(garbage);
			}
			
			long newpos = appendObject(o);
			goToPos(offset);
			rafile.writeLong(newpos);
		}
		
		/**
		 *  Appends object at the end of the file using the decoder.
		 *  Private and final to maximize in-lining potential.
		 *  
		 *  Layout:
		 *  [size 8 bytes]
		 *  [Object data variable]
		 *  
		 *  @param The object being written.
		 *  @return The object's starting position.
		 */
		private final long appendObject(Object o) throws IOException
		{
			// TODO: Implement Huge Object Support, also possibly user memory mapping for large objects in general
			
			byte[] encobj = encoder.apply(o);
			
			// 8 byte alignment
			long start = getNextAlignedPosition();
			int padding = (int) (start - rafile.length());
			rafile.setLength(rafile.length() + padding + KVNode.NODE_SIZE);
			
			rafile.seek(start);
			rafile.write(encobj);
			
			return start;
		}
		
		/**
		 *  Reads object using the decoder at current file position.
		 *  Private and final to maximize in-lining potential.
		 *  
		 *  Assumed layout:
		 *  [size 8 bytes]
		 *  [Object data variable]
		 *  
		 *  @return The object.
		 */
		private final Object readObject() throws IOException
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
		 *  Seeks the file to the target position.
		 *  Private and final to maximize in-lining potential.
		 *  
		 *  @param offset Offset from the view position.
		 * @throws IOException Thrown on IO issues.
		 */
		private final void goToPos(long offset) throws IOException
		{
			long tpos = pos + offset;
			if (rafile.getFilePointer() != tpos)
			{
				rafile.seek(tpos);
			}
		}
	}
	
	@FunctionalInterface
	public interface IOFunction<T, R> {
	    R apply(T t) throws IOException;
	}
}
