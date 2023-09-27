package jadex.collection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jadex.common.BufferInputStream;
import jadex.common.BufferOutputStream;
import jadex.common.IAutoLock;
import jadex.common.RwAutoLock;
import jadex.common.SUtil;

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
	protected static final double MAX_GARBAGE_PERCENTAGE = 50.0;
	
	/** Default minimum percentage for the load factor of the hash table before re-indexing. */
	protected static final double MIN_LOAD_FACTOR = 10.0;
	
	/** Default maximum percentage for the load factor of the hash table before re-indexing. */
	protected static final double MAX_LOAD_FACTOR = 70.0;
	
	/** Magic word starting the file. */
	protected static final int MAGIC_WORD = 0xbd15bd15;
	
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
	
	/** Size of references in the map (8 bytes / long) and size values. */
	protected static final int REF_SIZE = 8;
	
	/** Table of index sizes. */
	protected static final long[] INDEX_SIZES = new long[27];
	static {
		/*  Index size is based on the powers of two, by calculating
		 *  the nearest Mersenne prime or, if it does not exist, 
		 *  the nearest prime smaller than 2^exp. This also happens to
		 *  line up neatly with Integer.MAX_VALUE, which is a Mersenne
		 *  prime. The map supports more object than Integer.MAX_VALUE,
		 *  however, functionality degrades due to the Map API (size()
		 *  can only return Integer_MAX_VALUE)
		 */
		
		
		int expoffset = 9;
		for (int i = 0; i < INDEX_SIZES.length; ++i)
		{
			expoffset += 1;
			INDEX_SIZES[i] = (1L << (i + expoffset)) - 1;
			while (true)
			{
				if (BigInteger.valueOf(INDEX_SIZES[i]).isProbablePrime(100))
					break;
				INDEX_SIZES[i] -= 2;
			}
		}
	}
	
	/**
	 *  Size of the header:
	 *  
	 *  [4 bytes Magic Word]
	 *  [4 bytes Map Structure Version]
	 *  [8 byte Index Size]
	 *  [8 bytes Map Size/Elements]
	 *  [8 bytes garbage size]
	 *  [104 bytes reserved]
	 *  
	 */
	protected static final int HEADER_SIZE = 128;
	
	/** Position of the magic word in the header. */
	protected static final int HDR_POS_MGC_WRD = 0;
	
	/** Position of the map structure version in the header. */
	protected static final int HDR_POS_STR_VER = HDR_POS_MGC_WRD + 4;
	
	/** Position of the map size in the header. */
	protected static final int HDR_POS_IDX_SIZE = HDR_POS_MGC_WRD + REF_SIZE;
	
	/** Position of the map size in the header. */
	protected static final int HDR_POS_MAP_SIZE = HDR_POS_IDX_SIZE + REF_SIZE;
	
	/** Position of the garbage size in the header. */
	protected static final int HDR_POS_GARBAGE_SIZE = HDR_POS_MAP_SIZE + REF_SIZE;
	
	/** Position of the data pointer, pointing to the next empty byte in the file. */
	protected static final int HDR_POS_DATA_POINTER = HDR_POS_GARBAGE_SIZE + REF_SIZE;
	
	/** Bit shift to convert a counter to a reference offset. */
	protected static final int REF_SIZE_SHIFT = Long.numberOfTrailingZeros(REF_SIZE);
	
	/** Reserved heap size */
	protected static final int RESERVED_HEAP_SIZE = 128;
	
	/** Maximum index exponent, limit: hashCode() returns int. */
	protected static final int MAX_INDEX_EXP = 32;
	
	/** Bit shift defining size per mapping = 256MiB. */
	protected static final int MAX_MAP_SHIFT = 28;
	
	/** Maximum size per mapping = 256MiB. */
	protected static final int MAX_MAP = 1 << MAX_MAP_SHIFT;
	
	/** Maximum size per mapping = 64KiB. */
	protected static final int MIN_MAP = 1 << 16;
	
	/** Minimum file size (header size + initial index size). */
	protected static final long MIN_FILE_SIZE = HEADER_SIZE + INDEX_SIZES[0] << REF_SIZE_SHIFT; //(INDEX_SIZES[INITIAL_INDEX_EXP] << REF_SIZE_SHIFT) + MIN_MAP;
	
	/** Default Encoder using Java serialization. */
	protected static final Function<Object, ByteBuffer> DEF_ENCODER = (o) -> {
		try {
			var bos = new BufferOutputStream();
			var oos = new ObjectOutputStream(bos);
			oos.writeObject(o);
			oos.close();
			return bos.toBuffer();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		};
		return null;
	};
	
	protected static final Function<ByteBuffer, Object> DEF_DECODER = (a) -> {
		try {
			var bis = new BufferInputStream(a);
			var ois = new ObjectInputStream(bis);
			Object ret = ois.readObject();
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
	
	/** Minimum load factor (not percentage) of the index. */
	protected double minloadfactor = MIN_LOAD_FACTOR / 100.0;
	
	/** Maximum load factor (not percentage) of the index. */
	protected double maxloadfactor = MAX_LOAD_FACTOR / 100.0;
	
	/** Memory mapping of the header. */
	protected MappedByteBuffer headermap;
	
	/** Memory mapping of the index. */
	protected volatile MappedByteBuffer indexmap;
	
	/** Mappings for the rest of the file. */
	protected volatile List<MappedByteBuffer> mappings = new ArrayList<>();
	
	/** Lock for the mappings list. */
	protected RwAutoLock mappingslock = new RwAutoLock();
	
	/** Objects directly mapped with their own mapping */
	protected Map<Long, MappedByteBuffer> directmappings = new RwMapWrapper<>(new HashMap<>());
	
	/** 
	 * Structure version of the map to detect changes in the indexing/map structure performed by
	 * other processes / instances operating on the same file.
	 */
	protected int mapstructversion;
	
	protected volatile long indexsizecache = -1;
	
	/**
	 *  Creates a new map, configure with builder pattern.
	 */
	public SharedPersistentMap()
	{
		//mapping.duplicate()
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
		mapstructversion = -1;
		try
		{
			file.createNewFile();
			this.rfile = new RandomAccessFile(file, mode);
			if (rfile.length() == 0)
			{
				FileLock f = rfile.getChannel().lock(0, Long.MAX_VALUE, false);
				if (rfile.length() == 0)
				{
					try
					{
						initializeMap();
					}
					finally {
						f.release();
					}
				}
			}
			else if (rfile.length() < MIN_FILE_SIZE)
			{
				throw new IllegalStateException("File does not appear to be a shared map: " + file.getName());
			}
			else
			{
				headermap = rfile.getChannel().map(MapMode.READ_WRITE, 0, HEADER_SIZE);
				headermap.rewind();
			}
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
			rfile.getChannel().close();
		}
		catch (IOException e)
		{
		}
		try
		{
			rfile.close();
		}
		catch (IOException e)
		{
		}
		rfile = null;
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
				long pos = getIndexReference(i);
				
				if (pos != 0)
				{
					KVNode node = new KVNode(getMappedBuffer(heapToAbsolute(pos), KVNode.NODE_SIZE , false));
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
			var eset = m.entrySet();
			for (Map.Entry<? extends K, ? extends V> entry : eset)
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
				
				
				if (prevpos <= 0)
				{
					// Beginning of linked list, node in index
					long isize = readIndexSize();
					long modhash = keynode.getRawHash() % isize;
					MappedByteBuffer ibuf = getIndexMap().duplicate();
					ibuf.position((int) (modhash << REF_SIZE_SHIFT));
					ibuf.putLong(nextpos);
					if (nextpos > 0)
					{
						// Not sole element in bucket
						KVNode next = new KVNode(getMappedBuffer(heapToAbsolute(nextpos), KVNode.NODE_SIZE, false));
						next.setPrevious(0);
					}
				}
				else
				{
					// Node in bucket
					KVNode prev = new KVNode(getMappedBuffer(heapToAbsolute(prevpos), KVNode.NODE_SIZE, false));
					prev.setNext(nextpos);
					
					if (nextpos > 0)
					{
						// Not end of linked list
						KVNode next = new KVNode(getMappedBuffer(heapToAbsolute(nextpos), KVNode.NODE_SIZE, false));
						next.setPrevious(prevpos);
					}
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
		
		long msize = getHeaderLong(HDR_POS_MAP_SIZE);
		
		try
		{	
			for (long i = 0; i < isize; ++i)
			{
				long pos = getIndexReference(i);
				
				if (pos  > 0)
				{
					KVNode node = new KVNode(getMappedBuffer(heapToAbsolute(pos), KVNode.NODE_SIZE, false));
					do
					{
						ret.add(new KVNode(node));
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
			return getHeaderLong(HDR_POS_MAP_SIZE);
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
			mapstructversion = getHeaderInt(HDR_POS_STR_VER);
			initializeMap();
			MappedByteBuffer hmap = headermap.duplicate();
			hmap.position(HDR_POS_STR_VER);
			hmap.putInt(mapstructversion + 1);
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
			verifyMapState();
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
			verifyMapState();
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
	 *  Verifies the map file and memory mapping state.
	 */
	protected void verifyMapState()
	{
		int mgcwrd = getHeaderInt(HDR_POS_MGC_WRD);
		
		if (mgcwrd != MAGIC_WORD)
			throw new IllegalStateException("File is not a shared map.");
		
		int curstructversion = getHeaderInt(HDR_POS_STR_VER);
		if (curstructversion != mapstructversion)
		{
			indexmap = null;
			try (IAutoLock l = mappingslock.writeLock())
			{
				mappings = new ArrayList<>();
			}
			directmappings.clear();
			mapstructversion = curstructversion;
		}
	}
	
	/**
	 *  Performs maintenance tasks if required.
	 *  Note: Write lock must be held before method is called.
	 *  
	 *  @throws IOException Thrown on IO issues. 
	 */
	protected void performMaintenanceIfRequired() throws IOException
	{
		long garbage = getHeaderLong(HDR_POS_GARBAGE_SIZE);
		
		long size = getHeaderLong(HDR_POS_MAP_SIZE);
		
		long indexsize = readIndexSize();
		
		long corrisize = getIndexSizeForMapSize(size);
		
		boolean restructured = false;
		if (garbage > getHeaderLong(HDR_POS_DATA_POINTER) * maxgarbagefactor || corrisize != indexsize)
		{
			// Full restructuring is needed to release garbage,
			// Index is also resized if required.
			
			System.out.println("Restructuring map... " + size + " " + indexsize + " " + garbage);
			
			File tmpfile = File.createTempFile("sharedmap", "tmp");
			
			SharedPersistentMap<K, V> tmpmap = new SharedPersistentMap<K, V>().setFile(tmpfile).open();
			try (FileLock l = tmpmap.rfile.getChannel().lock(0, Long.MAX_VALUE, false))
			{
				tmpmap.initializeMap(corrisize);
				
				var entryset = this.entrySet();
				for (Map.Entry<K, V> e : entryset)
				{
					KVNode node = tmpmap.appendKVPair(e.getKey());
					node.overwriteValue(e.getValue());
				}
				tmpmap.addSize(entryset.size());
			}
			
			tmpmap.close();
			
			RandomAccessFile tmpraf = new RandomAccessFile(tmpfile, "r");
			rfile.setLength(tmpraf.length());
			tmpraf.seek(0);
			rfile.seek(0);
			byte[] buf = new byte[65536];
			int read = tmpraf.read(buf);
			while (read != -1)
			{
				rfile.write(buf, 0, read);
				read = tmpraf.read(buf);
			}			
			
			rfile.seek(0);
			
			restructured = true;
			
			tmpraf.close();
			tmpraf = null;
			tmpfile.delete();
		}
		/*else if (corrisize > indexsize)
		{
			// just needs growing index
			
			ByteBuffer indexbuffer = ByteBuffer.allocate((int) (corrisize << REF_SIZE_SHIFT));
			long deltasize = (corrisize - indexsize) << REF_SIZE_SHIFT;
			
			var entryset = this.entrySet();
			for (long i = 0; i < indexsize; ++i)
			{
				long pos = getIndexReference(i);
				
				if (pos != 0)
				{
					do
					{
						KVNode node = new KVNode(getMappedBuffer(heapToAbsolute(pos), KVNode.NODE_SIZE, false));
						int ibytepos = (int) ((node.getRawHash() % corrisize) << REF_SIZE_SHIFT);
						
						indexbuffer.position(ibytepos);
						long collnodepos = indexbuffer.getLong();
						long nextpos = node.getNext();
						
						if (collnodepos != 0)
						{
							KVNode collisionnode = new KVNode(getMappedBuffer(heapToAbsolute(collnodepos), KVNode.NODE_SIZE, false));
							collisionnode.setPrevious(pos);
							node.setNext(collnodepos);
							node.setPrevious(0);
						}
						else
						{
							node.setNext(0);
							node.setPrevious(0);
						}
						
						indexbuffer.position(ibytepos);
						indexbuffer.putLong(pos);
						pos = nextpos;
					}
					while (pos != 0);
				}
			}
			
			
			//for (Map.Entry<K, V> e : entryset)
			//{
			//	KVNode node = (SharedPersistentMap<K, V>.KVNode) e;
			//	long pos = node.getPrevious();
			//	if (pos > 0)
			//		node.setPrevious(pos + deltasize);
			//	pos = node.getNext();
			//	if (pos > 0)
			//		node.setNext(pos + deltasize);
			//}
			
			long filepos = rfile.length();
			long remaining = filepos - indexsize - HEADER_SIZE;
			rfile.setLength(rfile.length() + deltasize);
			
			
			byte[] buf = new byte[(int) deltasize];
			while (remaining > 0)
			{
				if (remaining >= buf.length)
				{
					rfile.seek(filepos - buf.length);
					rfile.readFully(buf);
					rfile.seek(filepos);
					rfile.write(buf);
					filepos -= buf.length;
					remaining -= buf.length;
				}
				else
				{
					rfile.seek(filepos - remaining);
					rfile.readFully(buf, 0, (int) remaining);
					rfile.seek(filepos);
					rfile.write(buf);
					filepos -= remaining;
					remaining = 0;
				}
			}
			
			//entryset = this.entrySet();
			//dSystem.out.println("Entry set size2 " + entryset.size());
			
			rfile.seek(HEADER_SIZE);
			rfile.write(indexbuffer.array());
			
			restructured = true;
		}*/
		
		if (restructured)
		{
			indexmap = null;
			try (IAutoLock l = mappingslock.writeLock())
			{
				mappings.clear();
			}
			directmappings.clear();
			
			MappedByteBuffer hbuf = headermap.duplicate();
			hbuf.position(HDR_POS_STR_VER);
			hbuf.putInt(++mapstructversion);
		}
	}
	
	/**
	 *  Returns the appropriate index size for the size of the map.
	 * 
	 *  @param mapsize Map size.
	 *  @param indexsize Current index size.
	 *  @return Correct index size.
	 */
	protected long getIndexSizeForMapSize(long mapsize)
	{
		int i = 0;
		while (INDEX_SIZES[i] * maxloadfactor < mapsize)
			++i;
		long isize = INDEX_SIZES[i];
		return isize;
	}
	
	/**
	 *  Reads the current index size in long words.
	 *  NOTE: Called MUST lock the file with, at minimum, a read lock.
	 *  
	 *  @return Index size or -1, if the file is invalid or empty.
	 */
	protected long readIndexSize()
	{
		long ret = getHeaderLong(HDR_POS_IDX_SIZE);
		return ret;
	}
	
	/**
	 * Initializes the map file as an empty map.
	 * 
	 * @throws IOException Thrown on IO issues. 
	 */
	protected void initializeMap() throws IOException
	{
		long isize = INDEX_SIZES[0];
		initializeMap(isize);
	}
	
	/**
	 * Initializes the map file as an empty map with a specified index size.
	 * 
	 * @param isize Index size.
	 * @throws IOException Thrown on IO issues. 
	 */
	protected void initializeMap(long isize) throws IOException
	{
		int len = HEADER_SIZE;
		rfile.getChannel().write(ByteBuffer.allocate(len), 0);
		len =  (int) (isize << REF_SIZE_SHIFT);
		rfile.getChannel().write(ByteBuffer.allocate(len));
		//rfile.getChannel().write(fill(ByteBuffer.allocate(len), (byte) 0xFF), HEADER_SIZE);
				
		headermap = rfile.getChannel().map(MapMode.READ_WRITE, 0, HEADER_SIZE);
		
		MappedByteBuffer hbuf = headermap.duplicate();
		hbuf.position(HDR_POS_MGC_WRD);
		hbuf.putInt(MAGIC_WORD);
		hbuf.position(HDR_POS_STR_VER);
		hbuf.putInt(0);
		
		hbuf.position(HDR_POS_DATA_POINTER);
		hbuf.putLong(HEADER_SIZE + (isize << REF_SIZE_SHIFT) + RESERVED_HEAP_SIZE);
		
		//rfile.getChannel().write(ByteBuffer.allocateDirect((int) MIN_FILE_SIZE), 0);
		
		putHeaderLong(HDR_POS_IDX_SIZE, isize);
		mappings.clear();
		//mappings.add(rfile.getChannel().map(MapMode.READ_WRITE, HEADER_SIZE + isize, MIN_MAP));
		
		
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
		long nodepos = getIndexReference(getIndexPosition(key.hashCode()));
		
		if (nodepos > 0)
		{
			KVNode kvnode = new KVNode(getMappedBuffer(heapToAbsolute(nodepos), KVNode.NODE_SIZE, false));
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
		long heappos = absoluteToHeap(start);
		
		putHeaderLong(HDR_POS_DATA_POINTER, start + KVNode.NODE_SIZE);
		
		long ipos = getIndexPosition(key.hashCode());
		long nodepos = getIndexReference(ipos);
		
		MappedByteBuffer kvbuf = getMappedBuffer(start, KVNode.NODE_SIZE, true);
		
		KVNode ret = new KVNode(kvbuf);
		/*kvbuf.reset();
		kvbuf.put(fill(ByteBuffer.allocate(KVNode.NODE_SIZE), (byte) 0xFF));
		kvbuf.reset();*/
		
		if (nodepos  > 0)
		{
			KVNode collisionnode = new KVNode(getMappedBuffer(heapToAbsolute(nodepos), KVNode.NODE_SIZE, false));
			collisionnode.setPrevious(heappos);
			ret.setNext(nodepos);
		}
		
		MappedByteBuffer ibuf = getIndexMap().duplicate();
		ibuf.position((int) (ipos << REF_SIZE_SHIFT));
		ibuf.putLong(heappos);
		
		ret.setKey(key);
		return ret;
	}
	
	/**
	 *  Converts an absolute position to a heap position
	 * @param absolute Absolute position in the file.
	 * @return Position in the heap.
	 */
	protected long absoluteToHeap(long absolute)
	{
		return absolute - HEADER_SIZE - (readIndexSize() << REF_SIZE_SHIFT);
	}
	
	/**
	 *  Converts an absolute position to a heap position
	 * @param heap Position in the heap.
	 * @return Absolute position in the file.
	 */
	protected long heapToAbsolute(long heap)
	{
		return heap + HEADER_SIZE + (readIndexSize() << REF_SIZE_SHIFT);
	}
	
	/*
	 *  Converts a hashCode() hash into a valid relative index position.
	 */
	public long getIndexPosition(int hash)
	{
		long isize = readIndexSize();
		
		if (isize < 0)
			return -1;
		
		long modhash = Integer.toUnsignedLong(hash) % isize;
		return modhash;
	}
	
	/**
	 *  Reads the positional reference at a given position in the hashtable index.
	 *  
	 *  @param indexposition Position in the index.
	 *  @return KVNode reference at the given index position.
	 * @throws IOException 
	 */
	protected long getIndexReference(long indexposition) throws IOException
	{
		MappedByteBuffer ibuf = getIndexMap().duplicate();
		ibuf.position((int) (indexposition << REF_SIZE_SHIFT));
		return ibuf.getLong();
	}
	
	/**
	 *  Reads an int-value from the header.
	 *  
	 *  @param headerpos Position in the header.
	 *  @return Value at the position.
	 */
	protected int getHeaderInt(int headerpos)
	{
		MappedByteBuffer hbuf = headermap.duplicate();
		hbuf.position(headerpos);
		return hbuf.getInt();
	}
	
	/**
	 *  Reads a long-value from the header.
	 *  
	 *  @param headerpos Position in the header.
	 *  @return Value at the position.
	 */
	protected long getHeaderLong(int headerpos)
	{
		MappedByteBuffer hbuf = headermap.duplicate();
		hbuf.position(headerpos);
		return hbuf.getLong();
	}
	
	/**
	 *  Writes a long-value from the header.
	 *  
	 *  @param headerpos Position in the header.
	 *  @param value Value to write at the position.
	 */
	protected void putHeaderLong(int headerpos, long value)
	{
		MappedByteBuffer hbuf = headermap.duplicate();
		hbuf.position(headerpos);
		hbuf.putLong(value);
	}
	
	/**
	 *  Adds an amount to the garbage counter.
	 * 
	 *  @param garbage Amount of garbage to add to the garbage counter.
	 *  @throws IOException Thrown on IO issues.
	 */
	protected void addGarbage(long garbage) throws IOException
	{
		MappedByteBuffer hbuf = headermap.duplicate();
		hbuf.position(HDR_POS_GARBAGE_SIZE);
		garbage += hbuf.getLong();
		hbuf.position(HDR_POS_GARBAGE_SIZE);
		hbuf.putLong(garbage);
	}
	
	/**
	 *  Adds an amount to the map item size.
	 * 
	 *  @param size Amount of garbage to added to the map size.
	 *  @throws IOException Thrown on IO issues.
	 */
	protected void addSize(long size) throws IOException
	{
		MappedByteBuffer hbuf = headermap.duplicate();
		hbuf.position(HDR_POS_MAP_SIZE);
		size += hbuf.getLong();
		hbuf.position(HDR_POS_MAP_SIZE);
		hbuf.putLong(size);
	}
	
	/**
	 *  Returns the next 8byte-aligned starting position after the current end of file.
	 *  
	 *  @return The next aligned starting position after the current end of file.
	 */
	protected long getNextAlignedPosition() throws IOException
	{
		long dp = getHeaderLong(HDR_POS_DATA_POINTER);
		return (dp + 7) & ~7L;
	}
	
	/**
	 *  Fills a ByteBuffer with a byte value.
	 *  @param b ByteBuffer object.
	 *  @param fillval Fill value.
	 */
	protected ByteBuffer fill(ByteBuffer b, byte fillval)
	{
		b.rewind();
		byte[] fill = new byte[b.capacity()];
		Arrays.fill(fill, fillval);
		b.put(fill);
		b.rewind();
		return b;
	}
	
	protected MappedByteBuffer getMappedBuffer(long absposition, long lsize, boolean append) throws IOException
	{
		if (absposition < 0)
			throw new IllegalArgumentException("Negative position requested: " + absposition);
		
		// This should be ok since we ought to have the
		// write lock if we are enlarging the file.
		if (append && rfile.length() < absposition + lsize)
		{
			long isize;
			long size = rfile.length() - HEADER_SIZE - (isize = (readIndexSize() << REF_SIZE_SHIFT));
			if (size <= MAX_MAP)
			{
				size <<= 1;
				rfile.setLength(HEADER_SIZE + isize + size);
			}
			else
			{
				if (rfile.length() < MAX_MAP)
					rfile.setLength(MAX_MAP + MAX_MAP);
				else
					rfile.setLength(rfile.length() + MAX_MAP);
			}
		}	
		
		MappedByteBuffer ret = null;
		long isize = 0;
		if (absposition < HEADER_SIZE)
		{
			// Header part of file
			ret = headermap.duplicate();
			ret.position((int) absposition);
			ret.mark();
		}
		else if (absposition - HEADER_SIZE < (isize = (readIndexSize() << REF_SIZE_SHIFT)))
		{
			// Index part of file
			ret = getIndexMap().duplicate();
			ret.position((int) (absposition - HEADER_SIZE));
			ret.mark();
		}
		else
		{
			// Heap part of file
			long relpos = absposition - HEADER_SIZE - isize;
			if (lsize < MAX_MAP || (relpos % MAX_MAP) + lsize <= MAX_MAP)
			{
				int mapidx = (int) (relpos >>> MAX_MAP_SHIFT);
				
				try (IAutoLock l = mappingslock.readLock())
				{
					if (mappings.size() > mapidx)
						ret = mappings.get(mapidx);
				}
				int expectedmapsize = MAX_MAP;
				
				if (mapidx == 0)
				{
					expectedmapsize = nextPowerOfTwo((int) (relpos + lsize));
					if (ret != null && ret.capacity() < expectedmapsize)
					{
						ret = null;
					}
				}
				
				if (ret == null)
				{
					ret = rfile.getChannel().map(MapMode.READ_WRITE, HEADER_SIZE + isize, expectedmapsize);
					try (IAutoLock l = mappingslock.writeLock())
					{
						while(mappings.size() <= mapidx)
							mappings.add(null);
						mappings.set(mapidx, ret);
					}
				}
				
				ret = ret.duplicate();
				ret.position((int) relpos);
				ret.mark();
			}
			else
			{
				// Directly mapped
				ret = directmappings.get(absposition);
				
				if (ret == null)
				{
					ret = rfile.getChannel().map(MapMode.READ_WRITE, absposition, lsize);
					directmappings.put(absposition, ret);
				}
				
				ret = ret.duplicate();
				
				// Direct mappings start at position 0.
				ret.rewind();
				ret.mark();
			}
		}
		
		return ret;
	}
	
	protected MappedByteBuffer getIndexMap() throws IOException
	{
		if (indexmap == null)
		{
			long isize = readIndexSize() << REF_SIZE_SHIFT;
			indexmap = rfile.getChannel().map(MapMode.READ_WRITE, HEADER_SIZE, isize);
			indexmap.rewind();
		}
		
		return indexmap;
	}
	
	/**
	 *  Calculates the next power of two that's equal or larger than x.
	 *  Note: Does not handle zero or negative values.
	 *  
	 *  @param x Any positive int value
	 *  @return Next power of two.
	 */
	public final int nextPowerOfTwo(int x)
	{
		x--;
	    x |= x >> 1;
	    x |= x >> 2;
	    x |= x >> 4;
	    x |= x >> 8;
	    x |= x >> 16;
	    x++;

	    return x;
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
		/** Offset for previous location. */
		private static final int PREV_OFFSET = 0;
		
		/** Offset for previous location. */
		private static final int NEXT_OFFSET = PREV_OFFSET + REF_SIZE;
		
		/** Offset for key object location. */
		private static final int KEY_OFFSET = NEXT_OFFSET + REF_SIZE;
		
		/** Size of the key object */
		private static final int KEY_SIZE_OFFSET = KEY_OFFSET + REF_SIZE;
		
		/** Raw hash value of the key object (Integer.toUnsignedLong(key.hashCode()). */
		private static final int KEY_RAWHASH_OFFSET = KEY_SIZE_OFFSET + REF_SIZE;
		
		/** Offset for value object location. */
		private static final int VAL_OFFSET = KEY_RAWHASH_OFFSET + REF_SIZE;
		
		/** Size of the value object */
		private static final int VAL_SIZE_OFFSET = VAL_OFFSET + REF_SIZE;
		
		/** Size of a KVNode */
		private static final int NODE_SIZE = (int) (VAL_SIZE_OFFSET + REF_SIZE);
		
		/** MappedByteBuffer containing the KV node within the file. */
		private MappedByteBuffer buffer;
		
		/**
		 *  Creates a view on a KV node.
		 *  @param position Position of the KV node.
		 */
		public KVNode
		(MappedByteBuffer buffer)
		{
			this.buffer = buffer;
		}
		
		/**
		 *  Creates a view on a KV node.
		 *  @param position Position of the KV node.
		 */
		public KVNode(KVNode other)
		{
			this.buffer = other.buffer.duplicate();
			buffer.reset();
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
			long pos = getLong(PREV_OFFSET);
			if (pos <= 0)
				return null;
			buffer = getMappedBuffer(heapToAbsolute(pos), NODE_SIZE, false);
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
			return getLong(PREV_OFFSET)  > 0;
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
			long pos = getLong(NEXT_OFFSET);
			if (pos <= 0)
				return null;
			buffer = getMappedBuffer(heapToAbsolute(pos), NODE_SIZE, false);
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
			return getLong(NEXT_OFFSET) > 0;
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
				long keypos = getLong(KEY_OFFSET);
				// Use the fact that size follows position.
				long keysize = buffer.getLong();
				
				@SuppressWarnings("unchecked")
				K ret = (K) readObject(keypos, keysize);
				
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
		/*public long getPos()
		{
			buffer.reset();
			return buffer.position();
		}*/
		
		/**
		 *  Returns the size of the key object.
		 *  
		 *  @return Size of the key object.
		 *  @throws IOException Thrown on IO issues.
		 */
		public long getKeySize() throws IOException
		{
			return getLong(KEY_SIZE_OFFSET);
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
				long valpos = getLong(VAL_OFFSET);
				// Use the fact that size follows position.
				long valsize = buffer.getLong();
				
				@SuppressWarnings("unchecked")
				V ret = (V) readObject(valpos, valsize);
				
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
				long oldpos = getLong(VAL_OFFSET);
				// Use the fact that size follows position.
				long oldsize = buffer.getLong();
				@SuppressWarnings("unchecked")
				V oldval = (V) readObject(oldpos, oldsize);
				if (!Objects.equals(value, oldval))
				{
					writeObject(VAL_OFFSET, value);
					addGarbage(oldsize);
				}
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
			return getLong(VAL_SIZE_OFFSET);
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
			return getLong(PREV_OFFSET);
		}
		
		/**
		 *  Sets a new previous KV node position.
		 * 
		 *  @param prev New previous KV node position
		 *  @throws IOException Thrown on IO issues.
		 */
		protected void setPrevious(long prev) throws IOException
		{
			buffer.reset();
			buffer.position(buffer.position() + PREV_OFFSET);
			buffer.putLong(prev);
		}
		
		/**
		 *  Gets the position of the next KV node, 0 if end of chain.
		 * 
		 *  @return Position of the next KV node, 0 if end of chain.
		 *  @throws IOException Thrown on IO issues.
		 */
		public long getNext() throws IOException
		{
			return getLong(NEXT_OFFSET);
		}
		
		/**
		 *  Sets a new next KV node position.
		 * 
		 *  @param next New next KV node position
		 *  @throws IOException Thrown on IO issues.
		 */
		public void setNext(long next) throws IOException
		{
			buffer.reset();
			buffer.position(buffer.position() + NEXT_OFFSET);
			buffer.putLong(next);
		}
		
		/**
		 *  Returns the raw hashcode for the key.
		 *  @return Raw hash code.
		 */
		public long getRawHash()
		{
			return getLong(KEY_RAWHASH_OFFSET);
		}
		
		/**
		 *  Returns the position of the next node reference.
		 *  
		 *  @return Next node reference.
		 */
//		public long getNextReferencePosition()
//		{
//			return pos + NEXT_OFFSET;
//		}
		
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
				buffer.reset();
				buffer.position(buffer.position() + KEY_RAWHASH_OFFSET);
				buffer.putLong(Integer.toUnsignedLong(key.hashCode()));
				buffer.reset();
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		
		/**
		 *  Overwrites the value object.
		 *  
		 *  @param value Value object.
		 *  @throws IOException Thrown on IO issues.
		 */
		protected void overwriteValue(V value) throws IOException
		{
			long oldsize = getLong(VAL_SIZE_OFFSET);
			writeObject(VAL_OFFSET, value);
			if (oldsize > 0)
				addGarbage(oldsize);
		}
		
		/**
		 * Reads a long value in the KVNode.
		 * 
		 *  @param offset Offset to the long value.
		 *  @return The long value read.
		 */
		private long getLong(int offset)
		{
			buffer.reset();
			buffer.position(buffer.position() + offset);
			return buffer.getLong();
		}
		
		/**
		 *  Writes an object (key or value), replacing the existing one.
		 *  
		 *  @param offset Offset in the KV pair KEY_OFFSET or VALUE_OFFSET
		 *  @param o Object to write.
		 *  @throws IOException Thrown on IO issues.
		 */
		private void writeObject(int offset, Object o) throws IOException
		{
			// TODO: Implement Huge Object Support, also possibly user memory mapping for large objects in general
			
			ByteBuffer encobj = encoder.apply(o);
			int size = encobj.capacity();
			
			// 8 byte alignment
			long start = getNextAlignedPosition();
			putHeaderLong(HDR_POS_DATA_POINTER, start + size);
			
			MappedByteBuffer tbuf = getMappedBuffer(start, size, true);
			tbuf.put(encobj);
			
			buffer.reset();
			buffer.position(buffer.position() + offset);
			buffer.putLong(absoluteToHeap(start));
			// Use the fact that size follows position.
			buffer.putLong(size);
		}
		
		/**
		 *  Reads object using the decoder at current file position.
		 *  
		 *  Assumed layout:
		 *  [size 8 bytes]
		 *  [Object data variable]
		 *  
		 *  @param heapposition Position in the heap.
		 *  @return The object.
		 *  @throws IOException Thrown on IO issues.
		 */
		private Object readObject(long heapposition, long size) throws IOException
		{
			if (size >= Integer.MAX_VALUE)
			{
				// TODO: Implement Huge Object Support, also possibly user memory mapping for large objects in general
				throw new UnsupportedOperationException("Huge Objects not supported by this version of SharedPersistentMap");
			}
			
			Object ret = null;
			if (heapposition > 0)
			{
				MappedByteBuffer buf = getMappedBuffer(heapToAbsolute(heapposition), size, false);
				buf.reset();
				buf = buf.slice(buf.position(), (int) size);
				ret = decoder.apply(buf);
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
	
	public static void main(String[] args)
	{
		File file = new File("stringtest.map");
		file.delete();
		
		/*SharedPersistentMap<String, String> smap = new SharedPersistentMap<>();
		smap.setFile(file.getAbsolutePath()).open();
		smap.put("Cat", "Meow!");
		
		smap.put("Dog", "Woof!");
		
		System.out.println(smap.get("Cat"));*/
		
		int tsize = 100000;
		String[] vals = new String[tsize];
		for (int i = 0; i < tsize; ++i)
			vals[i] = SUtil.createUniqueId();
		
		try
		{
			System.in.read();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
		
		long t = System.currentTimeMillis();
		for (int i = 0; i < tsize; ++i)
			map.put(String.valueOf(i), vals[i]);
		t = System.currentTimeMillis() - t;
		System.out.println("HashMap filled with " + tsize + " K-V pairs: " + t + "ms.");
		
		SharedPersistentMap<String, String> smap = new SharedPersistentMap<>();
		smap.setFile(file.getAbsolutePath()).open();
		
		t = System.currentTimeMillis();
		for (int i = 0; i < tsize; ++i)
		{
			smap.put(String.valueOf(i), vals[i]);
			if (smap.get(String.valueOf(i)) == null)
				System.out.println("put fail " + i);
		}
		t = System.currentTimeMillis() - t;
		System.out.println("SharedMap filled with " + tsize + " K-V pairs: " + t + "ms.");
		int aput = smap.size();
		
		t = System.currentTimeMillis();
		for (int i = 0; i < tsize; ++i)
			map.get(String.valueOf(i));
		t = System.currentTimeMillis() - t;
		System.out.println("HashMap reads with " + tsize + " K-V pairs: " + t + "ms.");
		
		t = System.currentTimeMillis();
		for (int i = 0; i < tsize; ++i)
			smap.get(String.valueOf(i));
		t = System.currentTimeMillis() - t;
		System.out.println("SharedMap reads with " + tsize + " K-V pairs: " + t + "ms.");
		
		int brem = smap.size();
		t = System.currentTimeMillis();
		for (int i = 0; i < tsize / 2; ++i)
		{
			smap.remove(String.valueOf(i));
		}
		t = System.currentTimeMillis() - t;
		System.out.println("SharedMap remove " + (tsize/2) + " of " + tsize + " K-V pairs: " + t + "ms.");
		System.out.println("Map size after put " + aput);
		System.out.println("Map Size before removal " + brem);
		System.out.println("Map Size after removal " + smap.size());
		
		smap.clear();
		
		/*SharedPersistentMap<String, String> smap = new SharedPersistentMap<>();
		smap.setFile(file.getAbsolutePath()).open();
		smap.clear();
		smap.put("Cat", "Meow!");
		smap.put("Dog", "Woof!");
		
		System.out.println("get Cat " + smap.get("Cat"));
		*/
		file.delete();
	}
}
