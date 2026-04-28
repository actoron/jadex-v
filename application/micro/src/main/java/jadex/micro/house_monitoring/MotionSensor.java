package jadex.micro.house_monitoring;

import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.injection.annotation.Inject;
import jadex.micro.house_monitoring.IRuleSystemService.EventType;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  This class represents a motion sensor in the smart home. 
 */
public class MotionSensor	implements IMotionSensorService
{
	/** The component. */
	@Inject
	protected IComponent	comp;
	
	@Override
	public IFuture<Void> motionDetected()
	{
		return comp.getFeature(IRequiredServiceFeature.class).searchService(IRuleSystemService.class)
			.thenCompose(rulesystem -> rulesystem.notifyEvent(EventType.MOTION_DETECTED, ""));
	}
}
