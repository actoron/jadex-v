package jadex.bdi.annotation;

/** The exclude mode determines when and if a plan is removed from the applicable plans list (APL). */
public enum ExcludeMode
{
	/** The plan is never removed. */
	Never,

	/** The plan is removed after it has been executed once, regardless of success or failure or abortion. */
	WhenTried,
	
	/** The plan is removed after it has been executed once, but only when it exited with an exception. */
	WhenFailed,
	
	/** The plan is removed after it has been executed once, but only when it exited without an exception. */
	WhenSucceeded;
}