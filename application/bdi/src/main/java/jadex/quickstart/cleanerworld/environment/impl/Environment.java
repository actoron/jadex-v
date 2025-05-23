package jadex.quickstart.cleanerworld.environment.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jadex.core.ComponentIdentifier;
import jadex.core.IComponent;
import jadex.quickstart.cleanerworld.environment.ILocationObject;

/**
 *  The environment object for non distributed applications.
 */
public class Environment
{
	//-------- class attributes --------

	/** The singleton. */
	private static Environment instance;

	//-------- attributes --------

	/** The daytime. */
	private boolean daytime;

	/** The cleaners. */
	private Map<String, Cleaner> cleaners;

	/** The wastes. */
	private List<Waste> wastes;

	/** The waste bins. */
	private List<Wastebin> wastebins;

	/** The charging stations. */
	private List<Chargingstation> chargingstations;

	//-------- constructors --------

	/**
	 *  Create a new environment.
	 */
	private Environment()
	{
		this.daytime = true;
		this.cleaners = new LinkedHashMap<String, Cleaner>();
		this.wastes = new ArrayList<Waste>();
		this.wastebins = new ArrayList<Wastebin>();
		this.chargingstations = new ArrayList<Chargingstation>();

		// Add some things to our world.
		addWaste(new Waste(new Location(0.1, 0.5)));
		addWaste(new Waste(new Location(0.2, 0.5)));
		addWaste(new Waste(new Location(0.3, 0.5)));
		addWaste(new Waste(new Location(0.9, 0.9)));
		addWastebin(new Wastebin(new Location(0.2, 0.2), 20));
		addWastebin(new Wastebin(new Location(0.8, 0.1), 20));
		addChargingStation(new Chargingstation(new Location(0.775, 0.775)));
		addChargingStation(new Chargingstation(new Location(0.15, 0.4)));
	}

	/**
	 *  Get the singleton.
	 *  @return The environment.
	 */
	public static synchronized Environment getInstance()
	{
		if(instance==null)
		{
			instance = new Environment();
		}
		return instance;
	}
	
	//-------- cleaner handling --------
	
	/**
	 *  Creates a new cleaner object.
	 */
	public Cleaner	createCleaner(IComponent agent, Cleaner self)
	{
		ComponentIdentifier	cid	= agent.getId();
		Cleaner	ret;
		boolean	create;
		synchronized(this)
		{
			ret	= cleaners.get(cid.getLocalName());
			create	= ret==null;
			if(create)
			{
				if(self!=null)
				{
					ret	= self;
					ret.setId(cid.getLocalName());
					ret.setLocation(new Location(Math.random()*0.4+0.3, Math.random()*0.4+0.3));
					ret.setCid(cid);
					ret.setVisionRange(0.1);
					ret.setChargestate(0.8);
				}
				else
				{
					ret	= new Cleaner(cid.getLocalName(), new Location(Math.random()*0.4+0.3, Math.random()*0.4+0.3), cid, null, 0.1, 0.8);
				}
				cleaners.put(cid.getLocalName(), ret);
			}
		}
		
		if(create)
		{
			// TODO: get finish future in lifecycle feature?
//			// Remove on agent kill.
//			SComponentManagementService.listenToComponent(cid, agent)
//				.addResultListener(new IntermediateEmptyResultListener<CMSStatusEvent>()
//			{
//				@Override
//				public void intermediateResultAvailable(CMSStatusEvent cse)
//				{
//					if(cse instanceof CMSTerminatedEvent)
//					{
//						synchronized(Environment.this)
//						{
//							cleaners.remove(cid);
//						}
//					}
//				}
//			});
		}
		else
		{
			throw new IllegalStateException("Cleaner for agent "+cid+" already exists (duplicate actsense?).");
		}
		
		return ret;
	}
	
	//-------- methods --------

	/**
	 *  Get the daytime.
	 *  @return The current vision.
	 */
	public synchronized boolean getDaytime()
	{
		return daytime;
	}

	/**
	 *  Set the daytime.
	 *  @param daytime The daytime.
	 */
	public synchronized void setDaytime(boolean daytime)
	{
		this.daytime = daytime;
	}

	/**
	 *  Add a piece of waste.
	 *  @param waste The new piece of waste.
	 */
	public synchronized void addWaste(Waste waste)
	{
		wastes.add(waste.clone());
	}

	/**
	 *  Remove a piece of waste.
	 *  @param waste The piece of waste.
	 */
	public synchronized boolean removeWaste(Waste waste)
	{
		boolean	ret	= wastes.remove(waste);
		return ret;
	}

	/**
	 *  Add a wastebin.
	 *  @param wastebin The new waste bin.
	 */
	public synchronized void addWastebin(Wastebin wastebin)
	{
		wastebins.add(wastebin.clone());
	}

	/**
	 *  Add a charging station.
	 *  @param station The new charging station.
	 */
	public synchronized void addChargingStation(Chargingstation station)
	{
		chargingstations.add(station.clone());
	}

	/**
	 *  Get all wastes.
	 *  @return All wastes.
	 */
	public synchronized Waste[] getWastes()
	{
		return cloneList(wastes, Waste.class);
	}

	/**
	 *  Get all wastebins.
	 *  @return All wastebins.
	 */
	public synchronized Wastebin[] getWastebins()
	{
		return cloneList(wastebins, Wastebin.class);
	}

	/**
	 *  Get all charging stations.
	 *  @return All stations.
	 */
	public synchronized Chargingstation[] getChargingstations()
	{
		return cloneList(chargingstations, Chargingstation.class);
	}

	/**
	 *  Get all cleaners.
	 *  @return All cleaners.
	 */
	public synchronized Cleaner[] getCleaners()
	{
		return cloneList(cleaners.values(), Cleaner.class);
	}

	/**
	 *  Get a cleaner for a component opr null.
	 */
	public synchronized Cleaner getCleaner(String id)
	{
		return cleaners.get(id);
	}

	/**
	 *  Update a cleaner.
	 */
	public synchronized void	updateCleaner(Cleaner cleaner)
	{
		cleaners.put(cleaner.getId(), cleaner.clone());
	}
	
	/**
	 *  Let a cleaner pick up waste.
	 */
	public synchronized void	pickupWaste(Cleaner cleaner, Waste waste)
	{
		Cleaner	mycleaner	= cleaners.get(cleaner.getId());
		
		if(mycleaner.getCarriedWaste()!=null)
		{
			throw new RuntimeException("Cleaner already carries waste: "+waste);
		}
		
		// Find global copy of waste object.
		Waste mywaste	= null;
		for(Waste w: wastes)
		{
			if(w.equals(waste))
			{
				mywaste	= w;
			}
		}
		
		if(mywaste==null)
		{
			throw new RuntimeException("No such waste: "+waste);
		}
		
		if(mycleaner.getLocation().isNear(waste.getLocation()))
		{
			// Update global objects
			mywaste.setLocation(null);
			mycleaner.setCarriedWaste(mywaste);
			wastes.remove(waste);
		}
		else
		{
			throw new RuntimeException("Cleaner not in pickup range: "+mycleaner+", "+mywaste);
		}
	}
	

	/**
	 *  Drop a piece of waste.
	 */
	public synchronized void	dropWasteInWastebin(Cleaner cleaner, Waste waste, Wastebin wastebin)
	{
		Cleaner	mycleaner	= cleaners.get(cleaner.getId());
		
		if(mycleaner.getCarriedWaste()==null || !mycleaner.getCarriedWaste().equals(waste))
		{
			throw new RuntimeException("Cleaner does not carry the waste: "+cleaner+", "+waste);
		}
		
		// Find global copy of wastenbin object.
		Wastebin mywastebin	= null;
		for(Wastebin wb: wastebins)
		{
			if(wb.equals(wastebin))
			{
				mywastebin	= wb;
			}
		}
		
		if(mywastebin==null)
		{
			throw new RuntimeException("No such waste bin: "+wastebin);
		}
		
		if(mycleaner.getLocation().isNear(wastebin.getLocation()))
		{
			// Update local and global objects
			mywastebin.addWaste(waste.clone());
			mycleaner.setCarriedWaste(null);
		}
		else
		{
			throw new RuntimeException("Cleaner not in drop range: "+mycleaner+", "+mywastebin);
		}
	}


	/**
	 *  Get a wastebin for a name.
	 *  @return The wastebin.
	 */
	public synchronized Wastebin getWastebin(String name)
	{
		Wastebin ret = null;
		for(Wastebin wb : wastebins)
		{
			if(wb.getId().equals(name))
			{
				ret = wb;//.clone();	// No clone as only used from gui and needs to change original object.
				break;
			}
		}
		return ret;
	}
	
	/**
	 *  Deep clone a list of objects.
	 */
	public static <T extends ILocationObject> T[]	cloneList(Collection<T> list, Class<T> type)
	{
		List<ILocationObject>	ret	= new ArrayList<>();
		for(ILocationObject o: list)
		{
			ret.add(((LocationObject)o).clone());
		}
		@SuppressWarnings("unchecked")
		T[]	aret	= ret.toArray((T[])Array.newInstance(type, list.size()));
		return aret;
	}
}
