package jadex.collection;

import java.awt.image.DataBuffer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jadex.common.BufferOutputStream;
import jadex.common.SUtil;
import jadex.common.BufferInputStream;

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
public class SharedPersistentMap<K, V> implements Map<K, V>
{
	/** Default maximum percentage of garbage in the map file that needs to be cleaned up. */
	protected static final double MAX_GARBAGE_PERCENTAGE = 20.0;
	
	/** Default maximum percentage for the load factor of the hash table before re-indexing. */
	protected static final double MAX_LOAD_FACTOR = 70.0;
	
	/** Magic word starting the file. */
	protected static final short MAGIC_WORD = (short) 0xbd15;
	
	/** Type of map - normal shared map. */
	protected static final byte MAP_TYPE = 0;
	
	/** Thread local for tracking the write lock for reentrant acquisition. */
	protected static final ThreadLocal<ReentrantLock> WRITE_LOCK = new ThreadLocal<>()
	{
		protected ReentrantLock initialValue()
		{
			return null;
		};
	};
	
	
	
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
	
	/** Size of reference in the map (8 bytes / long). */
	protected static final int REF_SIZE = 8;
	
	/** Bit shift to convert a counter to a reference offset. */
	protected static final int REF_SIZE_SHIFT = Long.numberOfTrailingZeros(REF_SIZE) + 1;
	
	/** Initial index exponent, nearest prime < 2^8 = 251. */
	protected static final int INITIAL_INDEX_EXP = 8;
	
	/** Maximum index exponent, limit: hashCode() returns int. */
	protected static final int MAX_INDEX_EXP = 32;
	
	/** Minimum file size (header size + initial index size). */
	protected static final long MIN_FILE_SIZE = HEADER_SIZE + (getIndexSizeFromExp(INITIAL_INDEX_EXP) << REF_SIZE_SHIFT);
	
	/** Default Encoder using Java serialization. */
	protected static final Function<Object, ByteBuffer> DEF_ENCODER = (o) -> {
		try {
			var bos = new BufferOutputStream();
			var oos = new ObjectOutputStream(bos);
			oos.writeObject(o);
			oos.close();
			return bos.toDirectBuffer();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		};
		return null;
	};
	
	protected static final Function<ByteBuffer, Object> DEF_DECODER = (a) -> {
		try {
			a.rewind();
			//var bais = new ByteBufferInputStream(a);
			byte[] b = new byte[a.capacity()];
			a.rewind();
			a.get(b);
			var bais = new ByteArrayInputStream(b);
			var ois = new ObjectInputStream(bais);
			Object ret = ois.readObject();
			ois.close();
			return ret;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		};
		return null;
	};
	
	/** The file backing the map */
	protected File file;
	
	/** Current file access mode. */
	protected String mode = "rw";
	
	/** The memory-mapped file when opened. */
	protected RandomAccessFile rfile;
	
	/** The serialization/encoder functionality. */
	protected Function<Object, ByteBuffer> encoder = DEF_ENCODER;
	
	/** The decoder functionality. */
	protected Function<ByteBuffer, Object> decoder = DEF_DECODER;
	
	/** Factor (not percentage) for maximum garbage tolerated. */
	protected double maxgarbagefactor = MAX_GARBAGE_PERCENTAGE / 100.0;
	
	/** Maximum load factor (not percentage) of the index. */
	protected double maxloadfactor = MAX_LOAD_FACTOR / 100.0;
	
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
	 *  Sets a percentage for the maximum load of the index (load factor), default: 70%
	 * 
	 *  @param percentage Desired load percentage.
	 *  @return
	 */
	public SharedPersistentMap<K, V> setLoadPercentage(double percentage)
	{
		maxloadfactor = percentage / 100.0;
		return this;
	}
	
	/**
	 *  Sets the maximum percentage of the file that can be garbage before garbage
	 *  collection is triggered (default: 20%).
	 *  
	 *  @param percentage Maximum allowed percentage of garbage.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> setMaxGarbage(double percentage)
	{
		maxgarbagefactor = percentage / 100.0;
		return this;
	}
	
	/**
	 *  Sets the encoder for encoding/serializing objects.
	 *  
	 *  @param encoder Encoder for encoding/serializing objects.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> setEncoder(Function<Object, ByteBuffer> encoder)
	{
		this.encoder = encoder;
		return this;
	}
	
	/**
	 *  Sets the encoder for decoding objects.
	 *  
	 *  @param decoder Encoder for decoding objects.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> setDecoder(Function<ByteBuffer, Object> decoder)
	{
		this.decoder = decoder;
		return this;
	}
	
	/**
	 *  Configures the map for synchronized/non-synchronized writes.
	 *  If true, writes to the map are immediately written to
	 *  persistent storage. This will reduce the data loss in case of a crash.
	 *  
	 *  @param sync True, if writes should be written to storage immediately.
	 *  @return This map.
	 */
	public SharedPersistentMap<K, V> setSynchronized(boolean sync)
	{
		mode = sync ? "rwd" : "rw";
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
			this.rfile = new RandomAccessFile(file, mode);
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
			rfile.close();
			rfile = null;
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
	public V get(Object key)
	{
		V ret = readTransaction(() ->
		{
			V lret = null;
			KVNode keynode = findKVPair(key);
			if (keynode != null)
			{
				lret = keynode.getValue();
				
			}
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
	public boolean containsKey(Object key)
	{
		boolean ret = readTransaction(() ->
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
	 *  Checks if a value is contained in the map.
	 *  
	 *  @param value The value.
	 *  @return True, if the map contains the value.
	 */
	public boolean containsValue(Object value)
	{
		return readTransaction(() ->
		{
			long isize = readIndexSize();
			
			for (long i = 0; i < isize; ++i)
			{
				long pos = readLong(HEADER_SIZE + (i << REF_SIZE_SHIFT));
				
				if (pos != 0)
				{
					KVNode node = new KVNode(pos);
					do
					{
						V mapval = node.getValue();
						if (mapval.equals(value))
							return true;
						node = node.next();
					}
					while (node != null);
				}
			}
			return false;
		});
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
		V ret = writeTransaction(() ->
		{
			V lret = null;
			
			KVNode keynode = findKVPair(key);
			if (keynode == null)
			{
				keynode = appendKVPair(key);
				addSize(1);
			}
			lret = keynode.setValue(value);
			
			return lret;
		});
		return ret;
	}
	
	/**
	 *  Adds all entries of another map to this one.
	 *  
	 *  @param m The other map.
	 */
	public void putAll(Map<? extends K, ? extends V> m)
	{
		writeTransaction(() -> 
		{
			long entriesadded = 0;
			for (Map.Entry<? extends K, ? extends V> entry : m.entrySet())
			{
				KVNode keynode = findKVPair(entry.getKey());
				if (keynode == null)
				{
					keynode = appendKVPair(entry.getKey());
					++entriesadded;
				}
				keynode.setValue(entry.getValue());
			}
			addSize(entriesadded);
		});
	}
	
	/**
	 *  Removes key from the map.
	 *  
	 *  @param key The key.
	 *  @return The old value associates with the key or null if there was no mapping..
	 */
	public V remove(Object key)
	{
		V ret = writeTransaction((m) ->
		{
			V lret = null;
			
			KVNode keynode = findKVPair(key);
			if (keynode != null)
			{
				long garbage = keynode.getKeySize();
				garbage += keynode.getValueSize();
				garbage += KVNode.NODE_SIZE;
				
				lret = keynode.getValue();
				long prevpos = keynode.getPrevious();
				long nextpos = keynode.getNext();
				
				if (prevpos != 0)
				{
					KVNode prev = new KVNode(prevpos);
					prev.setNext(nextpos);
				}
				
				if (nextpos != 0)
				{
					KVNode next = new KVNode(nextpos);
					next.setPrevious(prevpos);
				}
				
				addSize(-1);
				addGarbage(garbage);
			}
			
			return lret;
		});
		return ret;
	}
	
	/**
	 *  Returns the entry set for the Map.
	 *  WARNING: Do not use without holding at least a read lock
	 *  while getting and using the returned set.
	 *  
	 *  @return Returns the entry set of the map.
	 *  
	 */
	public Set<Entry<K, V>> entrySet()
	{
		HashSet<Entry<K, V>> ret = new HashSet<>();
		
		long isize = readIndexSize();
		
		try
		{	
			for (long i = 0; i < isize; ++i)
			{
				long pos = readLong(HEADER_SIZE + (i << REF_SIZE_SHIFT));
				
				if (pos != 0)
				{
					KVNode node = new KVNode(pos);
					do
					{
						ret.add(new KVNode(node.getPos()));
						node = node.next();
					}
					while (node != null);
				}
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
		
		return ret;
	}
	
	/**
	 *  Returns the key set for the Map.
	 *  
	 *  WARNING: This will load all keys into memory, please consider
	 *  using entrySet() instead.
	 *  
	 *  WARNING: Do not use without holding at least a read lock
	 *  while getting and using the returned set.
	 *  
	 *  @return Returns the key set of the map.
	 *  
	 */
	public Set<K> keySet()
	{
		Set<Entry<K, V>> eset = entrySet();
		return eset.stream().map((e) -> e.getKey()).collect(Collectors.toSet());
	}
	
	/**
	 *  Returns the values of the Map.
	 *  
	 *  WARNING: This will load all values into memory, please consider
	 *  using entrySet() instead.
	 *  
	 *  WARNING: Do not use without holding at least a read lock
	 *  while getting and using the returned collection.
	 *  
	 *  @return Returns the key set of the map.
	 *  
	 */
	public Collection<V> values()
	{
		Set<Entry<K, V>> eset = entrySet();
		return eset.stream().map((e) -> e.getValue()).toList();
	}
	
	/**
     * Returns the number of key-values in this map.
     *
     * @return the number of key-values in this map
     */
    public int size()
    {
    	long size = readTransaction(() ->
		{
			return readLong(HDR_POS_MAP_SIZE);
		});
    	return (int) Math.min(size, Integer.MAX_VALUE);
    }

    /**
     * Returns true, if this map contains no key-value mappings.
     *
     * @return True, if this map contains no key-value mappings
     */
    public boolean isEmpty()
    {
    	return size() == 0;
    }
	
	/**
	 *  Clears the map, deleting all elements.
	 */
	public void clear() 
	{
		writeTransaction(() -> 
		{
			initializeMap();
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
	public void readTransaction(IORunnable transaction)
	{
		readTransaction((m) -> {
			transaction.run();
			return null;
		});
	}
	
	/**
	 *  Performs a read transaction on the map.
	 *  Engages read lock and automatically releases on success or
	 *  exception.
	 *  
	 *  @param transaction Transaction to perform.
	 *  @return Return value of lambda expression.
	 */
	public <R> R readTransaction(IOSupplier<R> transaction)
	{
		return readTransaction((m) -> transaction.get());
	}
	
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
		try(IAutoLock l = readLock())
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
	public void writeTransaction(IORunnable transaction)
	{
		writeTransaction((m) -> {
			transaction.run();
			return null;
		});
	}
	
	/**
	 *  Performs a write transaction on the map.
	 *  Engages write lock and automatically releases on success or
	 *  exception.
	 *  
	 *  @param transaction Transaction to perform.
	 *  @return Return value of lambda expression.
	 */
	public <R> R writeTransaction(IOSupplier<R> transaction)
	{
		return writeTransaction((m) -> transaction.get());
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
		try(IAutoLock l = writeLock())
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
	public IAutoLock readLock() throws IOException
	{
		IAutoLock ret = WRITE_LOCK.get();
		
		if (ret == null)
		{
			FileLock f = rfile.getChannel().lock(0, Long.MAX_VALUE, true);
			ret = new IAutoLock()
			{
				public void release()
				{
					try
					{
						f.release();
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
				}
				
				public void close()
				{
					try
					{
						f.close();
					}
					catch (IOException e)
					{
						throw new RuntimeException(e);
					}
				}
			};
		}
		else
		{
			((ReentrantLock) ret).inc();
		}
		
		return ret;
	}
	
	/**
	 *  Engages the write lock over the whole file.
	 *  
	 *  @return The lock, close to release.
	 *  
	 *  @throws IOException Thrown on IO issues.
	 */
	public IAutoLock writeLock() throws IOException
	{
		ReentrantLock ret = WRITE_LOCK.get();
		
		if (ret == null)
		{
			FileLock f = rfile.getChannel().lock(0, Long.MAX_VALUE, false);
			ret = new ReentrantLock()
			{
				public void release()
				{
					int recount = dec();
					
					if (recount < 0)
					{
						WRITE_LOCK.remove();
						try
						{
							performMaintenanceIfRequired();
						}
						catch (Exception e)
						{
							e.printStackTrace(); // Should not happen, print on console.
						}
						
						try
						{
							f.release();
						}
						catch (IOException e)
						{
							throw new RuntimeException(e);
						}
					}
				}
				
				public void close()
				{
					release();
				}
			};
		}
		else
		{
			ret.inc();
		}
		
		return ret;
	}
	
	// ****************************** Internal methods ******************************
	
	/**
	 *  Performs maintenance tasks if required.
	 *  Note: Write lock must be held before method is called.
	 *  
	 *  @throws IOException Thrown on IO issues. 
	 */
	protected void performMaintenanceIfRequired() throws IOException
	{
		long garbage = readLong(HDR_POS_GARBAGE_SIZE);
		
		long size = readLong(HDR_POS_MAP_SIZE);
		
		long indexsize = readIndexSize();
		
		/*if (garbage > rafile.length() * maxgarbagefactor ||
			size > indexsize * maxloadfactor)
		{
			File file = File.createTempFile("sharedmap", "tmp");
			SharedPersistentMap<K, V> tmpmap = new SharedPersistentMap<K, V>().setFile(file).open();
			
			//tmpmap.putAll(this);
			
			tmpmap.close();
			
			RandomAccessFile tmpraf = new RandomAccessFile(file, "r");
			rafile.setLength(tmpraf.length());
			tmpraf.seek(0);
			rafile.seek(0);
			byte[] buf = new byte[65536];
			int read = tmpraf.read(buf);
			while (read != -1)
			{
				rafile.write(buf, 0, read);
				read = tmpraf.read(buf);
			}
			while (buf == null);
			
			rafile.close();
			tmpraf.close();
			tmpraf = null;
			file.delete();
		}*/
	}
	
	/**
	 *  Reads the current index size in long words.
	 *  NOTE: Called MUST lock the file with, at minimum, a read lock.
	 *  
	 *  @return Index size or -1, if the file is invalid or empty.
	 */
	protected long readIndexSize()
	{
		long ret = -1;
		try
		{
			int fword = readInt(0);
			int maptype = fword & 0x0000FF00;
			int exp = fword & 0x000000FF;
			if ((fword >>> 16) == (MAGIC_WORD & 0xFFFF) && exp < MAX_INDEX_EXP && maptype == MAP_TYPE)
			{
				ret = getIndexSizeFromExp(exp);
			}
			else
			{
				//System.out.println(Integer.toHexString((fword >>> 16)));
				initializeMap();
				return readIndexSize();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
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
		rfile.setLength(MIN_FILE_SIZE);
		rfile.getChannel().write(ByteBuffer.allocateDirect((int) MIN_FILE_SIZE), 0);
		writeInt(0, (MAGIC_WORD << 16) | INITIAL_INDEX_EXP);
	}
	
	/**
	 *  Finds the key-value pair that contains the given key.
	 * 
	 *  @param key The key to look up.
	 *  @return The KVNode containing the key or null if not found.
	 *  @throws IOException Thrown on IO issues.
	 */
	protected KVNode findKVPair(Object key) throws IOException
	{
		long isize = readIndexSize();
		long modhash = key.hashCode() % isize;
		long nodepos = readLong(HEADER_SIZE + (modhash << REF_SIZE_SHIFT));
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
	protected KVNode appendKVPair(K key) throws IOException
	{
		// 8 byte alignment
		long start = getNextAlignedPosition();
		int padding = (int) (start - rfile.length());
		
		ByteBuffer buf = ByteBuffer.allocateDirect(KVNode.NODE_SIZE);
		rfile.setLength(rfile.length() + padding + KVNode.NODE_SIZE);
		rfile.getChannel().write(ByteBuffer.allocateDirect(KVNode.NODE_SIZE), start);
		
		long isize = readIndexSize();
		long modhash = key.hashCode() % isize;
		long refpos = HEADER_SIZE + (modhash << REF_SIZE_SHIFT);
		long nodepos = readLong(refpos);
		if (nodepos != 0)
		{
			KVNode prevnode = new KVNode(nodepos);
			while(prevnode.getNext() != 0)
				prevnode = prevnode.next();
			refpos = prevnode.getNextReferencePosition();
		}
		
		writeLong(refpos, start);
		
		KVNode ret = new KVNode(start);
		ret.setKey(key);
		return ret;
	}
	
	/**
	 *  Adds an amount to the garbage counter.
	 * 
	 *  @param garbage Amount of garbage to add to the garbage counter.
	 *  @throws IOException Thrown on IO issues.
	 */
	protected void addGarbage(long garbage) throws IOException
	{
		garbage += readLong(HDR_POS_GARBAGE_SIZE);
		writeLong(HDR_POS_GARBAGE_SIZE, garbage);
	}
	
	/**
	 *  Adds an amount to the map item size.
	 * 
	 *  @param size Amount of garbage to added to the map size.
	 *  @throws IOException Thrown on IO issues.
	 */
	protected void addSize(long size) throws IOException
	{
		size += readLong(HDR_POS_MAP_SIZE);
		writeLong(HDR_POS_MAP_SIZE, size);
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
		return size;
	}
	
	/**
	 *  Returns the next 8byte-aligned starting position after the current end of file.
	 *  
	 *  @return The next aligned starting position after the current end of file.
	 */
	private long getNextAlignedPosition() throws IOException
	{
		return (rfile.length() + 7) & ~7L;
	}
	
	/**
	 *  Reads an int value from the file.
	 *  
	 *  @param position Position of the int value in the file.
	 *  @return The long value.
	 *  @throws IOException Thrown on IO issues.
	 */
	private int readInt(long position) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(4);
		rfile.getChannel().read(buf, position);
		buf.rewind();
		return buf.getInt();
	}
	
	/**
	 *  Writes an int value from the file.
	 *  
	 *  @param position Position of the long value in the file.
	 *  param value The long value.
	 *  @throws IOException Thrown on IO issues.
	 */
	private void writeInt(long position, int value) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(4);
		buf.putInt(value);
		buf.rewind();
		rfile.getChannel().write(buf, position);
	}
	
	/**
	 *  Reads a long value from the file.
	 *  
	 *  @param position Position of the long value in the file.
	 *  @return The long value.
	 *  @throws IOException Thrown on IO issues.
	 */
	private long readLong(long position) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(8);
		rfile.getChannel().read(buf, position);
		buf.rewind();
		return buf.getLong();
	}
	
	/**
	 *  Writes a long value from the file.
	 *  
	 *  @param position Position of the long value in the file.
	 *  param value The long value.
	 *  @throws IOException Thrown on IO issues.
	 */
	private void writeLong(long position, long value) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocateDirect(8);
		buf.putLong(value);
		buf.rewind();
		rfile.getChannel().write(buf, position);
	}
	
	/**
	 *  Fully reads a buffer at a give position.
	 *  
	 *  @param position Position in the file.
	 *  @param buffer The buffer to be filled.
	 *  @throws IOException Thrown on IO issues.
	 */
	private final void readBuffer(long position, ByteBuffer buffer) throws IOException
	{
		int read = 0;
		while (read < buffer.capacity())
			read += rfile.getChannel().read(buffer, position + read);
	}
	
	/**
	 *  Returns the index size with padding.
	 *  HEADER_SIZE + padded index size = start of data
	 *  
	 *  @param exp The exponent.
	 *  @return Size of the padded index.
	 */
//	private static long getPaddedIndexSize(int exp)
//	{
//		return 1L << exp;
//	}
	
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
	protected class KVNode implements Entry<K, V>
	{
		/** Size of a KVNode */
		private static final int NODE_SIZE = 32;
		
		/** Offset for previous location. */
		private static final long PREV_OFFSET = 0;
		
		/** Offset for previous location. */
		private static final long NEXT_OFFSET = PREV_OFFSET + REF_SIZE;
		
		/** Offset for key object location. */
		private static final long KEY_OFFSET = NEXT_OFFSET + REF_SIZE;
		
		/** Offset for value object location. */
		private static final long VAL_OFFSET = KEY_OFFSET + REF_SIZE;
		
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
			pos = readLong(pos + PREV_OFFSET);
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
			return readLong(pos + PREV_OFFSET) != 0;
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
			pos = readLong(pos + NEXT_OFFSET);
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
			return readLong(pos + NEXT_OFFSET) != 0;
		}
		
		/**
		 *  Gets the key object.
		 *  
		 *  @return Key object.
		 *  @throws IOException Thrown on IO issues.
		 */
		public K getKey()
		{
			try
			{
				long keypos = readLong(pos + KEY_OFFSET);
				@SuppressWarnings("unchecked")
				K ret = (K) readObject(keypos);
				return ret;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		/**
		 *  Returns the position of the KVNode.
		 *  @return Position of the KVNode.
		 */
		public long getPos()
		{
			return pos;
		}
		
		/**
		 *  Returns the size of the key object.
		 *  
		 *  @return Size of the key object.
		 *  @throws IOException Thrown on IO issues.
		 */
		public long getKeySize() throws IOException
		{
			long size = 0;
			long pos = readLong(this.pos + KEY_OFFSET);
			
			if (pos != 0)
				size = readLong(pos);
			
			return size;
		}
		
		/**
		 *  Gets the value object.
		 *  
		 *  @return Value object.
		 *  @throws IOException Thrown on IO issues.
		 */
		public V getValue()
		{
			try
			{
				@SuppressWarnings("unchecked")
				V ret = (V) readObject(readLong(pos + VAL_OFFSET));
				return ret;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		/**
		 *  Sets the value object.
		 *  
		 *  @param value Value object.
		 *  @return The previous value.
		 *  @throws IOException Thrown on IO issues.
		 */
		public V setValue(V value)
		{
			try
			{
				long oldpos = readLong(pos + VAL_OFFSET);
				@SuppressWarnings("unchecked")
				V oldval = (V) readObject(oldpos);
				if (!Objects.equals(value, oldval))
					writeObject(VAL_OFFSET, value);
				return oldval;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		/**
		 *  Returns the size of the value object.
		 *  
		 *  @return Size of the value object.
		 *  @throws IOException Thrown on IO issues.
		 */
		public long getValueSize() throws IOException
		{
			long size = 0;
			long pos = readLong(this.pos + VAL_OFFSET);
			if (pos != 0)
			{
				size = readLong(pos);
			}
			return size;
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
			return readLong(pos + PREV_OFFSET);
		}
		
		/**
		 *  Sets a new previous KV node position.
		 * 
		 *  @param prev New previous KV node position
		 *  @throws IOException Thrown on IO issues.
		 */
		protected void setPrevious(long prev) throws IOException
		{
			writeLong(pos + PREV_OFFSET, prev);
		}
		
		/**
		 *  Gets the position of the next KV node, 0 if end of chain.
		 * 
		 *  @return Position of the next KV node, 0 if end of chain.
		 *  @throws IOException Thrown on IO issues.
		 */
		public long getNext() throws IOException
		{
			return readLong(pos + NEXT_OFFSET);
		}
		
		/**
		 *  Sets a new next KV node position.
		 * 
		 *  @param next New next KV node position
		 *  @throws IOException Thrown on IO issues.
		 */
		public void setNext(long next) throws IOException
		{
			writeLong(pos + NEXT_OFFSET, next);
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
		 *  Sets the key object.
		 *  
		 *  @param key Key object.
		 *  @throws IOException Thrown on IO issues.
		 */
		protected void setKey(K key)
		{
			try
			{
				writeObject(KEY_OFFSET, key);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		/**
		 *  Writes an object (key or value), replacing the existing one.
		 *  Private and final to maximize in-lining potential.
		 *  
		 *  @param offset Offset in the KV pair KEY_OFFSET or VALUE_OFFSET
		 *  @param o Object to write.
		 *  @throws IOException Thrown on IO issues.
		 */
		private final void writeObject(long offset, Object o) throws IOException
		{
			long oldpos = readLong(pos + offset);
			if (oldpos != 0)
			{
				// Old object lost, update garbage
				long size = readLong(oldpos);
				addGarbage(size);
			}
			
			long newpos = appendObject(o);
			writeLong(pos + offset, newpos);
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
		 *  @throws IOException Thrown on IO issues.
		 */
		private final long appendObject(Object o) throws IOException
		{
			// TODO: Implement Huge Object Support, also possibly user memory mapping for large objects in general
			
			ByteBuffer encobj = encoder.apply(o);
			
			// 8 byte alignment
			long start = getNextAlignedPosition();
			int padding = (int) (start - rfile.length());
			rfile.setLength(rfile.length() + padding + encobj.capacity() + 8);
			
			int size = encobj.capacity();
			writeLong(start, size);
			encobj.rewind();
			decoder.apply(encobj);
			encobj.rewind();
			rfile.getChannel().write(encobj, start + 8);
			
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
		 *  @param position Position in the file.
		 *  @return The object.
		 *  @throws IOException Thrown on IO issues.
		 */
		private final Object readObject(long position) throws IOException
		{
			Object ret = null;
			if (position != 0)
			{
				long longsize = readLong(position);
				if (longsize <= Integer.MAX_VALUE)
				{
					ByteBuffer buf = ByteBuffer.allocateDirect((int) longsize);
					readBuffer(position + 8, buf);
					ret = decoder.apply(buf);
				}
				else
				{
					// TODO: Implement Huge Object Support, also possibly user memory mapping for large objects in general
					throw new UnsupportedOperationException("Huge Objects not supported by this version of SharedPersistentMap");
				}
			}
			return ret;
		}
		
		
	}
	
	/**
	 *  Adds reentrant functionality to the locking mechanism by counting
	 *
	 */
	private static abstract class ReentrantLock implements IAutoLock
	{
		/** Count of reentrant locks. */
		private int reentrantcount = 0;
		
		/**
		 *  Increases the reentrant count.
		 */
		public void inc()
		{
			++reentrantcount;
		}
		
		/**
		 *  Decreases reentrant count.
		 *  @return Reentrant count after Decrease
		 */
		public int dec()
		{
			return --reentrantcount;
		}
	}
	
	/**
	 *  Lambda interface throwing IOExceptions.
	 *
	 *  @param <T> Input parameter, usually the map.
	 *  @param <R> Return value.
	 *  @throws IOException Thrown on IO issues.
	 */
	@FunctionalInterface
	public interface IOFunction<T, R> {
	    public R apply(T t) throws IOException;
	}
	
	/**
	 *  Lambda interface throwing IOExceptions without map being passed.
	 *
	 *  @param <R> Return value.
	 *  @throws IOException Thrown on IO issues.
	 */
	@FunctionalInterface
	public interface IOSupplier<R> {
		public R get() throws IOException;
	}
	
	/**
	 *  Lambda interface throwing IOExceptions without any input or output.
	 *  
	 *  @throws IOException Thrown on IO issues.
	 */
	@FunctionalInterface
	public interface IORunnable {
		public void run() throws IOException;
	}
	
	
}
