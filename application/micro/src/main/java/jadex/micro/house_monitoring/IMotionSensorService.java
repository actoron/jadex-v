package jadex.micro.house_monitoring;

import jadex.future.IFuture;
import jadex.providedservice.annotation.Service;

/**
 *  This service provides access to a motion sensor.
 */
@Service
public interface IMotionSensorService
{
	//-------- tool methods, i.e. visible to the LLM --------
	
	// n/a
	
	//-------- UI only methods --------
	
	/**
	 *  Simulate a motion detected event, e.g. by clicking a button in the UI.
	 */
	public IFuture<Void> motionDetected();
}
