package jadex.micro.mandelbrot.model;

import java.util.Arrays;

import jadex.common.Base64;
import jadex.common.ClassInfo;
import jadex.core.ComponentIdentifier;

/**
 * Struct for calculation of a specific mandelbrot cutout.
 */
public class AreaData
{
	/** The x start. */
	protected double xstart;

	/** The x end. */
	protected double xend;

	/** The y start. */
	protected double ystart;

	/** The y end. */
	protected double yend;

	/** The x offset. */
	protected int xoff;

	/** The y offset. */
	protected int yoff;

	/** The x size. */
	protected int sizex;

	/** The y size. */
	protected int sizey;

	/** The max value where iteration is stopped. */
	protected short max;

	///** The number of parallel workers. */
	//protected int par;

	/** The calculator service provider id. */
	protected ComponentIdentifier cid;

	/** The tasksize of a task (in pixel/points). */
	protected int tasksize;
	
	/** The algorithm used to calculate the data. */
	//protected IFractalAlgorithm	algorithm;
	protected ClassInfo	algorithm;

	/** The result data. */
	protected short[][]	data;
	
	/** The display id. */
	protected String displayid;
	
	/** The chunk count, i.e. how many intermediate results per part. */
	protected int chunkcount;
	
	/** The retrycnt. */
	protected int retrycnt;
	
	/**
	 *  Create an empty area data.
	 */
	public AreaData()
	{
		// bean constructor
	}
	
	/**
	 * Create a new area data.
	 */
	public AreaData(double xstart, double xend, double ystart, double yend, int sizex, int sizey)
	{
		this.xstart = xstart;
		this.xend = xend;
		this.ystart = ystart;
		this.yend = yend;
		this.sizex = sizex;
		this.sizey = sizey;
	}
	
	/**
	 * Create a new area data.
	 * /
	public AreaData(double xstart, double xend, double ystart, double yend,
		int sizex, int sizey, short max, int tasksize, ClassInfo algorithm, String displayid, int chunkcount)
		//int sizex, int sizey, short max, int par, int tasksize, IFractalAlgorithm algorithm, String displayid)
	{
		this(xstart, xend, ystart, yend, 0, 0, sizex, sizey, max, tasksize, algorithm, null, null, displayid, chunkcount);
		//this(xstart, xend, ystart, yend, 0, 0, sizex, sizey, max, par, tasksize, algorithm, null, null, displayid);
	}*/

	/**
	 * Create a new area data.
	 * /
	public AreaData(double xstart, double xend, double ystart, double yend,
		int xoff, int yoff, int sizex, int sizey, short max, int tasksize, ClassInfo algorithm,
		//int xoff, int yoff, int sizex, int sizey, short max, int par, int tasksize, IFractalAlgorithm algorithm,
		ComponentIdentifier cid, short[][] data, String displayid, int chunkcount)
	{
		this.xstart = xstart;
		this.xend = xend;
		this.ystart = ystart;
		this.yend = yend;
		this.xoff = xoff;
		this.yoff = yoff;
		this.sizex = sizex;
		this.sizey = sizey;
		this.max = max;
		//this.par = par;
		this.tasksize = tasksize;
		this.algorithm = algorithm;
		this.cid = cid;
		this.data = data;
		this.displayid = displayid;
		this.chunkcount = chunkcount;
	}*/

	/**
	 * Get the xstart.
	 * 
	 * @return the xstart.
	 */
	public double getXStart()
	{
		return xstart;
	}

	/**
	 * Set the xstart.
	 * 
	 * @param xstart The xstart to set.
	 */
	public AreaData setXStart(double xstart)
	{
		this.xstart = xstart;
		return this;
	}

	/**
	 * Get the xend.
	 * 
	 * @return the xend.
	 */
	public double getXEnd()
	{
		return xend;
	}

	/**
	 * Set the xend.
	 * 
	 * @param xend The xend to set.
	 */
	public AreaData setXEnd(double xend)
	{
		this.xend = xend;
		return this;
	}

	/**
	 * Get the ystart.
	 * 
	 * @return the ystart.
	 */
	public double getYStart()
	{
		return ystart;
	}

	/**
	 * Set the ystart.
	 * 
	 * @param ystart The ystart to set.
	 */
	public AreaData setYStart(double ystart)
	{
		this.ystart = ystart;
		return this; 
	}

	/**
	 * Get the yend.
	 * 
	 * @return the yend.
	 */
	public double getYEnd()
	{
		return yend;
	}

	/**
	 * Set the yend.
	 * 
	 * @param yend The yend to set.
	 */
	public AreaData setYEnd(double yend)
	{
		this.yend = yend;
		return this;
	}

	/**
	 * Get the x offset.
	 * 
	 * @return the x offset.
	 */
	public int getXOffset()
	{
		return xoff;
	}

	/**
	 * Set the x offset.
	 * 
	 * @param xoff The x offset to set.
	 */
	public AreaData setXOffset(int xoff)
	{
		this.xoff = xoff;
		return this;
	}

	/**
	 * Get the y offset.
	 * 
	 * @return the y offset.
	 */
	public int getYOffset()
	{
		return yoff;
	}

	/**
	 * Set the y offset.
	 * 
	 * @param yoff The y offset to set.
	 */
	public AreaData setYOffset(int yoff)
	{
		this.yoff = yoff;
		return this;
	}

	/**
	 * Get the sizex.
	 * 
	 * @return the sizex.
	 */
	public int getSizeX()
	{
		return sizex;
	}

	/**
	 * Set the sizex.
	 * 
	 * @param sizex The sizex to set.
	 */
	public AreaData setSizeX(int sizex)
	{
		this.sizex = sizex;
		return this;
	}

	/**
	 * Get the sizey.
	 * 
	 * @return the sizey.
	 */
	public int getSizeY()
	{
		return sizey;
	}

	/**
	 * Set the sizey.
	 * 
	 * @param sizey The sizey to set.
	 */
	public AreaData setSizeY(int sizey)
	{
		this.sizey = sizey;
		return this;
	}

	/**
	 * Get the max value.
	 * 
	 * @return the max value.
	 */
	public short getMax()
	{
		return max;
	}

	/**
	 * Set the max value.
	 * 
	 * @param max The max value to set.
	 */
	public AreaData setMax(short max)
	{
		this.max = max;
		return this;
	}

	/**
	 * Get the data.
	 * 
	 * @return the data.
	 */
	// Not called getData as it should not be serialized in XML.
	// todo: support @XMLExclude
	public short[][] fetchData()
	{
		return data;
	}

	/**
	 * Set the data.
	 * 
	 * @param data The data to set.
	 */
	public AreaData setData(short[][] data)
	{
		this.data = data;
		return this;
	}

	/**
	 * Get the data as a transferable string.
	 * 
	 * @return the data string.
	 */
	public String getDataString()
	{
		String	ret	= null;
		if(data!=null)
		{
			ret	= new String(Base64.encode(shortToByte(data)));
		}
		return ret;
	}

	/**
	 * Set the data.
	 * 
	 * @param data The data to set.
	 */
	public AreaData setDataString(String sdata)
	{
		this.data = byteToshort(Base64.decode(sdata.getBytes()));
		return this;
	}
	
	/**
	 * Set the data.
	 * 
	 * @param data The data to set.
	 */
	public static short[][] byteToshort(byte[] sdata)
	{
		int	offset	= 0;
		short	rows	= bytesToshort(sdata, offset);
		short	cols	= bytesToshort(sdata, offset+=2);
		short[][]	data	= new short[rows][cols];
		for(short i=0; i<data.length; i++)
		{
			for(short j=0; j<data[i].length; j++)
			{
				data[i][j]	= bytesToshort(sdata, offset+=2);
			}
		}
		
		return data;
	}
	
	/**
	 *  Convert bytes to a short.
	 */
	protected static short bytesToshort(byte[] buffer, int offset)
	{
		short value = (short)((0xFF & buffer[offset+0]) << 8);
		value |= (0xFF & buffer[offset+1]);

		return value;
	}
	
	/**
	 * Get the data as a transferable string.
	 * 
	 * @return the data string.
	 */
	public static byte[] shortToByte(short[][] data)
	{
		byte[]	ret	= null;
		if(data!=null)
		{
			short	rows	= (short)data.length;
			short	cols	= (short)data[0].length;
			ret	= new byte[rows*cols*2+4];
			int	offset	= 0;
			
			shortToBytes(rows, ret, offset);
			shortToBytes(cols, ret, offset+=2);
			
			for(short i=0; i<data.length; i++)
			{
				for(short j=0; j<data[i].length; j++)
				{
					shortToBytes(data[i][j], ret, offset+=2);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Convert a short to bytes.
	 */
	protected static byte[] shortToBytes(short val, byte[] buffer, int offset)
	{
		buffer[offset+0] = (byte)(val >>> 8);
		buffer[offset+1] = (byte)val;

		return buffer;
	}

	/**
	 * Get the par.
	 * 
	 * @return the par.
	 * /
	public int getParallel()
	{
		return par;
	}*/

	/**
	 * Set the par.
	 * 
	 * @param par The par to set.
	 * /
	public void setParallel(int par)
	{
		this.par = par;
	}*/

	/**
	 * Get the calculator id.
	 * 
	 * @return the calculator id.
	 */
	public ComponentIdentifier getCalculatorId()
	{
		return cid;
	}

	/**
	 * Set the calculator id.
	 * 
	 * @param id The calculator id to set.
	 */
	public AreaData setCalculatorId(ComponentIdentifier cid)
	{
		this.cid = cid;
		return this;
	}

	/**
	 * Get the tasksize.
	 * 
	 * @return the tasksize.
	 */
	public int getTaskSize()
	{
		return tasksize;
	}

	/**
	 * Set the tasksize.
	 * 
	 * @param tasksize The tasksize to set.
	 */
	public AreaData setTaskSize(int tasksize)
	{
		this.tasksize = tasksize;
		return this;
	}

	/**
	 *  Get the algorithm.
	 *  @return the algorithm.
	 * /
	public IFractalAlgorithm getAlgorithm()
	{
		return algorithm;
	}*/

	/**
	 *  Set the algorithm.
	 *  @param algorithm	The algorithm to set.
	 * /
	public void setAlgorithm(IFractalAlgorithm algorithm)
	{
		this.algorithm	= algorithm;
	}*/
	
	/**
	 *  Get the algorithm.
	 *  @return the algorithm.
	 */
	public ClassInfo getAlgorithmClass()
	{
		return algorithm;
	}

	/**
	 *  Set the algorithm.
	 *  @param algorithm	The algorithm to set.
	 */
	public AreaData setAlgorithmClass(ClassInfo algorithm)
	{
		this.algorithm	= algorithm;
		return this;
	}
	
	/**
	 *  Get the algorithm.
	 *  @return the algorithm.
	 */
	public IFractalAlgorithm getAlgorithm(ClassLoader cl)
	{
		IFractalAlgorithm ret = null;
		if(algorithm!=null)
		{
			Class<IFractalAlgorithm> clazz = (Class<IFractalAlgorithm>)algorithm.getType(cl!=null? cl: AreaData.class.getClassLoader());
			try 
			{
				ret = clazz.getDeclaredConstructor().newInstance();
			} 
			catch(Exception e) 
			{
				e.printStackTrace();
			} 
		}
		return ret;
	}
	
	/**
	 *  Add a chunk to a part
	 */
	public void addChunk(PartDataChunk data)
	{
		if(data.getData()==null)
			return;
		
		if(this.data==null)
			this.data = new short[getSizeX()][getSizeY()];
		
		short[] chunk = data.getData();
		
		int xi = data.getXStart(); //+((int)data.getArea().getX())
		int yi = data.getYStart(); //+((int)data.getArea().getY())
		int xmax = (int)(data.getArea().getWidth()); //+data.getArea().getX()
		
		//System.out.println("received: "+xi+" "+yi);
		
		//for(int i=0; i<chunk.length; i++)
		//	System.out.print(chunk[i]+"-");
		
		/*if(yi==99)
			for(int y=0; y<results.length; y++)
			{
				for(int x=0; x<results[y].length; x++)
				{
					System.out.print(results[x][y]+"-");
				}
				System.out.println();
			}
		*/
		
		try
		{
			int cnt = 0;
			while(cnt<chunk.length)
			{
				this.data[xi][yi] = chunk[cnt++];
				if(++xi>=xmax)
				{
					xi=0; // xstart only for first line in chunk
					yi++;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 *  Get the string representation.
	 * /
	public String toString()
	{
		return "AreaData(x="+xoff+", y="+yoff+" xstart="+xstart+" ystart="+ystart+")";
	}*/
	
	@Override
	public String toString() 
	{
		return "AreaData [xstart=" + xstart + ", xend=" + xend + ", ystart=" + ystart + ", yend=" + yend + ", xoff="
				+ xoff + ", yoff=" + yoff + ", sizex=" + sizex + ", sizey=" + sizey + ", max=" + max + ", cid=" + cid
				+ ", tasksize=" + tasksize + ", algorithm=" + algorithm + ", data=" + Arrays.toString(data)
				+ ", displayid=" + displayid + ", chunkcount=" + chunkcount + ", retrycnt=" + retrycnt + "]";
	}
	
	/**
	 *  Get the displayid.
	 *  @return the displayid.
	 */
	public String getDisplayId()
	{
		return displayid;
	}

	/**
	 *  Set the displayid.
	 *  @param displayid The displayid to set.
	 */
	public AreaData setDisplayId(String displayid)
	{
		this.displayid = displayid;
		return this;
	}
	
	/**
	 *  Get the chunk count.
	 *  @return The chunk count
	 */
	public int getChunkCount() 
	{
		return chunkcount;
	}

	/**
	 *  Set the chunk count.
	 *  @param chunkcount The chunk count to set
	 */
	public AreaData setChunkCount(int chunkcount) 
	{
		this.chunkcount = chunkcount;
		return this;
	}

	/**
	 *	Value for identifying this area data. 
	 */
	public Object getId()
	{
		return toString();
	}

	/**
	 * @return the retrycnt
	 */
	public int getRetryCount()
	{
		return retrycnt;
	}

	/**
	 * @param retrycnt the retrycnt to set
	 */
	public AreaData setRetryCount(int retrycnt)
	{
		this.retrycnt = retrycnt;
		return this;
	}

}
