package jadex.bt.cleanerworld.gui.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Set;

import javax.swing.JPanel;

import jadex.bt.cleanerworld.BTCleanerAgent;
import jadex.bt.cleanerworld.environment.Chargingstation;
import jadex.bt.cleanerworld.environment.Cleaner;
import jadex.bt.cleanerworld.environment.Waste;
import jadex.bt.cleanerworld.environment.Wastebin;
import jadex.core.IComponentHandle;
import jadex.math.IVector2;

/**
 *  Panel for showing the cleaner world view as provided by sensor.
 */
class SensorPanel extends JPanel
{
	//-------- attributes --------
	
	/** The agent. */
	private IComponentHandle handle;
	
	//-------- constructors --------

	/**
	 *  Create a sensor panel.
	 */
	public SensorPanel(IComponentHandle handle)
	{
		this.handle = handle;
	}
	
	//-------- JPanel methods --------

	/**
	 *  Paint the world view.
	 */
	protected void	paintComponent(Graphics g)
	{
//		try
//		{
			GuiData	data = fetchGuiData();
			
			// Paint background (dependent on daytime).
			Rectangle	bounds	= getBounds();
			g.setColor(data.daytime ? Color.lightGray : Color.darkGray);
			g.fillRect(0, 0, bounds.width, bounds.height);

			// Paint the known cleaners.
			for(Cleaner cleaner: data.cleaners)
			{
				// Paint agent.
				Point p	= onScreenLocation(cleaner.getLocation(), bounds);
				int w	= (int)(cleaner.getVisionRange()*bounds.width);
				int h	= (int)(cleaner.getVisionRange()*bounds.height);
				int colorcode	= Math.abs(cleaner.getId().toString().hashCode()%8);
				g.setColor(new Color((colorcode&1)!=0?255:100, (colorcode&2)!=0?255:100, (colorcode&4)!=0?255:100, 192));	// Vision range
				g.fillOval(p.x-w, p.y-h, w*2, h*2);
				g.setColor(new Color(50, 50, 50, 180));
				g.fillOval(p.x-3, p.y-3, 7, 7);
				g.drawString(cleaner.getId(),
					p.x+5, p.y-5);
				g.drawString("battery: " + (int)(cleaner.getChargestate()*100.0) + "%",
					p.x+5, p.y+5);
				g.drawString("waste: " + (cleaner.getCarriedWaste()!=null ? "yes" : "no"),
					p.x+5, p.y+15);
			}

			// Paint agent.
			Point	p	= onScreenLocation(data.self.getLocation(), bounds);
			int w	= (int)(data.self.getVisionRange()*bounds.width);
			int h	= (int)(data.self.getVisionRange()*bounds.height);
			int colorcode	= Math.abs(data.self.getId().toString().hashCode()%8);
			g.setColor(new Color((colorcode&1)!=0?255:100, (colorcode&2)!=0?255:100, (colorcode&4)!=0?255:100, 192));	// Vision range
			g.fillOval(p.x-w, p.y-h, w*2, h*2);
			g.setColor(Color.black);	// Agent
			g.fillOval(p.x-3, p.y-3, 7, 7);
			g.drawString(data.self.getId().toString(),
				p.x+5, p.y-5);
			g.drawString("battery: " + (int)(data.self.getChargestate()*100.0) + "%",
				p.x+5, p.y+5);
			g.drawString("waste: " + (data.self.getCarriedWaste()!=null ? "yes" : "no"),
				p.x+5, p.y+15);
			
//			// Paint pheromones.
//			for(IPheromone pheromone: data.pheromones)
//			{
//				colorcode	= Math.abs(pheromone.getType().hashCode()%8);
//				g.setColor(new Color((colorcode&1)!=0?192:0, (colorcode&2)!=0?192:0, (colorcode&4)!=0?192:0, (int)(192*pheromone.getStrength())));
//				p	= onScreenLocation(pheromone.getLocation(), bounds);
//				int size	= (int)(pheromone.getStrength()*7);
//				g.fillOval(p.x-size, p.y-size, size*2+1, size*2+1);
//			}

			// Paint charge stations.
			for(Chargingstation station: data.stations)
			{
				g.setColor(Color.blue);
				p = onScreenLocation(station.getLocation(), bounds);
				g.drawRect(p.x-10, p.y-10, 20, 20);
				g.setColor(data.daytime ? Color.black : Color.white);
				g.drawString(station.getId(), p.x+14, p.y+5);
			}

			// Paint waste bins.
			for(Wastebin bin: data.wastebins)
			{
				g.setColor(Color.red);
				p = onScreenLocation(bin.getLocation(), bounds);
				g.drawOval(p.x-10, p.y-10, 20, 20);
				g.setColor(data.daytime ? Color.black : Color.white);
				g.drawString(bin.getId()+" ("+bin.getWastes().length+"/"+bin.getCapacity()+")", p.x+14, p.y+5);
			}

			// Paint waste.
			for(Waste waste: data.wastes)
			{
				if(waste.getLocation()==null) // is waste carried by cleaner?
					continue;
				g.setColor(Color.red);
				p	= onScreenLocation(waste.getLocation(), bounds);
				g.fillOval(p.x-3, p.y-3, 7, 7);
			}

			// Paint movement target.
			if(data.target!=null)
			{
				g.setColor(Color.black);
				p = onScreenLocation(data.target, bounds);
				g.drawOval(p.x-5, p.y-5, 10, 10);
				g.drawLine(p.x-7, p.y, p.x+7, p.y);
				g.drawLine(p.x, p.y-7, p.x, p.y+7);
			}
//		}
//		catch(ComponentTerminatedException e)
//		{	
//		}
	}
	
	//-------- helper methods --------

	/**
	 *  Get the on screen location for a location in  the world.
	 */
	private static Point onScreenLocation(IVector2 loc, Rectangle bounds)
	{
		assert loc!=null;
		assert bounds!=null;
		return new Point((int)(bounds.width*loc.getXAsDouble()),
			(int)(bounds.height*(1.0-loc.getYAsDouble())));
	}
	
	/**
	 *  Fetch gui data from agent.
	 */
	private GuiData	fetchGuiData()
	{
		// Read sensor data on agent thread to avoid inconsistencies/conflicts.
		
		BTCleanerAgent agent = handle.getPojoHandle(BTCleanerAgent.class);
		
		GuiData	ret	= new GuiData();
		ret.self = agent.getCleaner().get();
		ret.target	= agent.getTarget().get();
		ret.daytime = agent.isDaytime().get();
		ret.wastes = agent.getWastes().get();
		ret.cleaners = agent.getCleaners().get();
		ret.stations = agent.getStations().get();
		ret.wastebins = agent.getWastebins().get();
		
		//System.out.println("guidata: "+ret);
		
		return ret;
	}
	
	/**
	 *  Data transfer object from agent thread to gui thread.
	 */
	private static class GuiData
	{
		/** The cleaner. */
		public Cleaner self;
		
		/** The current movement target, if any. */
		public IVector2 target;
		
		/** The daytime flag. */
		public boolean daytime;
		
		/** The known other cleaners. */
		public Set<Cleaner> cleaners;
		
		/** The known waste pieces. */
		public Set<Waste> wastes;
		
		/** The known charging stations. */
		public Set<Chargingstation> stations;
		
		/** The known waste bins. */
		public Set<Wastebin> wastebins;

		@Override
		public String toString() 
		{
			return "GuiData [self=" + self + ", target=" + target + ", daytime=" + daytime + ", cleaners=" + cleaners
					+ ", wastes=" + wastes + ", stations=" + stations + ", wastebins=" + wastebins + "]";
		}
	}
}
