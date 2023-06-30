package jadex.future;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.function.Function;

import jadex.collection.BloomFilter;
import jadex.common.SUtil;

/**
 *  A result listener that filters duplicates.
 */
public class DuplicateRemovalIntermediateResultListener<E> extends IntermediateDelegationResultListener<E>
{
	/** The bloom filter. */
	protected BloomFilter filter;
	
	/** The serializer function. */
	protected Function<Object, byte[]> serializer;
	
	/**
	 * Create a new listener.
	 * @param delegate The delegation target.
	 */
	public DuplicateRemovalIntermediateResultListener(IIntermediateResultListener<E> delegate, Function<Object, byte[]> serializer)
	{
		this(delegate, false, serializer);
	}

	/**
	 * Create a new listener.
	 * @param delegate The delegation target.
	 * @param undone use undone methods.
	 */
	public DuplicateRemovalIntermediateResultListener(IIntermediateResultListener<E> delegate, boolean undone, Function<Object, byte[]> serializer)
	{
		super(delegate, undone);
		this.filter = new BloomFilter();
	}

	/**
	 * Create a new listener.
	 * @param future The delegation target.
	 */
	public DuplicateRemovalIntermediateResultListener(IntermediateFuture<E> future)
	{
		this(future, false);
	}

	/**
	 * Create a new listener.
	 * @param future The delegation target.
	 * @param undone use undone methods.
	 */
	public DuplicateRemovalIntermediateResultListener(IntermediateFuture<E> future, boolean undone)
	{
		super(future, undone);
		this.filter = new BloomFilter();
	}
	
	/**
	 *  Set the bloom filter used to find duplicates.
	 *  @param filter The filter.
	 */
	public void setBloomFilter(BloomFilter filter)
	{
		this.filter = filter;
	}
	
	/**
	 *  Check results before sending them further.
	 */
	public void customIntermediateResultAvailable(E result)
	{
		if(filter.add(objectToByteArray(result)))
		{
//			System.out.println("addfil: "+result+" "+filter.hashCode());
			super.customIntermediateResultAvailable(result);
		}
//		else
//		{
//			System.out.println("Filtered out duplicate: "+result);
//		}
	}
	
	/**
	 *  Convert a value to a byte array. 
	 *  @param value The value.
	 *  @return The byte array.
	 */
	public byte[] objectToByteArray(Object value)
	{
		byte[] ret = null;
		if(value instanceof Serializable)
		{
			ByteArrayOutputStream bos = null;
			ObjectOutputStream out = null;
			try
			{
				bos = new ByteArrayOutputStream();
				out = new ObjectOutputStream(bos);
				out.writeObject(value);
				ret = bos.toByteArray();
			}
			catch(Exception e)
			{
				SUtil.rethrowAsUnchecked(e);
			}
			finally
			{
				if(bos!=null)
					try{bos.close();}catch(Exception e){SUtil.rethrowAsUnchecked(e);}
				if(out!=null)
					try{out.close();}catch(Exception e){SUtil.rethrowAsUnchecked(e);}
			}
		}	
		else 
		{
			ret = serializer.apply(value);
			//SBinarySerializer.writeObjectToByteArray(value, null);
		}
		
		return ret;
	}
}