package jadex.mj.feature.execution;

import jadex.mj.core.MjComponent;
import jadex.mj.feature.execution.impl.MjExecutionFeature;

/**
 *  The execution feature controls how and when components execute their steps,
 *  e.g., single-threaded vs parallel steps, real-time vs. simulation time.
 */
public interface IMjExecutionFeature	extends IMjExternalExecutionFeature
{
	/** Constant for unset step level. */
	public static final int STEP_PRIORITY_UNSET = -1;
	
	/** Constant for first normal step level. */
	public static final int STEP_PRIORITY_NORMAL = 0;

	/** Constant for first immediate step level. */
	public static final int STEP_PRIORITY_IMMEDIATE = 100;
	
	/**
	 *  Get the feature instance of the currently running component.
	 */
	public static IMjExecutionFeature	get()
	{
		IMjExecutionFeature	ret	= MjExecutionFeature.LOCAL.get();
		if(ret==null)
		{
			throw new IllegalCallerException("Not running inside any component.");
		}
		return ret;
	}
	
	/**
	 *  Get the feature instance of the given component.
	 */
	public static IMjExternalExecutionFeature	getExternal(MjComponent self)
	{
		return self.getFeature(IMjExecutionFeature.class);
	}
	
	/**
	 *  Get the component to which this feature belongs. 
	 */
	public MjComponent	getComponent();
	
	/**
	 *  Get the current time.
	 *  @return	The time in milliseconds.
	 */
	public long	getTime();
}
