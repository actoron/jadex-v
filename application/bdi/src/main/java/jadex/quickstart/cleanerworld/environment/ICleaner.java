package jadex.quickstart.cleanerworld.environment;

import java.beans.PropertyChangeListener;

/**
 *  Cleaner object represents knowledge about a cleaner robot.
 */
public interface ICleaner	extends ILocationObject
{
	/**
	 *  Get the id (or name) of this object.
	 *  @return The id.
	 */
	public String getId();

	/**
	 *  Get the location of this object.
	 * @return The location of the object.
	 */
	public ILocation getLocation();

	/**
	 *  Get the chargestate of this Cleaner.
	 * @return Charge state of the battery (0.0-1.0).
	 */
	public double getChargestate();
	
	/**
	 *  Get the carried-waste of this Cleaner.
	 * @return The carried waste, if any.
	 */
	public IWaste getCarriedWaste();

	/**
	 *  Get the vision-range of this Cleaner.
	 * @return The distance that this cleaner is able to see.
	 */
	public double getVisionRange();

//	/**
//	 *  Get the identifier (address) of the cleaner
//	 *  @return The identifier that can be used to send a message to the cleaner.
//	 */
//	public IComponentIdentifier getAgentIdentifier();
	
	/**
	 * Add a PropertyChangeListener to the listener list.
	 * The listener is registered for all properties.
	 *
	 * @param listener The PropertyChangeListener to be added.
	 */
	// TODO: Hack!!! Needed for sanity check in BDI feature, that belief type is supported
	public void addPropertyChangeListener(PropertyChangeListener listener);
}
