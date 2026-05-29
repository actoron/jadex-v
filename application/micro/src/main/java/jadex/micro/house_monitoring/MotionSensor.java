package jadex.micro.house_monitoring;

import jadex.common.SUtil;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.micro.llmcall2.IRuleSystemService;
import jadex.micro.llmcall2.IRuleSystemService.EventType;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  This class represents a motion sensor in the smart home. 
 */
public class MotionSensor	implements IMotionSensorService
{
	//-------- static part --------
	
	/** The motion detected event type. */
	public static final String	EVENT_TYPE_MOTION_DETECTED = "motion_detected";
	
	//-------- attributes --------
	
	/** The component. */
	@Inject
	protected IComponent	comp;
	
	//-------- initialization --------
	
	/**
	 *  Add the supported event type to the rule system when the component starts.
	 */
	@Inject
	protected void ruleSystemAdded(IRuleSystemService ruleSystem)
	{
		ruleSystem.registerEventType(
			new EventType(EVENT_TYPE_MOTION_DETECTED, "A motion is detected by the sensor (e.g. an animal or person).")
				, IMotionSensorService.class)
			.catchEx(ex -> SUtil.throwUnchecked(ex));
	}
	
	//-------- IMotionSensorService interface --------
	
	@Override
	public IFuture<Void> motionDetected()
	{
		return comp.getFeature(IRequiredServiceFeature.class).searchService(IRuleSystemService.class)
			.thenCompose(rulesystem -> rulesystem.notifyEvent(EVENT_TYPE_MOTION_DETECTED, null));
	}
}
