package jadex.bdi.marsworld.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import jadex.bdi.marsworld.math.IVector2;
import jadex.common.IFilter;

/**
 * Implementation of a 2D variant of a k-dimensional-tree for spatial separation and searches.
 */
public class KdTree
{
	/** Default value for maximum number of objects in leaf nodes. */
	protected static final int DEFAULT_MAX_LEAF_NODE_SIZE = 6;
	
	/** Default value for maximum number of samples taken to estimate coordinate median. */
	protected static final int DEFAULT_MAX_MEDIAN_SAMPLES = 10;
	
	/** Current list of objects being used for tree rebuilds. */
	protected List<SpaceObject> objects;
	
	/** The root node. */
	protected KdNode root;
	
	/** Random for picking object samples for estimating median. */
	protected Random random;
	
	/** Maximum number of objects in leaf nodes. */
	protected int maxLeafNodeSize;
	
	/** Maximum number of samples taken to estimate coordinate median. */
	protected int maxMedianSamples;
	
	/**
	 *  Generates an empty KdTree using default configuration.
	 */
	public KdTree()
	{
		this(DEFAULT_MAX_LEAF_NODE_SIZE, DEFAULT_MAX_MEDIAN_SAMPLES);
	}
	
	/**
	 *  Generates an empty KdTree.
	 * 	@param maxLeafNodeSize Maximum number of objects in leaf nodes.
	 * 	@param maxMedianSamples Maximum number of samples taken to estimate coordinate median.
	 */
	public KdTree(int maxLeafNodeSize, int maxMedianSamples)
	{
		this.objects = new ArrayList<SpaceObject>();
		this.random = new Random();
		this.maxLeafNodeSize = maxLeafNodeSize;
		this.maxMedianSamples = maxMedianSamples;
	}
	
	/**
	 *  Finds all objects within a given search radius.
	 *  
	 *  @param point Center of the search area.
	 *  @param radius The search radius.
	 */
	public List<SpaceObject> getNearestObjects(IVector2 point, double radius)
	{
		return getNearestObjects(point, radius, null);
	}
	
	/**
	 *  Finds all objects within a given search radius.
	 *  
	 *  @param point Center of the search area.
	 *  @param radius The search radius.
	 *  @param filter Object filter.
	 */
	public List<SpaceObject> getNearestObjects(IVector2 point, double radius, IFilter<SpaceObject> filter)
	{
		return root != null? root.getNearestObjects(point, radius * radius, filter) : Collections.EMPTY_LIST;
	}
	
	/**
	 *  Finds an object closest to the given point (exhaustive search!).
	 * 	@param point The point.
	 * 	@return Object closest to the point.
	 */
	public SpaceObject getNearestObject(IVector2 point)
	{
		return root != null? root.getNearestObject(point, Double.MAX_VALUE) : null;
	}
	
	/**
	 *  Finds an object closest to the given point while filtering objects (exhaustive search!).
	 *  
	 * 	@param point The point.
	 *  @param filter Object filter.
	 * 	@return Object closest to the point.
	 */
	public SpaceObject getNearestObject(IVector2 point, IFilter filter)
	{
		return root != null? root.getNearestObject(point, Double.MAX_VALUE, filter) : null;
	}
	
	/**
	 *  Finds an object closest to the given point with a given search radius.
	 *  
	 * 	@param point The point.
	 * 	@param searchRadius The search radius.
	 * 	@return Object closest to the point.
	 */
	public SpaceObject getNearestObject(IVector2 point, double searchRadius)
	{
		return getNearestObject(point, searchRadius, null);
	}
	
	/**
	 *  Finds an object closest to the given point with a given search radius,
	 *  while filtering objects.
	 *  
	 * 	@param point The point.
	 * 	@param searchRadius The search radius.
	 *  @param filter Object filter.
	 * 	@return Object closest to the point.
	 */
	public SpaceObject getNearestObject(IVector2 point, double searchRadius, IFilter filter)
	{
		SpaceObject ret = null;
		if (root != null)
		{
			double sr2 = searchRadius * searchRadius;
			ret = root.getNearestObject(point, sr2, filter);
			if (ret != null && KdNode.getDistance(ret, point).getSquaredLength().getAsDouble() > sr2)
				return null;
		}
		return ret;
	}
	
	/**
	 *  Adds an object to the tree. The object will not become visible until rebuild() is called.
	 *  
	 *  @param obj The object being added.
	 */
	public void addObject(SpaceObject obj)
	{
		objects.add(obj);
	}
	
	/**
	 *  Removes an object to the tree. The object will not vanish until rebuild() is called.
	 *  @param obj The object being removed.
	 */
	public void removeObject(SpaceObject obj)
	{
		int index = objects.indexOf(obj);
		if (index == -1)
			return;
		
		if (objects.size() > 1)
		{
			objects.set(index, objects.get(objects.size() - 1));
			objects.remove(objects.size() - 1);
		}
		else
		{
			objects.clear();
		}
	}
	
	/**
	 *  Rebuilds the tree, updating spatial information, adding objects and removing objects.
	 */
	public void rebuild()
	{
		if (objects != null && !objects.isEmpty())
			root = new KdNode(objects, random);
		else
			root = null;
	}
}
