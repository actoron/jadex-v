package jadex.bdi.cleanerworld.environment;

import java.util.ArrayList;
import java.util.List;

import jadex.environment.SpaceObject;
import jadex.math.IVector2;
import jadex.math.Vector2Double;


public class Wastebin extends SpaceObject
{
	private List<Waste> wastes;
	
	private int capacity;
	
	public Wastebin(IVector2 position, int capacity)
	{
		super(position);
		this.capacity = capacity;
		this.wastes = new ArrayList<>();
	}
	
	public IVector2 getLocation()
	{
		return getPosition();
	}
	
	/**
	 *  Get the wastes of this Wastebin.
	 * @return wastes
	 */
	public Waste[] getWastes()
	{
		return (Waste[])wastes.toArray(new Waste[wastes.size()]);
	}

	/**
	 *  Set the wastes of this Wastebin.
	 * @param wastes the value to be set
	 */
	public void setWastes(Waste[] wastes)
	{
		this.wastes.clear();
		for(int i = 0; i < wastes.length; i++)
			this.wastes.add(wastes[i]);
		//getPropertyChangeHandler().firePropertyChange("wastes", null, wastes);
	}

	/**
	 *  Get an wastes of this Wastebin.
	 *  @param idx The index.
	 *  @return wastes
	 */
	public Waste getWaste(int idx)
	{
		return (Waste)this.wastes.get(idx);
	}

	/**
	 *  Set a waste to this Wastebin.
	 *  @param idx The index.
	 *  @param waste a value to be added
	 */
	public void setWaste(int idx, Waste waste)
	{
		this.wastes.set(idx, waste);
		//getPropertyChangeHandler().firePropertyChange("wastes", null, wastes);
	}

	/**
	 *  Add a waste to this Wastebin.
	 *  @param waste a value to be removed
	 */
	public void addWaste(Waste waste)
	{
		this.wastes.add(waste);
		//System.out.println("added waste: "+waste+" "+this);
		//getPropertyChangeHandler().firePropertyChange("wastes", null, wastes);
	}

	/**
	 *  Remove a waste from this Wastebin.
	 *  @param waste a value to be removed
	 *  @return  True when the wastes have changed.
	 */
	public boolean removeWaste(Waste waste)
	{
		boolean ret = this.wastes.remove(waste);
		//if(ret)
		//	getPropertyChangeHandler().firePropertyChange("wastes", null, wastes);
		return ret;
	}

	/**
	 *  Get the capacity of this Wastebin.
	 * @return The maximum number of waste objects to fit in this waste bin.
	 */
	public int getCapacity()
	{
		return this.capacity;
	}

	/**
	 *  Set the capacity of this Wastebin.
	 * @param capacity the value to be set
	 */
	public void setCapacity(int capacity)
	{
		int oldc = this.capacity;
		this.capacity = capacity;
		//getPropertyChangeHandler().firePropertyChange("capacity", oldc, capacity);
	}

	//-------- custom code --------

	/**
	 *  Test is the wastebin is full.
	 *  @return True, when wastebin is full.
	 */
	public boolean isFull()
	{
		return wastes.size() >= capacity;
	}

	/**
	 *  Empty the waste bin.
	 */
	public void empty()
	{
		wastes.clear();
	}

	public void fill()
	{
		// Fill with imaginary waste ;-)
		while(!isFull())
			wastes.add(new Waste(new Vector2Double(-1, -1)));
	}

	public boolean contains(Waste waste)
	{
		return wastes.contains(waste);
	}
	
	public Wastebin copy()
	{
		Wastebin ret = new Wastebin(this.getPosition(), this.getCapacity());
		ret.setId(this.getId());
		for(Waste waste: wastes)
			ret.addWaste(waste.copy());
		return ret;
	}
	
	public void onUpdateFrom(SpaceObject source)
	{
		Wastebin wb = (Wastebin)source;
		setPosition(wb.getPosition());
		setCapacity(wb.getCapacity());
		wastes.clear();
		for(Waste waste: wb.getWastes())
			addWaste(waste.copy());
	}

	@Override
	public String toString() 
	{
		return "Wastebin [wastes=" + wastes + ", capacity=" + capacity + ", position=" + position + ", id=" + id + "]";
	}
	
	
}

