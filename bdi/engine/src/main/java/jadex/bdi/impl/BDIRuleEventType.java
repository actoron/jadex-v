package jadex.bdi.impl;

/**
 *  Event types that are used in BDI rule system.
 */
public class BDIRuleEventType
{
	/** Event type that a fact has been added. */
	public static final String FACTADDED = "factadded";
	
	/** Event type that a fact has been removed. */
	public static final String FACTREMOVED = "factremoved";

	/** Event type that a fact has changed (property change in case of bean). */
	public static final String FACTCHANGED = "factchanged";

//	/** Event type that a belief value has changed (the whole value was changed). */
//	public static final String BELIEFCHANGED = "beliefchanged";
	
	
	/** Event type that a value has been added. */
	public static final String VALUEADDED = "valueadded";
	
	/** Event type that a value has been removed. */
	public static final String VALUEREMOVED = "valueremoved";

	/** Event type that a value has changed (property change in case of bean). */
	public static final String VALUECHANGED = "valuechanged";

//	/** Event type that a parameter value has changed (the whole value was changed). */
//	public static final String PARAMETERCHANGED = "parameterchanged";
//
//	
	/** Event type that a goal has been added. */
	public static final String GOALADOPTED = "goaladopted";
	
	/** Event type that a goal has been removed. */
	public static final String GOALDROPPED = "goaldropped";

	
//	/** Event type that a goal has been added. */
//	public static final String GOALACTIVE = "goaladopted"; // goaladopted?! or goalactive
	
//	/** Event type that a goal has been optionized. */
//	public static final String GOALOPTION = "goaloption";
	
//	/** Event type that a goal has been suspended. */
//	public static final String GOALSUSPENDED = "goalsuspended";

////	/** Event type that a goal has been suspended. */
////	public static final String GOALACTIVE = "goalactive";
//	

//	/** Event type that a goal has been added. */
//	public static final String GOALINPROCESS = "goalinprocess";
	
//	/** Event type that a goal has been removed. */
//	public static final String GOALNOTINPROCESS = "goalnotinprocess";

////	/** Event type that a goal has been added. */
////	public static final String GOALINHIBITED = "goalinhibited";
////
////	/** Event type that a goal has been added. */
////	public static final String GOALNOTINHIBITED = "goalnotinhibited";
//	
//	
//	/** Event type that a plan has been added. */
//	public static final String PLANADOPTED = "planadopted";
//	
//	/** Event type that a plan has been finished. */
//	public static final String PLANFINISHED = "planfinished";
}
