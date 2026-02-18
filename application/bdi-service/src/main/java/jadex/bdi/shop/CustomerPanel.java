package jadex.bdi.shop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import jadex.bdi.IBDIAgentFeature;
import jadex.bdi.ICapability;
import jadex.bdi.shop.CustomerCapability.BuyItem;
import jadex.common.SGUI;
import jadex.common.SUtil;
import jadex.core.ChangeEvent;
import jadex.core.ChangeEvent.Type;
import jadex.core.IChangeListener;
import jadex.core.IComponent;
import jadex.future.IFuture;
import jadex.future.IResultListener;
import jadex.requiredservice.IRequiredServiceFeature;

/**
 *  Customer gui that allows buying items at different shops.
 */
@SuppressWarnings("serial")
public class CustomerPanel extends JPanel
{
	//-------- attributes --------
	
	protected IComponent agent;
	protected ICapability capa;
	protected CustomerCapability cust;
	protected List<ItemInfo> shoplist = new ArrayList<>();
	protected JCheckBox remote;
	protected JTable shoptable;
	protected AbstractTableModel shopmodel = new ItemTableModel(shoplist);
	
	protected List<ItemInfo> invlist = new ArrayList<>();
	protected AbstractTableModel invmodel = new ItemTableModel(invlist);
	protected JTable invtable;
	protected Map<String, IShopService>	shops;
	
	//-------- constructors --------
	
	/**
	 *  Create a new gui.
	 */
	public CustomerPanel(IComponent agent, ICapability capa, CustomerCapability cust)
	{
		this.agent	= agent;
		this.cust	= cust;
		this.shops	= new LinkedHashMap<>();
		
		final JComboBox<String> shopscombo = new JComboBox<>();
		shopscombo.addItem("none");
		shopscombo.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if(shops.get(shopscombo.getSelectedItem()) instanceof IShopService)
				{
					refresh((IShopService)shops.get(shopscombo.getSelectedItem()));
				}
			}
		});
		
		remote = new JCheckBox("Remote");
		remote.setToolTipText("Also search remote platforms for shops.");
		final JButton searchbut = new JButton("Search");
		searchbut.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		    	
//		    	SServiceProvider.getServices(agent.getServiceProvider(), IShop.class, remote.isSelected(), true)
		    	try
		    	{
			    	searchbut.setEnabled(false);
					Collection<IShopService> coll = agent.getComponentHandle().scheduleStep(ia ->
					{
//						if(remote.isSelected())
//						{
//							return ia.getFeature(IRequiredServiceFeature.class).searchServices("remoteshopservices").get();
//						}
//						else
						{
							return ia.getFeature(IRequiredServiceFeature.class).searchServices(IShopService.class).get();
						}
					}).get();
					
					System.out.println("Customer search result: "+coll);
					((DefaultComboBoxModel<String>)shopscombo.getModel()).removeAllElements();
					shops.clear();
					if(coll!=null && coll.size()>0)
					{
						for(Iterator<IShopService> it=coll.iterator(); it.hasNext(); )
						{
							IShopService	shop	= it.next();
							String	name	= agent.getComponentHandle().scheduleAsyncStep(() -> shop.getName()).get();
							shops.put(name, shop);
							((DefaultComboBoxModel<String>)shopscombo.getModel()).addElement(name);
						}
					}
					else
					{
						((DefaultComboBoxModel<String>)shopscombo.getModel()).addElement("none");
					}
		    	}
		    	finally
		    	{
				    	searchbut.setEnabled(true);
		    	}
		    }
		});

		final NumberFormat df = NumberFormat.getInstance();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		final JTextField money = new JTextField(5);
		
		agent.getComponentHandle().scheduleStep(() ->
		{
				final double mon = cust.money.get();
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						money.setText(df.format(mon));
					}
				});
		});
		money.setEditable(false);
		
		agent.getComponentHandle().scheduleStep(() ->
		{
			capa.addChangeListener("money", new IChangeListener()
			{
				@Override
				public void valueChanged(ChangeEvent event)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							money.setText(df.format(event.value()));
						}
					});
				}
			});
		});
		
		JPanel selpanel = new JPanel(new GridBagLayout());
		selpanel.setBorder(new TitledBorder(new EtchedBorder(), "Properties"));
		int x=0;
		int y=0;
		selpanel.add(new JLabel("Money: "), new GridBagConstraints(
			x,y,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));
		x++;
		selpanel.add(money, new GridBagConstraints(
			x,y,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));
		x++;
		selpanel.add(new JLabel("Available shops: "), new GridBagConstraints(
			x,y,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));
		x++;
		selpanel.add(shopscombo, new GridBagConstraints(
			x,y,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,new Insets(2,2,2,2),0,0));
		x++;
		selpanel.add(searchbut, new GridBagConstraints(
			x,y,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));
		x++;
		selpanel.add(remote, new GridBagConstraints(
			x,y,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,new Insets(2,2,2,2),0,0));
		
		JPanel shoppanel = new JPanel(new BorderLayout());
		shoppanel.setBorder(new TitledBorder(new EtchedBorder(), "Shop Catalog"));
		shoptable = new JTable(shopmodel);
		shoptable.setPreferredScrollableViewportSize(new Dimension(600, 120));
		shoptable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		shoppanel.add(BorderLayout.CENTER, new JScrollPane(shoptable));

		JPanel invpanel = new JPanel(new BorderLayout());
		invpanel.setBorder(new TitledBorder(new EtchedBorder(), "Customer Inventory"));
		invtable = new JTable(invmodel);
		invtable.setPreferredScrollableViewportSize(new Dimension(600, 120));
		invpanel.add(BorderLayout.CENTER, new JScrollPane(invtable));

		agent.getComponentHandle().scheduleStep(() ->
		{
			try
			{
				capa.addChangeListener("inventory", new IChangeListener()
				{
					@Override
					public void valueChanged(ChangeEvent event)
					{
						if(event.type()==Type.REMOVED)
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									invlist.remove(event.value());
									invmodel.fireTableDataChanged();
								}
							});
						}
						
						else if(event.type()==Type.ADDED)
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
									invlist.add((ItemInfo) event.value());
									invmodel.fireTableDataChanged();
								}
							});
						}
						
						else if(event.type()==Type.CHANGED)
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								public void run()
								{
	//									System.out.println("factchanged: "+value);
									invmodel.fireTableDataChanged();
								}
							});
						}
					}
				});
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		});
		
		JPanel butpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
//		butpanel.setBorder(new TitledBorder(new EtchedBorder(), "Actions"));
		JButton buy = new JButton("Buy");
		final JTextField item = new JTextField(8);
		item.setEditable(false);
		butpanel.add(new JLabel("Selected item:"));
		butpanel.add(item);
		butpanel.add(buy);
		buy.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int sel = shoptable.getSelectedRow();
				if(sel!=-1)
				{
					final String name = (String)shopmodel.getValueAt(sel, 0);
					final Double price = (Double)shopmodel.getValueAt(sel, 1);
					final IShopService shop = (IShopService)shops.get(shopscombo.getSelectedItem());
					agent.getComponentHandle().scheduleStep(ia ->
					{
							BuyItem	big	= new BuyItem(name, shop, price.doubleValue());
							IFuture<Void> ret = ia.getFeature(IBDIAgentFeature.class).dispatchTopLevelGoal(big);
							ret.addResultListener(new IResultListener<Void>()
							{
								public void resultAvailable(Void result)
								{
									// Update number of available items
									SwingUtilities.invokeLater(() -> refresh(shop));
								}
								
								public void exceptionOccurred(Exception exception)
								{
									// Update number of available items
									SwingUtilities.invokeLater(() -> 
									{
										refresh(shop);
	
										String text = SUtil.wrapText("Item could not be bought. "+exception.getMessage());
										JOptionPane.showMessageDialog(SGUI.getWindowParent(CustomerPanel.this), text, "Buy problem", JOptionPane.INFORMATION_MESSAGE);
									});
								}
							});
					});
				}
			}
		});
		
		shoptable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				int sel = shoptable.getSelectedRow();
				if(sel!=-1)
				{
					item.setText(""+shopmodel.getValueAt(sel, 0));
				}
			}
		});
		
		setLayout(new GridBagLayout());
		x=0;
		y=0;
		add(selpanel, new GridBagConstraints(
			x,y++,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.BOTH,new Insets(2,2,2,2),0,0));
		add(shoppanel, new GridBagConstraints(
			x,y++,1,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,new Insets(2,2,2,2),0,0));
		add(invpanel, new GridBagConstraints(
			x,y++,1,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,new Insets(2,2,2,2),0,0));
		add(butpanel, new GridBagConstraints(
			x,y++,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.BOTH,new Insets(2,2,2,2),0,0));
		
//		refresh();
	}
	
	/**
	 *  Create a customer gui frame.
	 * /
	public static void createCustomerGui(final IBDIExternalAccess agent)
	{
		final JFrame f = new JFrame();
		f.add(new CustomerPanel(agent));
		f.pack();
		f.setLocation(SGUI.calculateMiddlePosition(f));
		f.setVisible(true);
		f.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				agent.killComponent();
			}
		});
		agent.addAgentListener(new IAgentListener() 
		{
			public void agentTerminating(AgentEvent ae) 
			{
				f.setVisible(false);
				f.dispose();
			}
			
			public void agentTerminated(AgentEvent ae) 
			{
			}
		});
	}*/
	
	/**
	 * Method to be called when goals may have changed.
	 */
	public void refresh(IShopService shop)
	{
		if(shop!=null)
		{
			agent.getComponentHandle().scheduleAsyncStep(() ->shop.getCatalog())
				.addResultListener(new IResultListener<ItemInfo[]>()
			{
				public void resultAvailable(ItemInfo[] aitems)
				{
					SwingUtilities.invokeLater(() ->
					{
						int sel = shoptable.getSelectedRow();
						shoplist.clear();
						for(int i = 0; i < aitems.length; i++)
						{
							if(!shoplist.contains(aitems[i]))
							{
	//							System.out.println("added: "+aitems[i]);
								shoplist.add(aitems[i]);
							}
						}
						shopmodel.fireTableDataChanged();
						if(sel!=-1 && sel<aitems.length)
							((DefaultListSelectionModel)shoptable.getSelectionModel()).setSelectionInterval(sel, sel);
					});
				}
				
				@Override
				public void exceptionOccurred(Exception exception)
				{
					exception.printStackTrace();
				}
			});
		}
		else
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					shoplist.clear();
					shopmodel.fireTableDataChanged();
				}
			});
		}
	}
		
}

@SuppressWarnings("serial")
class ItemTableModel extends AbstractTableModel
{
	protected List<ItemInfo> list;
	
	public ItemTableModel(List<ItemInfo> list)
	{
		this.list = list;
	}
	
	public int getRowCount()
	{
		return list.size();
	}

	public int getColumnCount()
	{
		return 3;
	}

	public String getColumnName(int column)
	{
		switch(column)
		{
			case 0:
				return "Name";
			case 1:
				return "Price";
			case 2:
				return "Quantity";
			default:
				return "";
		}
	}

	public boolean isCellEditable(int row, int column)
	{
		return false;
	}

	public Object getValueAt(int row, int column)
	{
		Object value = null;
		ItemInfo ii = (ItemInfo)list.get(row);
		if(column == 0)
		{
			value = ii.getName();
		}
		else if(column == 1)
		{
			value = Double.valueOf(ii.getPrice());
		}
		else if(column == 2)
		{
			value = Integer.valueOf(ii.getQuantity());
		}
		return value;
	}
};
