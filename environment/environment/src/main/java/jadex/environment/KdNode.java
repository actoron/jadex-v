package jadex.environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import jadex.common.IFilter;
import jadex.math.IVector2;

/**
 * Node for the k-dimensional-tree.
 */
public class KdNode
{
	/** Default value for maximum number of objects in leaf nodes. */
	protected int maxLeafNodeSize;
	
	/** Default value for maximum number of samples taken to estimate coordinate median. */
	protected int maxMedianSamples;
	
	/** Value fetcher for fetching the correct coordinate on the splitting hyperplane. */
	protected IKdValueFetcher fetcher;
	
	/** Node for objects with coordinate below the splitting hyperplane, null for leaf nodes. */
	protected KdNode left;
	
	/** Node for objects with coordinate above the splitting hyperplane, null for leaf nodes. */
	protected KdNode right;
	
	/** Coordinate for the splitting hyperplane. */
	protected double hyperplane;
	
	/** List containing objects in leaf nodes, null otherwise. */
	protected List<SpaceObject> content;
	
	/**
	 *  Generates a new KdNode with the given input using default configuration.
	 *  @param input The objects.
	 *  @param random Source of randomness for median estimation sampling.
	 */
	protected KdNode(List<SpaceObject> input, Random random)
	{
		this(input, random, KdValueFetcherX.FETCHER, KdTree.DEFAULT_MAX_LEAF_NODE_SIZE, KdTree.DEFAULT_MAX_MEDIAN_SAMPLES);
	}
	
	/**
	 *  Generates a new KdNode with the given input.
	 *  @param input The objects.
	 *  @param random Source of randomness for median estimation sampling.
	 *  @param maxLeafNodeSize Maximum number of objects in leaf nodes.
	 *  @param maxMedianSamples Maximum number of samples taken to estimate median for hyperplane.
	 */
	protected KdNode(List<SpaceObject> input, Random random, int maxLeafNodeSize, int maxMedianSamples)
	{
		this(input, random, KdValueFetcherX.FETCHER, maxLeafNodeSize, maxMedianSamples);
	}
	
	/**
	 *  Generates a new KdNode with the given input.
	 *  @param input The objects.
	 *  @param random Source of randomness for median estimation sampling.
	 *  @param fetcher Value fetcher for the coordinate of the current hyperplane.
	 *  @param maxLeafNodeSize Maximum number of objects in leaf nodes.
	 *  @param maxMedianSamples Maximum number of samples taken to estimate median for hyperplane.
	 */
	protected KdNode(List<SpaceObject> input, Random random, IKdValueFetcher fetcher, int maxLeafNodeSize, int maxMedianSamples)
	{
		this.fetcher = fetcher;
		this.maxLeafNodeSize = maxLeafNodeSize;
		this.maxMedianSamples = maxMedianSamples;
		
		if (input != null)
		{
			if (input.size() <= maxLeafNodeSize)
				content = input;
			else
			{
				hyperplane = estimateMedian(input, random, fetcher);
				
				int halfSize = input.size() >> 1;
				List<SpaceObject> leftList = new ArrayList<SpaceObject>(halfSize);
				List<SpaceObject> rightList = new ArrayList<SpaceObject>(halfSize);
				
				for (SpaceObject obj : input)
				{
					if (fetcher.getValue(getPosition(obj)) < hyperplane)
						leftList.add(obj);
					else
						rightList.add(obj);
				}
				
				if (leftList.size() > 0 && rightList.size() > 0)
				{
					left = new KdNode(leftList, random, fetcher.nextFetcher(), maxLeafNodeSize, maxMedianSamples);
					right = new KdNode(rightList, random, fetcher.nextFetcher(), maxLeafNodeSize, maxMedianSamples);
				}
				else
					content = input;
			}
		}
	}
	
	/**
	 *  Finds all objects within a given search radius.
	 *  
	 *  @param point Center of the search area.
	 *  @param radiusSquared Square of the search radius.
	 *  @param filter Object filter.
	 */
	public List<SpaceObject> getNearestObjects(IVector2 point, double radiusSquared, IFilter filter)
	{
		List<SpaceObject> ret = null;
		if (content != null)
		{
			ret = new LinkedList<SpaceObject>();
			for (SpaceObject obj : content)
				if (getDistance(obj, point).getSquaredLength().getAsDouble() < radiusSquared)
					if (filter == null || filter.filter(obj))
						ret.add(obj);
		}
		else if (left != null)
		{
			double diff = fetcher.getValue(point) - hyperplane;
			double distFromMedian2 = Math.abs(diff);
			distFromMedian2 *= distFromMedian2;
			
			if (distFromMedian2 < radiusSquared)
			{
				ret = left.getNearestObjects(point, radiusSquared, filter);
				ret.addAll(right.getNearestObjects(point, radiusSquared, filter));
			}
			else if (diff < 0.0f)
				ret = left.getNearestObjects(point, radiusSquared, filter);
			else
				ret = right.getNearestObjects(point, radiusSquared, filter);
		}
		
		return ret;
	}
	
	/**
	 *  Finds the object nearest to a given point within a search radius.
	 *  @param point The point.
	 *  @param radiusSquared The squared search radius.
	 *  @return Object nearest to the point or null if none is found.
	 */
	public SpaceObject getNearestObject(IVector2 point, double radiusSquared)
	{
		return getNearestObject(point, radiusSquared, null);
	}
	
	/**
	 *  Finds the object nearest to a given point within a search radius while filtering objects.
	 *  @param point The point.
	 *  @param radiusSquared The squared search radius.
	 *  @param filter Object filter.
	 *  @return Object nearest to the point or null if none is found.
	 */
	public SpaceObject getNearestObject(IVector2 point, double radiusSquared, IFilter filter)
	{
		SpaceObject ret = null;
		if (content != null)
		{
			double lengthSquared = Double.MAX_VALUE;
			for (SpaceObject obj : content)
			{
				IVector2 dist = getDistance(obj, point);
				double sqLength = dist.getSquaredLength().getAsDouble();
				if (sqLength < lengthSquared)
				{
					if (filter.filter(obj))
					{
						ret = obj;
						lengthSquared = sqLength;
					}
				}
			}
		}
		else if (left != null)
		{
			double diff = fetcher.getValue(point) - hyperplane;
			double distFromMedian2 = Math.abs(diff);
			distFromMedian2 *= distFromMedian2;
			
			if (distFromMedian2 < radiusSquared)
			{
				ret = left.getNearestObject(point, radiusSquared, filter);
				SpaceObject rightBest = right.getNearestObject(point, radiusSquared, filter);
				if (rightBest != null)
				{
					if (ret == null)
						ret = rightBest;
					else
						if (getDistance(rightBest, point).getSquaredLength().getAsDouble() < 
								getDistance(ret, point).getSquaredLength().getAsDouble())
							ret = rightBest;
				}
			}
			else if (diff < 0.0f)
				ret = left.getNearestObject(point, radiusSquared, filter);
			else
				right.getNearestObject(point, radiusSquared, filter);
		}
		return ret;
	}
	
	/**
	 *  Estimates the median value of the hyperplane coordinate of the input using random sampling.
	 *  @param input The input objects.
	 *  @param random A source of randomness for sampling.
	 *  @param fetcher The value fetcher for the hyperplane coordinate.
	 *  @return Estimated coordinate median.
	 */
	protected double estimateMedian(List<SpaceObject> input, Random random, IKdValueFetcher fetcher)
	{
		SpaceObject[] samples = new SpaceObject[Math.min(input.size(), maxMedianSamples)];
		for (int i = 0; i < samples.length; ++i)
			samples[i] = input.get(random.nextInt(input.size()));
		
		Arrays.sort(samples, fetcher.getComparator());
		int mIndex = maxMedianSamples >> 1;
		return fetcher.getValue(getPosition(samples[mIndex + 1]));
	}
	
	/**
	 *  Interface for hyperplane coordinate value fetchers.
	 */
	protected static interface IKdValueFetcher
	{
		/**
		 *  Returns the coordinate value of the current hyperplane.
		 *  @param point The input value.
		 *  @return Coordinate value of the current hyperplane.
		 */
		public double getValue(IVector2 point);
		
		/**
		 *  Returns the fetcher for the next hyperplane in the tree.
		 * @return Fetcher for the next hyperplane in the tree.
		 */
		public IKdValueFetcher nextFetcher();
		
		/**
		 *  Returns a comparator for sorting within the current hyperplane.
		 *  @return Comparator for sorting within the current hyperplane.
		 */
		public Comparator<SpaceObject> getComparator();
	}
	
	/**
	 *  Fetcher for the x-axis values.
	 */
	protected static class KdValueFetcherX implements IKdValueFetcher
	{
		/** Static fetcher to avoid instantiation overhead */
		public static final KdValueFetcherX FETCHER = new KdValueFetcherX();
		
		/** Static comparator to avoid instantiation overhead */
		protected static final Comparator COMPARATOR = new Comparator<SpaceObject>()
		{
			public int compare(SpaceObject o1, SpaceObject o2)
			{
				return (int) Math.signum(FETCHER.getValue(getPosition(o1)) - FETCHER.getValue(getPosition(o2)));
			}
		};
		
		/**
		 *  Returns the coordinate value of the current hyperplane.
		 *  @param point The input value.
		 *  @return Coordinate value of the current hyperplane.
		 */
		public double getValue(IVector2 point)
		{
			return point.getXAsDouble();
		}
		
		/**
		 *  Returns the fetcher for the next hyperplane in the tree.
		 * @return Fetcher for the next hyperplane in the tree.
		 */
		public IKdValueFetcher nextFetcher()
		{
			return KdValueFetcherY.FETCHER;
		}
		
		/**
		 *  Returns a comparator for sorting within the current hyperplane.
		 *  @return Comparator for sorting within the current hyperplane.
		 */
		public Comparator<SpaceObject> getComparator()
		{
			return COMPARATOR;
		}
	}
	
	/**
	 *  Fetcher for the x-axis values.
	 */
	protected static class KdValueFetcherY implements IKdValueFetcher
	{
		/** Static fetcher to avoid instantiation overhead */
		public static final KdValueFetcherY FETCHER = new KdValueFetcherY();
		
		/** Static comparator to avoid instantiation overhead */
		protected static final Comparator COMPARATOR = new Comparator<SpaceObject>()
		{
			public int compare(SpaceObject o1, SpaceObject o2)
			{
				return (int) Math.signum(FETCHER.getValue(getPosition(o1)) - FETCHER.getValue(getPosition(o2)));
			}
		};
		
		/**
		 *  Returns the coordinate value of the current hyperplane.
		 *  @param point The input value.
		 *  @return Coordinate value of the current hyperplane.
		 */
		public double getValue(IVector2 point)
		{
			return point.getYAsDouble();
		}
		
		/**
		 *  Returns the fetcher for the next hyperplane in the tree.
		 * @return Fetcher for the next hyperplane in the tree.
		 */
		public IKdValueFetcher nextFetcher()
		{
			return KdValueFetcherX.FETCHER;
		}
		
		/**
		 *  Returns a comparator for sorting within the current hyperplane.
		 *  @return Comparator for sorting within the current hyperplane.
		 */
		public Comparator<SpaceObject> getComparator()
		{
			return COMPARATOR;
		}
	}
	
	protected static final IVector2 getPosition(SpaceObject obj)
	{
		return (IVector2) obj.getPosition();
	}
	
	protected static final IVector2 getDistance(SpaceObject obj, IVector2 point)
	{
		return getPosition(obj).copy().subtract(point);
	}
}
