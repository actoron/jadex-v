package jadex.bt.cleanerworld.gui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jadex.common.SGUI;
import jadex.bt.cleanerworld.environment.ILocation;
import jadex.bt.cleanerworld.environment.IPheromone;
import jadex.bt.cleanerworld.environment.impl.Chargingstation;
import jadex.bt.cleanerworld.environment.impl.Cleaner;
import jadex.bt.cleanerworld.environment.impl.Environment;
import jadex.bt.cleanerworld.environment.impl.Location;
import jadex.bt.cleanerworld.environment.impl.Waste;
import jadex.bt.cleanerworld.environment.impl.Wastebin;


/**
 *  The gui for the cleaner world example.
 *  Shows the world from the viewpoint of the environment agent.
 */
public class EnvironmentGui	extends JFrame
{
	//-------- constants --------

	// auto generated by eclipse
	private static final long serialVersionUID = -1451077178653064172L;

	/** The image icons. */
	private static UIDefaults	icons	= new UIDefaults(new Object[]
	{
		"waste",	SGUI.makeIcon(EnvironmentGui.class, "/jadex/bt/cleanerworld/gui/images/waste.png"),
		"wastebin",	SGUI.makeIcon(EnvironmentGui.class, "/jadex/bt/cleanerworld/gui/images/wastebin.png"),
		"wastebin_full", SGUI.makeIcon(EnvironmentGui.class, "/jadex/bt/cleanerworld/gui/images/wastebin_full.png"),
		"chargingstation", SGUI.makeIcon(EnvironmentGui.class, "/jadex/bt/cleanerworld/gui/images/chargingstation.png"),
		"cleaner", SGUI.makeIcon(EnvironmentGui.class, "/jadex/bt/cleanerworld/gui/images/cleaner.png"),
		"background", SGUI.makeIcon(EnvironmentGui.class, "/jadex/bt/cleanerworld/gui/images/background.png"),
		"background_night", SGUI.makeIcon(EnvironmentGui.class, "/jadex/bt/cleanerworld/gui/images/background_night.png")
	});
	
	//-------- attributes --------
	
	/** The repaint timer. */
	private Timer	timer;

	//-------- constructors --------

	/**
	 *  Create a new gui.
	 */
	public EnvironmentGui()
	{
		super("Welcome to Cleaner World");
							
		final Environment env = Environment.getInstance();
				
		// Option panel.
		JPanel	options	= new JPanel(new GridBagLayout());
		options.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Environment Control"));
		final JCheckBox	daytime	= new JCheckBox("", env.getDaytime());
		daytime.setHorizontalTextPosition(SwingConstants.LEFT);
		final JLabel wastecnt = new JLabel(""+env.getWastes().length);

		final JComboBox<String> wastebinchoice = new JComboBox<String>();
		Wastebin[] wastebins = env.getWastebins();
		for(int i=0; i<wastebins.length; i++)
			wastebinchoice.addItem(wastebins[i].getId());
		final JComboBox< ? > fillstate = new JComboBox<String>(new String[]{"empty", "full"});
		JButton setfillstate = new JButton("Set fill-state");

		Insets insets = new Insets(2, 4, 4, 2);
		options.add(new JLabel("Toggle daytime"), new GridBagConstraints(0, 0, 1, 1, 0, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));
		options.add(daytime, new GridBagConstraints(1, 0, 3, 1, 1, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));
		options.add(new JLabel("Waste count:"), new GridBagConstraints(0, 1, 1, 1, 0, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));
		options.add(wastecnt, new GridBagConstraints(1, 1, 3, 1, 1, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));
		options.add(new JLabel("Wastebin:"), new GridBagConstraints(0, 2, 1, 1, 0, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));
		options.add(wastebinchoice, new GridBagConstraints(1, 2, 1, 1, 0, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));
		options.add(fillstate, new GridBagConstraints(2, 2, 1, 1, 0, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));
		options.add(setfillstate, new GridBagConstraints(3, 2, 1, 1, 1, 0,
			GridBagConstraints.WEST,  GridBagConstraints.NONE, insets, 0 , 0));

		final Image	waste_image	= ((ImageIcon)icons.getIcon("waste")).getImage();
		final Image	wastebin_image	= ((ImageIcon)icons.getIcon("wastebin")).getImage();
		final Image	wastebin_full_image	= ((ImageIcon)icons.getIcon("wastebin_full")).getImage();
		final Image	chargingstation_image	= ((ImageIcon)icons.getIcon("chargingstation")).getImage();
		final Image	cleaner_image	= ((ImageIcon)icons.getIcon("cleaner")).getImage();
		final Image	background_image	= ((ImageIcon)icons.getIcon("background")).getImage();
		final Image	background_night_image	= ((ImageIcon)icons.getIcon("background_night")).getImage();

		final JLabel waste	= new JLabel(new ImageIcon(waste_image), JLabel.CENTER);
		final JLabel wastebin	= new JLabel("dummy", new ImageIcon(wastebin_image), JLabel.CENTER);
		wastebin.setVerticalTextPosition(JLabel.BOTTOM);
		wastebin.setHorizontalTextPosition(JLabel.CENTER);
		final JLabel wastebin_full	= new JLabel("dummy", new ImageIcon(wastebin_full_image), JLabel.CENTER);
		wastebin_full.setVerticalTextPosition(JLabel.BOTTOM);
		wastebin_full.setHorizontalTextPosition(JLabel.CENTER);
		final JLabel	chargingstation	= new JLabel("dummy", new ImageIcon(chargingstation_image), JLabel.CENTER);
		chargingstation.setVerticalTextPosition(JLabel.BOTTOM);
		chargingstation.setHorizontalTextPosition(JLabel.CENTER);
		final JLabel	cleaner	= new JLabel("dummy", new ImageIcon(cleaner_image), JLabel.CENTER);
		cleaner.setVerticalTextPosition(JLabel.CENTER);
		cleaner.setHorizontalTextPosition(JLabel.RIGHT);

		// Map panel for painting the cleaner world view
		final JPanel	map	= new JPanel()
		{
			// generated by eclipse
			private static final long serialVersionUID = 6637115521263586060L;

			// overridden paint method.
			protected void	paintComponent(Graphics g)
			{
				// Get world state from environment object.
				boolean	daytime	= env.getDaytime();

				// Paint background (dependent on daytime).
				Rectangle	bounds	= getBounds();
				//g.setColor(daytime ? Color.lightGray : Color.darkGray);
				//g.fillRect(0, 0, bounds.width, bounds.height);
				Image	image	= daytime ? background_image : background_night_image;
				int w	= image.getWidth(this);
				int h	= image.getHeight(this);
				if(w>0 && h>0)
				{
					for(int y=0; y<bounds.height; y+=h)
					{
						for(int x=0; x<bounds.width; x+=w)
						{
							g.drawImage(image, x, y, this);
						}
					}
				}
				
				// Paint charge stations.
				Chargingstation[] stations = env.getChargingstations();
				for(int i=0; i<stations.length; i++)
				{
					Point p	= onScreenLocation(stations[i].getLocation(), bounds);
					chargingstation.setText(stations[i].getId());
					chargingstation.setForeground(daytime ? Color.black : Color.white);
					render(g, chargingstation, p);
				}

				// Paint waste bins.
				Wastebin[] wastebins = env.getWastebins();
				for(int i=0; i<wastebins.length; i++)
				{
					Point p	= onScreenLocation(wastebins[i].getLocation(), bounds);
					JLabel	renderer	= wastebin;
					if(wastebins[i].isFull())
						renderer	= wastebin_full;
					renderer.setText(wastebins[i].getId()+" ("+wastebins[i].getWastes().length+"/"+wastebins[i].getCapacity()+")");
					renderer.setForeground(daytime ? Color.black : Color.white);
					render(g, renderer, p);
				}

				// Paint waste.
				Waste[] wastes = env.getWastes();
				for(int i=0; i<wastes.length; i++)
				{
					Point p	= onScreenLocation(wastes[i].getLocation(), bounds);
					waste.setForeground(daytime ? Color.black : Color.white);
					render(g, waste, p);
				}

				// Paint the cleaner visions.
				Cleaner[] cleaners = env.getCleaners();

				//System.out.println("cls: "+env.hashCode()+" "+cleaners.length);
				for(int i=0; i<cleaners.length; i++)
				{
					int colorcode	= Math.abs(cleaners[i].getId().toString().hashCode()%8);
					Point	p	= onScreenLocation(cleaners[i].getLocation(), bounds);
					w	= (int)(cleaners[i].getVisionRange()*bounds.width);
					h	= (int)(cleaners[i].getVisionRange()*bounds.height);
					g.setColor(new Color((colorcode&1)!=0?255:100, (colorcode&2)!=0?255:100, (colorcode&4)!=0?255:100, 192));	// Vision range
					g.fillOval(p.x-w, p.y-h, w*2, h*2);
				}

				// Paint the cleaner agents.
				for(int i=0; i<cleaners.length; i++)
				{
					Point	p	= onScreenLocation(cleaners[i].getLocation(), bounds);
					cleaner.setText("<html>"
						+ cleaners[i].getId()+"<br>"
						+ "battery: " + (int)(cleaners[i].getChargestate()*100.0) + "%<br>"
						+ "waste: " + (cleaners[i].getCarriedWaste()!=null ? "yes" : "no")+"</html>");
					cleaner.setForeground(daytime ? Color.black	: Color.white);
					render(g, cleaner, new Point(p.x+45, p.y));	// Hack!!!
				}
			}
		};

		// Set widget sizes for clean layout
		wastebinchoice.setPreferredSize(new Dimension((int)wastebinchoice.getPreferredSize().getWidth()+10,
			(int)wastebinchoice.getPreferredSize().getHeight()));
		fillstate.setPreferredSize(new Dimension((int)fillstate.getPreferredSize().getWidth()+10,
			(int)fillstate.getPreferredSize().getHeight()));


		// Add listeners for setting widgets
		daytime.addChangeListener(new ChangeListener()
		{
			public void	stateChanged(ChangeEvent ce)
			{
				env.setDaytime(daytime.isSelected());
			}
		});
		setfillstate.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Wastebin wb = env.getWastebin((String)wastebinchoice.getSelectedItem());

				if(fillstate.getSelectedItem().equals("empty"))
					wb.empty();
				if(fillstate.getSelectedItem().equals("full"))
					wb.fill();
			}
		});
		
		// Handle scaling of images whenever window is resized.
		map.addComponentListener(new ComponentAdapter()
		{
			private Rectangle	_bounds;

			public void	componentResized(ComponentEvent ce)
			{
				Rectangle	bounds	= map.getBounds();
				if(_bounds==null)	_bounds	= bounds;
				double	scale	= Math.min(bounds.width/(double)_bounds.width,
					bounds.height/(double)_bounds.height);

				// Wastes
				((ImageIcon)waste.getIcon()).setImage(
					waste_image.getScaledInstance(
						(int)(waste_image.getWidth(map)*scale),
						(int)(waste_image.getHeight(map)*scale),
						Image.SCALE_DEFAULT));

				// Wastebin
				((ImageIcon)wastebin.getIcon()).setImage(
					wastebin_image.getScaledInstance(
						(int)(wastebin_image.getWidth(map)*scale),
						(int)(wastebin_image.getHeight(map)*scale),
						Image.SCALE_DEFAULT));

				// Full Wastebin
				((ImageIcon)wastebin_full.getIcon()).setImage(
					wastebin_full_image.getScaledInstance(
						(int)(wastebin_full_image.getWidth(map)*scale),
						(int)(wastebin_full_image.getHeight(map)*scale),
						Image.SCALE_DEFAULT));

				// Chargingstation
				((ImageIcon)chargingstation.getIcon()).setImage(
					chargingstation_image.getScaledInstance(
						(int)(chargingstation_image.getWidth(map)*scale),
						(int)(chargingstation_image.getHeight(map)*scale),
						Image.SCALE_DEFAULT));

				// Cleaner
				((ImageIcon)cleaner.getIcon()).setImage(
					cleaner_image.getScaledInstance(
						(int)(cleaner_image.getWidth(map)*scale),
						(int)(cleaner_image.getHeight(map)*scale),
						Image.SCALE_DEFAULT));
			}
		});
		
		// Allow placing waste with mouse
		map.addMouseListener(new MouseAdapter()
		{
			public void mousePressed(MouseEvent me)
			{
				Point	p	= me.getPoint();
				Rectangle	bounds	= map.getBounds();
				final Location	mouseloc = new Location((double)p.x/(double)bounds.width,
					1.0-(double)p.y/(double)bounds.height);
				final double tol = 7/(double)bounds.height;

				final Environment env = Environment.getInstance();
				
				Waste[] wastes = env.getWastes();
				Waste nearest = null;
				double dist = 0;
				for(int i=0; i<wastes.length; i++)
				{
					if(nearest==null || wastes[i].getLocation().getDistance(mouseloc)<dist)
					{
						nearest = wastes[i];
						dist = wastes[i].getLocation().getDistance(mouseloc);
					}
				}
				Waste waste = null;
				if(dist<tol)
					waste = nearest;

				// If waste is near clicked position remove the waste
				if(waste!=null)
				{
					env.removeWaste(waste);
				}

				// If position is clean add a new waste
				else
				{
					env.addWaste(new Waste(mouseloc));
				}
			}
		});

		// Show the gui.
		getContentPane().add(BorderLayout.SOUTH, options);
		getContentPane().add(BorderLayout.CENTER, map);
		setSize(600, 600);
		setLocation(SGUI.calculateMiddlePosition(EnvironmentGui.this));
		setVisible(true);

		// Repaint every 50 milliseconds
		timer	= new Timer(50, new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				map.invalidate();
				map.repaint();
				
				// update settings when environment changes
				if(daytime.isSelected()!=env.getDaytime())
				{
					daytime.setSelected(env.getDaytime());
				}
				
				if(!(""+env.getWastes().length).equals(wastecnt.getText()))
				{
					wastecnt.setText(""+env.getWastes().length);
				}
				
//				Set<String>	wbsadd	= new LinkedHashSet<>();
//				Set<String>	wbsremove	= new LinkedHashSet<>();
//				DefaultComboBoxModel<String>	wbsmodel	= (DefaultComboBoxModel<String>)wastebinchoice.getModel();
//				for(Wastebin wb: env.getWastebins())
//				{
//				}
//				
//				for(int i=wbsmodel.getSize()-1; i>=0; i--)
//				{
//					if(!nwbs.contains(wbsmodel.getElementAt(i)))
//					{
//						wbsmodel.r
//					}
//				}
//
//					
//						}
//						else if("wastebins".equals(propertyname))
//						{
//							if(ce.getNewValue()==null)
//							{
//								String name = ((Wastebin)ce.getOldValue()).getId();
//								.removeElement(name);
//							}
//							else
//							{
//								String name = ((Wastebin)ce.getNewValue()).getId();
//								int size = wastebinchoice.getModel().getSize();
//								boolean found = false;
//								for(int i=0; i<size && !found; i++)
//								{
//									String wbname = (String)wastebinchoice.getModel().getElementAt(i);
//									if(wbname.equals(name))
//										found = true;
//								}
//								if(!found)
//									((DefaultComboBoxModel<String>)wastebinchoice.getModel()).addElement(name);
//							}
//						}
						
			}
		});
		timer.start();
		
		// Exit program on window close.
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				System.exit(0);
			}
		});
	}
	
	//-------- helper methods --------

	/**
	 *  Get the on screen location for a location in  the world.
	 */
	private Point	onScreenLocation(ILocation loc, Rectangle bounds)
	{
		return new Point((int)(bounds.width*loc.getX()),
			(int)(bounds.height*(1.0-loc.getY())));
	}

	/**
	 *  Render a component on screen.
	 *  @param g	The graphics object.
	 *  @param comp	The component.
	 *  @param p	The on screen location.
	 */
	private void	render(Graphics g, Component comp, Point p)
	{
		Dimension	d	= comp.getPreferredSize();
		Rectangle	bounds	= new Rectangle(p.x - d.width/2, p.y - d.height/2, d.width+1, d.height);
		comp.setBounds(bounds);
		g.translate(bounds.x, bounds.y);
		comp.paint(g);
		g.translate(-bounds.x, -bounds.y);
	}

	/** Open world view window on Swing Thread. */
	public static void create()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				new EnvironmentGui().setVisible(true);
			}
		});
	}
}

