package jadex.quickstart.cleanerworld.environment;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import jadex.common.ErrorException;
import jadex.common.SUtil;
import jadex.execution.IExecutionFeature;
import jadex.future.Future;
import jadex.quickstart.cleanerworld.environment.impl.Cleaner;
import jadex.quickstart.cleanerworld.environment.impl.Environment;
import jadex.quickstart.cleanerworld.environment.impl.Location;
import jadex.quickstart.cleanerworld.environment.impl.LocationObject;
import jadex.quickstart.cleanerworld.environment.impl.Pheromone;
import jadex.quickstart.cleanerworld.environment.impl.Waste;
import jadex.quickstart.cleanerworld.environment.impl.Wastebin;

/**
 *  The sensor / actuator gives access to the perceived environment
 *  and provides operations to manipulate the environment.
 *  Each cleaner agent should create its own sensor/actuator.
 */
public class SensorActuator
{
	//-------- attributes --------
	
	/** The agent. Must be public due to Java Reflection Restrictions */
	public IExecutionFeature agent;
	
	/** The cleaner. */
	private Cleaner	self;
	
	/** The current movement target, if any. */
	private Location	target;
	
	/** The pheromone to disperse (if any). */
	private String	pheromone;
	
	/** The known other cleaners. */
	private Set<ICleaner>	cleaners	= new LinkedHashSet<>();
	
	/** The known waste pieces. */
	private Set<IWaste>	wastes	= new LinkedHashSet<>();
	
	/** The known charging stations. */
	private Set<IChargingstation>	chargingstations	= new LinkedHashSet<>();
	
	/** The known waste bins. */
	private Set<IWastebin>	wastebins	= new LinkedHashSet<>();
	
	/** Future allowing to wait for recharging. */
	private Future<Void>	recharging;
	
	//-------- sensor methods --------
	
	/**
	 *  Get the knowledge about the cleaner itself.
	 *  @return The cleaner object.
	 */
	public ICleaner getSelf()
	{
		lazyInit();

		return self;
	}

	/**
	 *  Check, if it is at day or at night.
	 *  @return true, if at day.
	 */
	public boolean isDaytime()
	{
		return Environment.getInstance().getDaytime();
	}
	
	/**
	 *  Get the known other cleaners.
	 *  @return a Set of Cleaner objects. 
	 */
	public Set<ICleaner>	getCleaners()
	{
		lazyInit();
		return cleaners;
	}
	
	/**
	 *  Get the known waste pieces.
	 *  @return a Set of Waste objects. 
	 */
	public Set<IWaste>	getWastes()
	{
		lazyInit();
		return wastes;
	}
		
	/**
	 *  Get the known charging stations.
	 *  @return a Set of Chargingstation objects. 
	 */
	public Set<IChargingstation>	getChargingstations()
	{
		lazyInit();
		return chargingstations;
	}
		
	/**
	 *  Get the known waste pieces.
	 *  @return a Set of Waste objects. 
	 */
	public Set<IWastebin>	getWastebins()
	{
		lazyInit();
		return wastebins;
	}
		
//	/**
//	 *  Get the currently perceived pheromones.
//	 *  @return a Set of Pheromone objects. 
//	 */
//	public Set<IPheromone>	getPheromones()
//	{
//		lazyInit();
//		
//		Set<IPheromone>	ret	= new LinkedHashSet<>(Arrays.asList(Environment.getInstance().getPheromones()));
//		for(Iterator<IPheromone> phi= ret.iterator(); phi.hasNext(); )
//		{
//			IPheromone	ph	= phi.next();
//			if(ph.getLocation().getDistance(self.getLocation())>self.getVisionRange())
//			{
//				phi.remove();
//			}
//		}
//		
//		return ret;
//	}
	
	/**
	 *  Use the provided set to manage the known waste objects.
	 *  Allows using custom data structures such as BDI belief sets directly.
	 */
	public void	manageWastesIn(Set<IWaste> wastes)
	{
		lazyInit();
		wastes.addAll(this.wastes);
		this.wastes	= wastes;
	}
		
	/**
	 *  Use the provided set to manage the known waste bin objects.
	 *  Allows using custom data structures such as BDI belief sets directly.
	 */
	public void	manageWastebinsIn(Set<IWastebin> wastebins)
	{
		lazyInit();
		wastebins.addAll(this.wastebins);
		this.wastebins	= wastebins;
	}
		
	/**
	 *  Use the provided set to manage the known charging station objects.
	 *  Allows using custom data structures such as BDI belief sets directly.
	 */
	public void	manageChargingstationsIn(Set<IChargingstation> chargingstations)
	{
		lazyInit();
		chargingstations.addAll(this.chargingstations);
		this.chargingstations	= chargingstations;
	}
	
	/**
	 *  Use the provided set to manage the known cleaner objects.
	 *  Allows using custom data structures such as BDI belief sets directly.
	 */
	public void	manageCleanersIn(Set<ICleaner> cleaners)
	{
		lazyInit();
		cleaners.addAll(this.cleaners);
		this.cleaners	= cleaners;
	}
		
	//-------- actuator methods --------
	
	/**
	 *  Move to the given location.
	 *  Blocks until the location is reached or a failure occurs.
	 *  @param location	The location.
	 */
	public void moveTo(ILocation location)
	{
		moveTo(location.getX(), location.getY());
	}
	
	long	lasttime;
	
	/**
	 *  Move to the given location.
	 *  Blocks until the location is reached or a failure occurs.
	 *  @param x	X coordinate.
	 *  @param y	Y coordinate.
	 */
	public void moveTo(double x, double y)
	{
		lazyInit();
		if(target!=null)
		{
			throw new IllegalStateException("Cannot move to multiple targets simultaneously. Target exists: "+target);
		}
		
		// When out of battery -> block until battery is recharged a bit.
		if(self.getChargestate()<=0)
		{
			if(recharging==null)
			{
				recharging	= new Future<Void>();
			}
			System.out.println("moveTo() called with empty battery -> blocking until recharged.");
			recharging.get();
		}
		
		this.target	= new Location(x, y);
		
		if(self.getLocation().isNear(target))
		{
			this.target	= null;
			return;
		}
		
		// Signal variable to check when location is reached.
		final Future<Void>	reached	= new Future<>();
		
		// Schedule an update step periodically.
		lasttime	= agent.getTime();
		@SuppressWarnings("unchecked")
		Callable<Void>[]	step	= new Callable[1];
		step[0]= () ->
		{
			if(!reached.isDone())	// no new timer when future is terminated from outside (e.g. agent killed)
			{
				long	currenttime	= agent.getTime();
				// Calculate time passed as fraction of a second.
				double	delta	= (currenttime-lasttime)/1000.0;
				lasttime	= currenttime;
				
				// Set new charge state
				double	chargestate	= self.getChargestate()-delta/100; 	// drop ~ 1%/sec while moving.
				if(chargestate<0)
				{
					self.setChargestate(0);
					throw new IllegalStateException("Ran out of battery during moveTo() -> target location not reached!");
				}
				self.setChargestate(chargestate);
				
				// Set new location
				double total_dist	= self.getLocation().getDistance(target);
				double move_dist	= Math.min(total_dist, 0.1*delta);	// speed ~ 0.1 units/sec 
				double dx = (target.getX()-self.getLocation().getX())*move_dist/total_dist;
				double dy = (target.getY()-self.getLocation().getY())*move_dist/total_dist;
				self.setLocation(new Location(self.getLocation().getX()+dx, self.getLocation().getY()+dy));
				
				// Post new own state to environment
				Environment.getInstance().updateCleaner(self);
				
				// Add pheromone (if any).
				if(pheromone!=null)
				{
					Pheromone	ph	= new Pheromone(self.getLocation(), pheromone);
					Environment.getInstance().addPheromone(ph);
				}

				// Get new external state from environment.
				update();
				
				// Finish or repeat?
				if(self.getLocation().isNear(target))
				{
					// Release block.
					reached.setResultIfUndone(null);
				}
				else
				{
					agent.waitForDelay(33).then(v ->
						agent.scheduleStep(step[0]).catchEx(exception -> reached.setExceptionIfUndone(exception)));
				}
			}
			return null;
		};
		agent.scheduleStep(step[0]).catchEx(exception -> reached.setExceptionIfUndone(exception));
		
		try
		{
			reached.get();	// Block agent/plan until location is reached.
		}
		catch(Throwable t)
		{
//			if(t instanceof Exception)
//				t.printStackTrace();
			// Move interrupted -> set exception to abort move steps.
			reached.setExceptionIfUndone(t instanceof Exception ? (Exception)t : new ErrorException((Error)t));
			SUtil.throwUnchecked(t);
		}
		finally
		{
			// After move finished/failed always reset state.
			target	= null;
			pheromone	= null;
		}
	}
	
	
	
	/**
	 *  Recharge a cleaner at a charging station to a desired charging level.
	 *  The cleaner needs to be at the location of the charging station
	 *  Note, the charging rate gets slower over 70% charge state.
	 *  @param chargingstation The charging station to recharge at.
	 *  @param level	The desired charging level between 0 and 1.
	 */
	public synchronized void	recharge(IChargingstation chargingstation, double level)
	{
		lazyInit();
		
		// Signal variable to check when level is reached.
		final Future<Void>	reached	= new Future<>();
		
		lasttime	= agent.getTime();
		@SuppressWarnings("unchecked")
		Callable<Void>[]	step	= new Callable[1];
		step[0]	= () ->
		{
			if(!reached.isDone())	// no new timer when future is terminated from outside (e.g. agent killed)
			{
				// Check the location.
				if(!self.getLocation().isNear(chargingstation.getLocation()))
				{
					throw new IllegalStateException("Cannot not recharge. Charging station out of reach!");
				}
				
				// Calculate time passed as fraction of a second.
				long currenttime	= agent.getTime();
				double	delta	= (currenttime-lasttime)/1000.0;
				lasttime	= currenttime;
				
				// Set new charge state
				double	inc	= delta/10; 	// increase ~ 10%/sec while recharging.
				if(self.getChargestate()>0.7)	// when >70% linearily decrease charging rate to 0 at 100%.
				{
					inc	= inc * 10/3.0 * (1-self.getChargestate());
				}
				self.setChargestate(self.getChargestate()+inc);
				
				// Restart blocked moveTo()s, if any.
				Future<Void>	rec	= recharging;
				recharging	= null;
				if(rec!=null)
				{
					rec.setResult(null);
				}
				
				// Post new own state to environment
				Environment.getInstance().updateCleaner(self);
				
				// Finish or repeat?
				if(self.getChargestate()>=level)
				{
					// Release block.
					reached.setResultIfUndone(null);
				}
				else
				{
					agent.waitForDelay(33).then(v ->
						agent.scheduleStep(step[0]).catchEx(exception -> reached.setExceptionIfUndone(exception)));
				}
			}
			return null;
		};
		agent.scheduleStep(step[0]).catchEx(exception -> reached.setExceptionIfUndone(exception));
		
		try
		{
			reached.get();	// Block agent/plan until level is reached.
		}
		catch(Throwable t)
		{
			// Move interrupted -> set exception to abort recharge steps.
			reached.setExceptionIfUndone(t instanceof Exception ? (Exception)t : new ErrorException((Error)t));
			SUtil.throwUnchecked(t);
		}
	}

	/**
	 *  Get the current movement target, if any.
	 *  @return	The target or null when no current target.
	 */
	public ILocation getTarget()
	{
		lazyInit();
		return target;
	}
	
	/**
	 *  Try to pick up some piece of waste.
	 *  @param waste The waste.
	 */
	public void	pickUpWaste(IWaste waste)
	{
		lazyInit();
		
		// Try action in environment
		Environment.getInstance().pickupWaste(self, (Waste)waste);
		
		// Update local knowledge (order is important for goal conditions! TODO atomic?)
		self.setCarriedWaste((Waste)waste);
		wastes.remove(waste);
		((Waste)waste).setLocation(null);
	}

	/**
	 *  Drop a piece of waste.
	 */
	public void dropWasteInWastebin(IWaste waste, IWastebin wastebin)
	{
		lazyInit();
		
		// Try action in environment
		Environment.getInstance().dropWasteInWastebin(self, (Waste)waste, (Wastebin)wastebin);
		
		// Update local knowledge
		self.setCarriedWaste(null);
		((Wastebin)wastebin).addWaste(waste);
	}
	
//	/**
//	 *  Disperse pheromones when moving.
//	 *  The dispersion happens during the current/next movoTo() operation and stops automatically afterwards.
//	 *  @param type	The pheromone type (can be an arbitrary string).
//	 */
//	public void	dispersePheromones(String type)
//	{
//		lazyInit();
//		this.pheromone	= type;
//	}
	
	//-------- internal methods --------
	
	void lazyInit()
	{
		if(agent!=null &&!agent.isComponentThread())
		{
			throw new IllegalStateException("Error: Must be called on agent thread.");
		}
		
		// lazy init to allow pure BDI pojo creation before component exists
		if(agent==null)
		{
			IExecutionFeature	local	= IExecutionFeature.isAnyComponentThread() ? IExecutionFeature.get() : null;
			// Not running on a component or created from a different cleaner
			if(local==null
				|| Environment.getInstance().getCleaner(local.getComponent().getId().getLocalName())!=null)
			{
				// Create dummy cleaner to be filled later
				self	= new Cleaner();
			}
			
			// First call on own component thread
			else
			{
				agent	= local;
				self	= Environment.getInstance().createCleaner(agent.getComponent(), self);
			}
		}
	}
	
	/**
	 *  Update the sensor based on current vision.
	 *  Called from activities like moveTo().
	 */
	void	update()
	{
		updateObjects(cleaners, Environment.getInstance().getCleaners());
		updateObjects(wastes, Environment.getInstance().getWastes());
		updateObjects(chargingstations, Environment.getInstance().getChargingstations());
		updateObjects(wastebins, Environment.getInstance().getWastebins());
	}
	
	/**
	 *  Update a set of location objects with the current situation.
	 *  @param oldset	The old set of objects, i.e. the previous knowledge.
	 *  @param newset	The new set of objects, i.e. the current perception.
	 */
	<T extends ILocationObject> void updateObjects(Set<T> oldset, T[] newset)
	{
		Map<T, T>	newmap	= new LinkedHashMap<>();
		for(T o: newset)
		{
			// Special treatment for knowledge about myself.
			if(o.equals(self))
			{
				self.update((Cleaner)o);
//				System.out.println("updated: "+self);
			}
			
			// Store new object in map -> used to apply changes to existing knowledge below
			else
			{
				newmap.put(o, o);
			}
		}
		
		// Apply new perception to previous knowledge.
		for(LocationObject oldobj: oldset.toArray(new LocationObject[oldset.size()]))
		{
			LocationObject	newobj	= (LocationObject)newmap.remove(oldobj);			
			
			// When previous object location in vision range, but current object location (if any) not in range -> remove. 
			if(oldobj.getLocation().getDistance(self.getLocation())<=self.getVisionRange()
				&& (newobj==null || newobj.getLocation().getDistance(self.getLocation())>self.getVisionRange()))
			{
				oldset.remove(oldobj);
//				System.out.println("removed: "+oldobj);
			}
			
			// When new object in vision range -> update knowledge about object.
			if(newobj!=null && newobj.getLocation().getDistance(self.getLocation())<=self.getVisionRange())
			{
				oldobj.update(newobj);
//				System.out.println("updated: "+oldobj);
			}
		}
		
		// Add remaining new objects, when in vision range.
		for(T newobj: newmap.values())
		{
			if(newobj.getLocation().getDistance(self.getLocation())<=self.getVisionRange())
			{
				oldset.add(newobj);
//				System.out.println("added: "+newobj);
			}
		}
	}
}
